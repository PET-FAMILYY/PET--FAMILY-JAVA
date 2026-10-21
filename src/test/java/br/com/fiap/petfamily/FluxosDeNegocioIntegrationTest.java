package br.com.fiap.petfamily;

import br.com.fiap.petfamily.entity.Usuario;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "cors.allowed-origins=*")
class FluxosDeNegocioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Flyway flyway;

    private String tutorAToken;
    private String tutorBToken;
    private String vetToken;
    private long petIdDoTutorA;

    @BeforeEach
    void setUp() throws Exception {
        tutorAToken = registrarERecuperarToken("Tutor A", "tutora@teste.com");
        tutorBToken = registrarERecuperarToken("Tutor B", "tutorb@teste.com");
        vetToken = criarVeterinarioERecuperarToken();

        MvcResult petResult = mockMvc.perform(post("/pets")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Rex","especie":"Cachorro","raca":"SRD","idade":2,"peso":10.0}"""))
                .andExpect(status().isCreated())
                .andReturn();
        petIdDoTutorA = campo(petResult, "id").asLong();
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"tutora@teste.com","senha":"senhaErrada"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acessarRotaProtegidaSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/pets")).andExpect(status().isUnauthorized());
    }

    @Test
    void tutorNaoAcessaPetDeOutroTutor() throws Exception {
        mockMvc.perform(get("/pets/" + petIdDoTutorA).header("Authorization", "Bearer " + tutorBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/pets/" + petIdDoTutorA).header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk());
    }

    @Test
    void agendarConsultaNoPassadoEhRejeitado() throws Exception {
        mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"petId":%d,"tipoConsulta":"Checkup","data":"2020-01-01","horario":"10:00"}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agendarNoMesmoHorarioDuasVezesGeraConflito() throws Exception {
        String corpo = """
                {"petId":%d,"tipoConsulta":"Checkup","data":"2030-06-15","horario":"10:00"}"""
                .formatted(petIdDoTutorA);

        mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json").content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json").content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarLiberaHorarioEFluxoCompletoDeAtendimentoFunciona() throws Exception {
        MvcResult agendada = mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"petId":%d,"tipoConsulta":"Checkup","data":"2030-07-01","horario":"14:00"}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isCreated())
                .andReturn();
        long consultaId = campo(agendada, "id").asLong();

        mockMvc.perform(post("/consultas/" + consultaId + "/realizar")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                                {"observacoes":"Atendimento ok"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADA"));

        mockMvc.perform(post("/consultas/" + consultaId + "/realizar")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                                {"observacoes":"tentativa duplicada"}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/consultas/" + consultaId + "/cancelar")
                        .header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isConflict());
    }

    @Test
    void somenteVeterinarioDefineCuidadoETutorDonoConclui() throws Exception {
        mockMvc.perform(post("/lembretes")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"titulo":"x","dataLembrete":"2030-01-01","tipo":"y","petId":%d}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isForbidden());

        MvcResult cuidado = mockMvc.perform(post("/lembretes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                                {"titulo":"Vacina","dataLembrete":"2030-01-01","tipo":"Vacinacao","petId":%d,"recorrenciaDias":30}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isCreated())
                .andReturn();
        long lembreteId = campo(cuidado, "id").asLong();

        mockMvc.perform(post("/lembretes/" + lembreteId + "/concluir")
                        .header("Authorization", "Bearer " + tutorBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/lembretes/" + lembreteId + "/concluir")
                        .header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));

        mockMvc.perform(post("/lembretes/" + lembreteId + "/concluir")
                        .header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/lembretes/meus").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.dataLembrete == '2030-01-31')]").exists());
    }

    // ------------------------------------------------------------------
    // Testes adicionados na correção pós-avaliação
    // ------------------------------------------------------------------

    @Test
    void todasAsMigrationsForamAplicadasSemPendencias() {
        assertTrue(flyway.info().applied().length >= 6, "esperado V1..V6 aplicadas");
        assertEquals(0, flyway.info().pending().length);
    }

    @Test
    void h2ConsoleNaoEstaLiberadoSemLoginForaDoPerfilDev() throws Exception {
        mockMvc.perform(get("/h2-console")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    @Test
    void jsonMalformadoRetorna400EmVezDe500() throws Exception {
        mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"petId":%d,"tipoConsulta":"x","data":"nao-e-data","horario":"10:00"}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotaInexistenteRetorna404EmVezDe500() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void agendaFuturaDoVeterinarioNaoMostraConsultasCanceladas() throws Exception {
        MvcResult agendada = mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"petId":%d,"tipoConsulta":"Checkup","data":"2030-08-01","horario":"09:00"}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = campo(agendada, "id").asLong();

        mockMvc.perform(get("/consultas/futuras").header("Authorization", "Bearer " + vetToken))
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(id)).exists());

        mockMvc.perform(post("/consultas/" + id + "/cancelar").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/consultas/futuras").header("Authorization", "Bearer " + vetToken))
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(id)).doesNotExist());
    }

    @Test
    void atendimentoRegistraQuemERealizou() throws Exception {
        MvcResult agendada = mockMvc.perform(post("/consultas/agendar")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"petId":%d,"tipoConsulta":"Checkup","data":"2030-09-01","horario":"11:00"}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = campo(agendada, "id").asLong();

        mockMvc.perform(post("/consultas/" + id + "/realizar")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                                {"observacoes":"Tudo certo"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizadaPorNome").value("Vet Teste"))
                .andExpect(jsonPath("$.realizadaEm").exists());
    }

    @Test
    void cuidadoJaConcluidoNaoPodeSerEditado() throws Exception {
        MvcResult cuidado = mockMvc.perform(post("/lembretes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                                {"titulo":"Vacina","dataLembrete":"2030-01-01","tipo":"Vacinacao","petId":%d}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = campo(cuidado, "id").asLong();

        mockMvc.perform(post("/lembretes/" + id + "/concluir").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/lembretes/" + id)
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                                {"titulo":"Alterado","dataLembrete":"2031-01-01","tipo":"Vacinacao","petId":%d}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isConflict());
    }

    @Test
    void alterarEmailDoTutorAtualizaTambemOLogin() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"tutora@teste.com","senha":"senha123"}"""))
                .andExpect(status().isOk())
                .andReturn();
        long tutorId = campo(login, "tutorId").asLong();

        mockMvc.perform(put("/tutores/" + tutorId)
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                                {"nome":"Tutor A Novo","email":"novo.tutora@teste.com","telefone":"11999990000"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"novo.tutora@teste.com","senha":"senha123"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"tutora@teste.com","senha":"senha123"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeContaExcluidaRetorna401() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"tutorb@teste.com","senha":"senha123"}"""))
                .andReturn();
        long tutorId = campo(login, "tutorId").asLong();

        mockMvc.perform(delete("/tutores/" + tutorId).header("Authorization", "Bearer " + tutorBToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tutorBToken))
                .andExpect(status().isUnauthorized());
    }

    private String registrarERecuperarToken(String nome, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/registrar")
                        .contentType("application/json")
                        .content("""
                                {"nome":"%s","email":"%s","senha":"senha123"}""".formatted(nome, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return campo(result, "token").asText();
    }

    private String criarVeterinarioERecuperarToken() throws Exception {
        String email = "vet.teste@teste.com";
        usuarioRepository.save(Usuario.builder()
                .nome("Vet Teste")
                .email(email)
                .senhaHash(passwordEncoder.encode("senha123"))
                .role(Usuario.Role.VETERINARIO)
                .criadoEm(LocalDateTime.now())
                .build());

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","senha":"senha123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return campo(result, "token").asText();
    }

    @Test
    void petFilhoteComIdadeZeroPodeSerCadastrado() throws Exception {
        mockMvc.perform(post("/pets")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                            {"nome":"Filhote","especie":"Gato","idade":0,"peso":0.8}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idade").value(0));
    }

    @Test
    void ordenacaoPorCampoInexistenteRetorna400EmVezDe500() throws Exception {
        mockMvc.perform(get("/pets?sort=campoQueNaoExiste")
                        .header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtroDeEspecieFuncionaTambemParaOTutor() throws Exception {
        mockMvc.perform(post("/pets")
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                            {"nome":"Mingau","especie":"Gato"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/pets?especie=gato").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Mingau"));
    }

    @Test
    void tutorQueConcluiuCuidadoConsegueExcluirAPropriaConta() throws Exception {
        MvcResult cuidado = mockMvc.perform(post("/lembretes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType("application/json")
                        .content("""
                            {"titulo":"Vacina","dataLembrete":"2030-01-01","tipo":"Vacinacao","petId":%d}"""
                                .formatted(petIdDoTutorA)))
                .andExpect(status().isCreated())
                .andReturn();
        long lembreteId = campo(cuidado, "id").asLong();

        mockMvc.perform(post("/lembretes/" + lembreteId + "/concluir")
                        .header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                            {"email":"tutora@teste.com","senha":"senha123"}"""))
                .andExpect(status().isOk())
                .andReturn();
        long tutorId = campo(login, "tutorId").asLong();

        mockMvc.perform(delete("/tutores/" + tutorId).header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sextaTentativaDeLoginErradaRetorna429() throws Exception {
        String corpo = """
            {"email":"bloqueio@teste.com","senha":"errada"}""";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").contentType("application/json").content(corpo))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login").contentType("application/json").content(corpo))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void tokenContinuaValidoDepoisDeTrocarOEmail() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("""
                            {"email":"tutora@teste.com","senha":"senha123"}"""))
                .andExpect(status().isOk()).andReturn();
        long tutorId = campo(login, "tutorId").asLong();

        mockMvc.perform(put("/tutores/" + tutorId)
                        .header("Authorization", "Bearer " + tutorAToken)
                        .contentType("application/json")
                        .content("""
                            {"nome":"Tutor A","email":"outro.email@teste.com"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tutorAToken))
                .andExpect(status().isOk());
    }

    private JsonNode campo(MvcResult result, String nomeDoCampo) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(nomeDoCampo);
    }
}