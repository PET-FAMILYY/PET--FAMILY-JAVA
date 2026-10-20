package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.config.InvalidaCaches;
import br.com.fiap.petfamily.dto.request.InteracaoIARequest;
import br.com.fiap.petfamily.dto.response.InteracaoIAResponse;
import br.com.fiap.petfamily.entity.InteracaoIA;
import br.com.fiap.petfamily.entity.Pet;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.repository.InteracaoIARepository;
import br.com.fiap.petfamily.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteracaoIAService {

    private final InteracaoIARepository interacaoIARepository;
    private final PetService petService;
    private final SecurityUtils securityUtils;
    private final AssistenteIASimulado assistente;

    @Transactional
    @InvalidaCaches
    public InteracaoIAResponse criar(InteracaoIARequest request) {
        Pet pet = petDoTutorAutenticado(request.getPetId());
        InteracaoIA interacao = InteracaoIA.builder()
                .pergunta(request.getPergunta())
                .resposta(assistente.responder(request.getPergunta(), pet))
                .dataHora(LocalDateTime.now())
                .categoria(request.getCategoria())
                .pet(pet)
                .build();
        return toResponse(interacaoIARepository.save(interacao));
    }

    @Transactional(readOnly = true)
    public Page<InteracaoIAResponse> listar(Pageable pageable) {
        return interacaoIARepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InteracaoIAResponse buscarPorId(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<InteracaoIAResponse> listarPorPet(Long petId) {
        petDoTutorAutenticado(petId);
        return interacaoIARepository.findByPetIdOrderByDataHoraDesc(petId)
                .stream().map(this::toResponse).toList();
    }

    private InteracaoIA findById(Long id) {
        return interacaoIARepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interação com IA não encontrada com id: " + id));
    }

    private Pet petDoTutorAutenticado(Long petId) {
        Pet pet = petService.findById(petId);
        Long tutorId = securityUtils.getTutorIdAutenticadoOuFalha();
        if (!pet.getTutor().getId().equals(tutorId)) {
            throw new AccessDeniedException("O pet informado não pertence ao tutor autenticado.");
        }
        return pet;
    }

    private InteracaoIAResponse toResponse(InteracaoIA i) {
        return InteracaoIAResponse.builder()
                .id(i.getId())
                .pergunta(i.getPergunta())
                .resposta(i.getResposta())
                .dataHora(i.getDataHora())
                .categoria(i.getCategoria())
                .petId(i.getPet().getId())
                .petNome(i.getPet().getNome())
                .build();
    }
}