package br.com.fiap.petfamily.oracle;

import br.com.fiap.petfamily.config.InvalidaCaches;
import br.com.fiap.petfamily.exception.ConflitoOperacaoException;
import br.com.fiap.petfamily.exception.OperacaoInvalidaException;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.oracle.OracleDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * Chama as funções e procedures da package PKG_PETFAMILY_S4 via CallableStatement,
 * no MESMO DataSource usado pelo JPA. Só existe no perfil "oracle".
 */
@Service
@Profile("oracle")
public class PacoteOracleService {

    private static final Logger log = LoggerFactory.getLogger(PacoteOracleService.class);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> COLECOES = Set.of("TUTORES", "CONSULTAS", "INTERACOES_IA");

    private final JdbcTemplate jdbc;
    private final Path diretorioExportacao;

    public PacoteOracleService(JdbcTemplate jdbc,
                               @Value("${petfamily.export.dir:json}") String diretorioExportacao) {
        this.jdbc = jdbc;
        this.diretorioExportacao = Path.of(diretorioExportacao);
    }

    // ---------- Procedures de escrita ----------

    @Transactional
    @InvalidaCaches
    public Long cadastrarTutor(TutorOracleRequest r) {
        return executar("pr_cadastrar_tutor", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call pkg_petfamily_s4.pr_cadastrar_tutor(?, ?, ?, ?)}")) {
                cs.setString(1, r.nome());
                cs.setString(2, r.email());
                cs.setString(3, r.telefone());
                cs.registerOutParameter(4, Types.NUMERIC);
                cs.execute();
                return cs.getLong(4);
            }
        }));
    }

    @Transactional
    @InvalidaCaches
    public Long cadastrarPet(PetOracleRequest r) {
        return executar("pr_cadastrar_pet", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call pkg_petfamily_s4.pr_cadastrar_pet(?, ?, ?, ?)}")) {
                cs.setLong(1, r.tutorId());
                cs.setString(2, r.nome());
                cs.setString(3, r.especie());
                cs.registerOutParameter(4, Types.NUMERIC);
                cs.execute();
                return cs.getLong(4);
            }
        }));
    }

    @Transactional
    @InvalidaCaches
    public Long agendarConsulta(AgendamentoOracleRequest r) {
        return executar("pr_agendar_consulta", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call pkg_petfamily_s4.pr_agendar_consulta(?, ?, ?, ?, ?)}")) {
                cs.setLong(1, r.petId());
                cs.setDate(2, java.sql.Date.valueOf(r.data()));
                cs.setString(3, r.horario().format(HH_MM));
                cs.setString(4, r.tipo());
                cs.registerOutParameter(5, Types.NUMERIC);
                cs.execute();
                return cs.getLong(5);
            }
        }));
    }

    @Transactional
    @InvalidaCaches
    public void atualizarStatus(Long consultaId, String status) {
        executar("pr_atualizar_status", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call pkg_petfamily_s4.pr_atualizar_status(?, ?)}")) {
                cs.setLong(1, consultaId);
                cs.setString(2, status);
                cs.execute();
                return null;
            }
        }));
    }

    // ---------- Funções ----------

    @Transactional(readOnly = true)
    public long contarPets(Long tutorId) {
        return executar("fn_contar_pets", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{? = call pkg_petfamily_s4.fn_contar_pets(?)}")) {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.setLong(2, tutorId);
                cs.execute();
                return cs.getLong(1);
            }
        }));
    }

    @Transactional(readOnly = true)
    public boolean statusValido(String status) {
        return executar("fn_status_valido", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{? = call pkg_petfamily_s4.fn_status_valido(?)}")) {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.setString(2, status);
                cs.execute();
                return cs.getInt(1) == 1;
            }
        }));
    }

    // ---------- Exportação JSON ----------

    /** Chama pr_exportar_json e devolve o JSON (CLOB lido dentro da mesma conexão). */
    @Transactional(readOnly = true)
    public String exportarJson(String colecao) {
        String nome = colecao == null ? "" : colecao.trim().toUpperCase(Locale.ROOT);
        if (!COLECOES.contains(nome)) {
            throw new OperacaoInvalidaException("Coleção deve ser TUTORES, CONSULTAS ou INTERACOES_IA.");
        }
        return executar("pr_exportar_json", () -> jdbc.execute((java.sql.Connection con) -> {
            try (CallableStatement cs = con.prepareCall("{call pkg_petfamily_s4.pr_exportar_json(?, ?)}")) {
                cs.setString(1, nome);
                cs.registerOutParameter(2, Types.CLOB);
                cs.execute();
                Clob clob = cs.getClob(2);
                try (Reader reader = clob.getCharacterStream()) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[8192];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        sb.append(buf, 0, n);
                    }
                    return sb.toString();
                } catch (IOException e) {
                    throw new SQLException("Falha ao ler o CLOB exportado", e);
                } finally {
                    clob.free();
                }
            }
        }));
    }

    /** Exporta a coleção e grava json/<colecao>.json (nome esperado por mongodb/02_importar.js). */
    public ExportacaoResponse exportarParaArquivo(String colecao) {
        String json = exportarJson(colecao);
        String arquivo = colecao.trim().toLowerCase(Locale.ROOT) + ".json";
        try {
            Files.createDirectories(diretorioExportacao);
            Path destino = diretorioExportacao.resolve(arquivo);
            Files.writeString(destino, json, StandardCharsets.UTF_8);
            log.info("Exportado {} ({} caracteres)", destino.toAbsolutePath(), json.length());
            return new ExportacaoResponse(colecao.toUpperCase(Locale.ROOT),
                    destino.toAbsolutePath().toString(), json.length());
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar o arquivo " + arquivo, e);
        }
    }

    // ---------- Tradução dos erros ORA-20xxx da package ----------

    private <T> T executar(String rotina, java.util.function.Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (DataAccessException ex) {
            throw traduzir(rotina, ex);
        }
    }

    /** ORA-20004 -> 404, ORA-20006 e ORA-20009 -> 409, demais ORA-20xxx -> 400. */
    private RuntimeException traduzir(String rotina, DataAccessException ex) {
        Throwable causa = ex.getMostSpecificCause();
        if (causa instanceof SQLException sql && sql.getErrorCode() >= 20000 && sql.getErrorCode() <= 20999) {
            String mensagem = extrairMensagem(sql.getMessage());
            log.warn("{} retornou ORA-{}: {}", rotina, sql.getErrorCode(), mensagem);
            return switch (sql.getErrorCode()) {
                case 20004 -> new ResourceNotFoundException(mensagem);
                case 20006, 20009 -> new ConflitoOperacaoException(mensagem);
                default -> new OperacaoInvalidaException(mensagem);
            };
        }
        return ex;
    }

    /** "ORA-20004: Tutor nao encontrado.\nORA-06512: ..." -> "Tutor nao encontrado." */
    static String extrairMensagem(String bruta) {
        if (bruta == null) {
            return "Erro ao executar rotina do banco.";
        }
        String primeira = bruta.split("\\R", 2)[0];
        int i = primeira.indexOf(':');
        return (i >= 0 ? primeira.substring(i + 1) : primeira).trim();
    }
}