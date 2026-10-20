package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.dto.response.DashboardResponse;
import br.com.fiap.petfamily.entity.Consulta.StatusConsulta;
import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final ConsultaRepository consultaRepository;
    private final LembreteRepository lembreteRepository;
    private final InteracaoIARepository interacaoIARepository;

    @Cacheable(value = "dashboard", key = "'resumo'")
    public DashboardResponse gerarResumo() {
        long totalTutores = tutorRepository.count();
        long totalPets = petRepository.count();
        long totalConsultas = consultaRepository.count();
        long totalLembretesPendentes = lembreteRepository.countByStatus(StatusLembrete.PENDENTE);
        long totalInteracoesIA = interacaoIARepository.count();
        long consultasRealizadas = consultaRepository.countByStatus(StatusConsulta.REALIZADA);

        double taxaAdesao = totalConsultas > 0
                ? Math.round((double) consultasRealizadas / totalConsultas * 1000.0) / 10.0
                : 0.0;

        return DashboardResponse.builder()
                .totalTutores(totalTutores)
                .totalPets(totalPets)
                .totalConsultas(totalConsultas)
                .totalLembretesPendentes(totalLembretesPendentes)
                .totalInteracoesIA(totalInteracoesIA)
                .taxaAdesaoPreventiva(taxaAdesao)
                .build();
    }
}
