package br.com.fiap.petfamily.service;

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

    @Transactional
    public InteracaoIAResponse criar(InteracaoIARequest request) {
        Pet pet = petService.findById(request.getPetId());
        Long tutorId = securityUtils.getTutorIdAutenticadoOuFalha();
        if (!pet.getTutor().getId().equals(tutorId)) {
            throw new AccessDeniedException("O pet informado não pertence ao tutor autenticado.");
        }
        String resposta = gerarRespostaSimulada(request.getPergunta(), request.getCategoria(), pet);
        InteracaoIA interacao = InteracaoIA.builder()
                .pergunta(request.getPergunta())
                .resposta(resposta)
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
        Pet pet = petService.findById(petId);
        Long tutorId = securityUtils.getTutorIdAutenticadoOuFalha();
        if (!pet.getTutor().getId().equals(tutorId)) {
            throw new AccessDeniedException("O pet informado não pertence ao tutor autenticado.");
        }
        return interacaoIARepository.findByPetIdOrderByDataHoraDesc(petId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private InteracaoIA findById(Long id) {
        return interacaoIARepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interação com IA não encontrada com id: " + id));
    }

    private String gerarRespostaSimulada(String pergunta, String categoria, Pet pet) {
        String nomePet = pet.getNome();
        String especie = pet.getEspecie();
        String lower = pergunta.toLowerCase();

        if (lower.contains("vacin")) {
            return String.format(
                "Para %s (%s), é essencial manter o calendário vacinal em dia. As vacinas principais variam " +
                "por espécie e estilo de vida. Recomendo consultar seu veterinário para montar o protocolo " +
                "ideal. Em cães, as vacinas V8/V10 e antirrábica são fundamentais; em gatos, V4/V5 e FeLV.",
                nomePet, especie);
        }
        if (lower.contains("aliment") || lower.contains("ração") || lower.contains("comida")) {
            return String.format(
                "A nutrição de %s deve ser adequada à espécie %s, faixa etária e condição corporal. " +
                "Prefira rações de qualidade com proteína como primeiro ingrediente. Evite alimentos " +
                "humanos como uva, cebola, chocolate e xilitol, que são tóxicos para pets.",
                nomePet, especie);
        }
        if (lower.contains("comportamento") || lower.contains("agitad") || lower.contains("agressiv")) {
            return String.format(
                "Mudanças de comportamento em %s podem indicar estresse, dor, doença ou ansiedade. " +
                "Observe padrões como perda de apetite, letargia ou isolamento. Se os sintomas persistirem " +
                "por mais de 48h, procure atendimento veterinário para avaliação clínica completa.",
                nomePet);
        }
        if (lower.contains("banho") || lower.contains("higiene") || lower.contains("pelo") || lower.contains("tosa")) {
            return String.format(
                "A higiene de %s (%s) é fundamental para saúde e bem-estar. A frequência do banho varia por " +
                "raça e pelagem. Use sempre produtos específicos para pets — produtos humanos podem alterar " +
                "o pH da pele. Escovações regulares previnem nós e parasitas.",
                nomePet, especie);
        }
        if (lower.contains("doença") || lower.contains("sintoma") || lower.contains("vômit") || lower.contains("diarr")) {
            return String.format(
                "Se %s apresenta sintomas como vômito, diarreia, letargia ou perda de apetite, " +
                "procure atendimento veterinário imediatamente. Não administre medicamentos humanos " +
                "sem prescrição — muitos são tóxicos para pets. Hidratação adequada é essencial.",
                nomePet);
        }
        if (lower.contains("parasit") || lower.contains("pulga") || lower.contains("carrapath") || lower.contains("verme")) {
            return String.format(
                "O controle de parasitas em %s deve ser feito de forma preventiva e contínua. " +
                "Antipulgas, anticarrapatos e vermífugos devem ser aplicados regularmente conforme " +
                "orientação veterinária. A prevenção protege tanto o pet quanto a família.",
                nomePet);
        }
        if (lower.contains("castração") || lower.contains("castrar") || lower.contains("esteriliz")) {
            return String.format(
                "A castração de %s traz benefícios como prevenção de doenças reprodutivas, redução " +
                "de comportamentos indesejados e controle populacional. O momento ideal varia por " +
                "espécie e raça. Consulte seu veterinário para avaliar o melhor momento para %s.",
                nomePet, nomePet);
        }
        return String.format(
            "Olá! Estou aqui para ajudar com o cuidado de %s (%s). Posso orientar sobre alimentação, " +
            "vacinação, higiene, comportamento e saúde preventiva. Para diagnósticos e tratamentos, " +
            "sempre consulte um médico veterinário. Como posso ajudar melhor hoje?",
            nomePet, especie);
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
