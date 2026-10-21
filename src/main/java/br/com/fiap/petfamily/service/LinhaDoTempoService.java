package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.dto.response.EventoSaudeResponse;
import br.com.fiap.petfamily.entity.Pet;
import br.com.fiap.petfamily.repository.ConsultaRepository;
import br.com.fiap.petfamily.repository.InteracaoIARepository;
import br.com.fiap.petfamily.repository.LembreteRepository;
import br.com.fiap.petfamily.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinhaDoTempoService {

    private final PetService petService;
    private final ConsultaRepository consultaRepository;
    private final LembreteRepository lembreteRepository;
    private final InteracaoIARepository interacaoIARepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<EventoSaudeResponse> montar(Long petId) {
        Pet pet = petService.findById(petId);
        securityUtils.exigirTutorDono(pet.getTutor().getId(), "Este pet não pertence ao tutor autenticado.");

        List<EventoSaudeResponse> eventos = new ArrayList<>();

        consultaRepository.findByPetId(petId).forEach(c -> eventos.add(new EventoSaudeResponse(
                "CONSULTA",
                c.getData().atTime(c.getHorario() != null ? c.getHorario() : LocalTime.MIDNIGHT),
                c.getTipoConsulta(), c.getObservacoes(), c.getStatus().name())));

        lembreteRepository.findByPetId(petId).forEach(l -> eventos.add(new EventoSaudeResponse(
                "CUIDADO", l.getDataLembrete().atStartOfDay(),
                l.getTitulo(), l.getDescricao(), l.getStatus().name())));

        interacaoIARepository.findByPetIdOrderByDataHoraDesc(petId).forEach(i -> eventos.add(new EventoSaudeResponse(
                "ASSISTENTE_IA", i.getDataHora(), resumir(i.getPergunta()), i.getResposta(), null)));

        eventos.sort(Comparator.comparing(EventoSaudeResponse::quando).reversed());
        return eventos;
    }

    private static String resumir(String texto) {
        return texto.length() <= 80 ? texto : texto.substring(0, 77) + "...";
    }
}