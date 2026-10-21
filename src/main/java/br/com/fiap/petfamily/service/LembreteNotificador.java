package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.repository.LembreteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Todo dia às 08:00 avisa sobre cuidados que vencem em até 3 dias (canal simulado por log). */
@Component
@RequiredArgsConstructor
@Slf4j
public class LembreteNotificador {

    private final LembreteRepository lembreteRepository;

    @Scheduled(cron = "${petfamily.lembretes.cron:0 0 8 * * *}")
    @Transactional(readOnly = true)
    public void avisarCuidadosProximos() {
        LocalDate hoje = LocalDate.now();
        var proximos = lembreteRepository
                .findByStatusAndDataLembreteBetween(StatusLembrete.PENDENTE, hoje, hoje.plusDays(3));

        proximos.forEach(l -> log.info("[AVISO] {} — {} vence em {} (tutor: {})",
                l.getPet().getNome(), l.getTitulo(), l.getDataLembrete(),
                l.getPet().getTutor().getEmail()));
        log.info("Verificação de cuidados concluída: {} aviso(s).", proximos.size());
    }
}