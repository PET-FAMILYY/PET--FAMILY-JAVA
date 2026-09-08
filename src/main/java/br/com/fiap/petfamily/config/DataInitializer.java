package br.com.fiap.petfamily.config;

import br.com.fiap.petfamily.entity.*;
import br.com.fiap.petfamily.entity.Consulta.StatusConsulta;
import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private static final String SENHA_DEMO = "senha123";

    private record TutoresDemo(Tutor pedro, Tutor joao, Tutor maria, Tutor ana) {}

    private record PetsDemo(Pet thor, Pet luna, Pet bolinha, Pet nemo, Pet mia) {}

    @Bean
    public CommandLineRunner loadData(
            TutorRepository tutorRepo,
            PetRepository petRepo,
            ConsultaRepository consultaRepo,
            ConsultaSlotRepository consultaSlotRepo,
            LembreteRepository lembreteRepo,
            InteracaoIARepository interacaoRepo,
            UsuarioRepository usuarioRepo,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (tutorRepo.count() > 0) {
                log.info("Dados já existem, pulando inicialização de demonstração.");
                return;
            }

            log.info("==> [perfil dev] Carregando dados de demonstração...");

            TutoresDemo tutores = criarTutoresDemo(tutorRepo);
            Usuario veterinario = criarUsuariosDemo(usuarioRepo, passwordEncoder, tutores);
            PetsDemo pets = criarPetsDemo(petRepo, tutores);
            criarConsultasDemo(consultaRepo, consultaSlotRepo, pets);
            criarCuidadosDemo(lembreteRepo, usuarioRepo, veterinario, pets, tutores.maria());
            criarInteracoesDemo(interacaoRepo, pets);

            log.info("==> Dados de demonstração carregados: {} tutores, {} pets. Senha padrão de todas as contas: '{}'.",
                    tutorRepo.count(), petRepo.count(), SENHA_DEMO);
        };
    }

    private TutoresDemo criarTutoresDemo(TutorRepository tutorRepo) {
        Tutor pedro = tutorRepo.save(Tutor.builder()
                .nome("Pedro Vaz").email("pedro@petfamily.com").telefone("(11) 99999-0001").build());
        Tutor joao = tutorRepo.save(Tutor.builder()
                .nome("João Victor").email("joao@petfamily.com").telefone("(11) 99999-0002").build());
        Tutor maria = tutorRepo.save(Tutor.builder()
                .nome("Maria Silva").email("maria@petfamily.com").telefone("(11) 99999-0003").build());
        Tutor ana = tutorRepo.save(Tutor.builder()
                .nome("Ana Oliveira").email("ana@petfamily.com").telefone("(11) 99999-0004").build());
        return new TutoresDemo(pedro, joao, maria, ana);
    }

    private Usuario criarUsuariosDemo(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder, TutoresDemo tutores) {
        String senhaHash = passwordEncoder.encode(SENHA_DEMO);
        for (Tutor tutor : new Tutor[]{tutores.pedro(), tutores.joao(), tutores.maria(), tutores.ana()}) {
            usuarioRepo.save(Usuario.builder()
                    .nome(tutor.getNome())
                    .email(tutor.getEmail())
                    .senhaHash(senhaHash)
                    .role(Usuario.Role.TUTOR)
                    .tutor(tutor)
                    .criadoEm(LocalDateTime.now())
                    .build());
        }
        return usuarioRepo.save(Usuario.builder()
                .nome("Dra. Camila Souza")
                .email("veterinario@petfamily.com")
                .senhaHash(senhaHash)
                .role(Usuario.Role.VETERINARIO)
                .criadoEm(LocalDateTime.now())
                .build());
    }

    private PetsDemo criarPetsDemo(PetRepository petRepo, TutoresDemo tutores) {
        Pet thor = petRepo.save(Pet.builder()
                .nome("Thor").especie("Cachorro").raca("Golden Retriever").idade(3).peso(28.5)
                .observacoesSaude("Alergia a ração com frango. Prefere ração de cordeiro.")
                .tutor(tutores.pedro()).build());
        Pet luna = petRepo.save(Pet.builder()
                .nome("Luna").especie("Gato").raca("Siamês").idade(2).peso(3.8)
                .observacoesSaude("Castrada. Sem alergias conhecidas. Muito ativa.")
                .tutor(tutores.joao()).build());
        Pet bolinha = petRepo.save(Pet.builder()
                .nome("Bolinha").especie("Cachorro").raca("Poodle").idade(5).peso(5.2)
                .observacoesSaude("Histórico de otite recorrente. Necessita limpeza auricular semanal.")
                .tutor(tutores.maria()).build());
        Pet nemo = petRepo.save(Pet.builder()
                .nome("Nemo").especie("Peixe").raca("Peixe-palhaço").idade(1).peso(0.1)
                .observacoesSaude("Aquário de água salgada. pH 8.2.")
                .tutor(tutores.ana()).build());
        Pet mia = petRepo.save(Pet.builder()
                .nome("Mia").especie("Gato").raca("Persa").idade(4).peso(4.5)
                .observacoesSaude("Problemas respiratórios leves. Acompanhamento semestral.")
                .tutor(tutores.pedro()).build());
        return new PetsDemo(thor, luna, bolinha, nemo, mia);
    }

    private void criarConsultasDemo(ConsultaRepository consultaRepo, ConsultaSlotRepository slotRepo, PetsDemo pets) {
        consultaRepo.save(Consulta.builder()
                .data(LocalDate.now().minusDays(60)).horario(LocalTime.of(9, 0))
                .tipoConsulta("Rotina").status(StatusConsulta.REALIZADA)
                .observacoes("Exame clínico completo. Pet saudável, vacinação em dia.")
                .pet(pets.thor()).build());

        Consulta vacinacaoThor = consultaRepo.save(Consulta.builder()
                .data(LocalDate.now().plusDays(7)).horario(LocalTime.of(14, 0))
                .tipoConsulta("Vacinação").status(StatusConsulta.AGENDADA)
                .observacoes("Vacina antirrábica anual e V10 de reforço.")
                .pet(pets.thor()).build());
        reservarSlot(slotRepo, vacinacaoThor);

        Consulta dermatoLuna = consultaRepo.save(Consulta.builder()
                .data(LocalDate.now().plusDays(3)).horario(LocalTime.of(10, 30))
                .tipoConsulta("Dermatologia").status(StatusConsulta.AGENDADA)
                .observacoes("Verificar possível dermatite. Queda de pelo excessiva.")
                .pet(pets.luna()).build());
        reservarSlot(slotRepo, dermatoLuna);

        consultaRepo.save(Consulta.builder()
                .data(LocalDate.now().minusDays(15)).horario(LocalTime.of(11, 0))
                .tipoConsulta("Otite").status(StatusConsulta.REALIZADA)
                .observacoes("Otite bacteriana bilateral. Prescrito antibiótico auricular por 10 dias.")
                .pet(pets.bolinha()).build());

        Consulta retornoBolinha = consultaRepo.save(Consulta.builder()
                .data(LocalDate.now().plusDays(14)).horario(LocalTime.of(15, 0))
                .tipoConsulta("Retorno").status(StatusConsulta.AGENDADA)
                .observacoes("Retorno para avaliar resolução da otite.")
                .pet(pets.bolinha()).build());
        reservarSlot(slotRepo, retornoBolinha);

        consultaRepo.save(Consulta.builder()
                .data(LocalDate.now().minusDays(5)).horario(LocalTime.of(8, 0))
                .tipoConsulta("Check-up").status(StatusConsulta.CANCELADA)
                .observacoes("Cancelado pelo tutor. Reagendar.")
                .pet(pets.mia()).build());
    }

    private void criarCuidadosDemo(LembreteRepository lembreteRepo, UsuarioRepository usuarioRepo,
                                    Usuario veterinario, PetsDemo pets, Tutor maria) {
        lembreteRepo.save(Lembrete.builder()
                .titulo("Vacina Antirrábica — Thor")
                .descricao("Renovação anual da vacina antirrábica. Agendar junto com V10.")
                .dataLembrete(LocalDate.now().plusDays(7)).tipo("Vacinação").status(StatusLembrete.PENDENTE)
                .recorrenciaDias(365).criadoPor(veterinario).pet(pets.thor()).build());

        lembreteRepo.save(Lembrete.builder()
                .titulo("Vermifugação — Luna")
                .descricao("Vermifugação trimestral. Usar produto indicado pelo veterinário.")
                .dataLembrete(LocalDate.now().plusDays(15)).tipo("Preventivo").status(StatusLembrete.PENDENTE)
                .recorrenciaDias(90).criadoPor(veterinario).pet(pets.luna()).build());

        lembreteRepo.save(Lembrete.builder()
                .titulo("Retorno Otite — Bolinha")
                .descricao("Retorno para verificar resolução da otite bilateral.")
                .dataLembrete(LocalDate.now().plusDays(14)).tipo("Retorno").status(StatusLembrete.PENDENTE)
                .criadoPor(veterinario).pet(pets.bolinha()).build());

        lembreteRepo.save(Lembrete.builder()
                .titulo("Banho e Tosa — Bolinha")
                .descricao("Agendamento mensal de banho e tosa no petshop.")
                .dataLembrete(LocalDate.now().minusDays(5)).tipo("Higiene").status(StatusLembrete.CONCLUIDO)
                .dataConclusao(LocalDate.now().minusDays(5)).criadoPor(veterinario)
                .concluidoPor(usuarioRepo.findByEmailIgnoreCase(maria.getEmail()).orElse(null))
                .pet(pets.bolinha()).build());

        lembreteRepo.save(Lembrete.builder()
                .titulo("Check-up Semestral — Mia")
                .descricao("Avaliação respiratória semestral. Incluir raio-X de tórax.")
                .dataLembrete(LocalDate.now().plusDays(30)).tipo("Rotina").status(StatusLembrete.PENDENTE)
                .recorrenciaDias(180).criadoPor(veterinario).pet(pets.mia()).build());
    }

    private void criarInteracoesDemo(InteracaoIARepository interacaoRepo, PetsDemo pets) {
        interacaoRepo.save(InteracaoIA.builder()
                .pergunta("Quais vacinas o Thor precisa tomar esse ano?")
                .resposta("Para Thor (Cachorro), é essencial manter o calendário vacinal em dia. " +
                    "As vacinas principais variam por espécie e estilo de vida. Recomendo consultar " +
                    "seu veterinário para montar o protocolo ideal. Em cães, as vacinas V8/V10 e " +
                    "antirrábica são fundamentais; em gatos, V4/V5 e FeLV.")
                .dataHora(LocalDateTime.now().minusDays(3)).categoria("Vacinação").pet(pets.thor()).build());

        interacaoRepo.save(InteracaoIA.builder()
                .pergunta("O que posso dar para a Luna comer além de ração?")
                .resposta("A nutrição de Luna deve ser adequada à espécie Gato, faixa etária e " +
                    "condição corporal. Prefira rações de qualidade com proteína como primeiro " +
                    "ingrediente. Evite alimentos humanos como uva, cebola, chocolate e xilitol, " +
                    "que são tóxicos para pets.")
                .dataHora(LocalDateTime.now().minusDays(2)).categoria("Alimentação").pet(pets.luna()).build());

        interacaoRepo.save(InteracaoIA.builder()
                .pergunta("O Bolinha está se coçando muito nas orelhas, o que pode ser?")
                .resposta("Se Bolinha apresenta sintomas como vômito, diarreia, letargia ou perda de " +
                    "apetite, procure atendimento veterinário imediatamente. Não administre " +
                    "medicamentos humanos sem prescrição — muitos são tóxicos para pets. " +
                    "Hidratação adequada é essencial.")
                .dataHora(LocalDateTime.now().minusDays(1)).categoria("Sintomas").pet(pets.bolinha()).build());
    }

    private void reservarSlot(ConsultaSlotRepository slotRepo, Consulta consulta) {
        slotRepo.save(ConsultaSlot.builder()
                .data(consulta.getData())
                .horario(consulta.getHorario())
                .consultaId(consulta.getId())
                .build());
    }
}
