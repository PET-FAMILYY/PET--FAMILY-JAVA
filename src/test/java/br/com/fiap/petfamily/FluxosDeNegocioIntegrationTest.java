package br.com.fiap.petfamily;

import br.com.fiap.petfamily.entity.Usuario;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxosDeNegocioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private JsonNode campo(MvcResult result, String nomeDoCampo) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(nomeDoCampo);
    }
}
