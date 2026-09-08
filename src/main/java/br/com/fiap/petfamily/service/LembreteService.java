package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.dto.request.LembreteRequest;
import br.com.fiap.petfamily.dto.response.LembreteResponse;
import br.com.fiap.petfamily.entity.Lembrete;
import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.entity.Pet;
import br.com.fiap.petfamily.entity.Usuario;
import br.com.fiap.petfamily.exception.ConflitoOperacaoException;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.repository.LembreteRepository;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import br.com.fiap.petfamily.security.SecurityUtils;
import br.com.fiap.petfamily.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LembreteService {

    private static final String CACHE_LEMBRETES = "lembretes";
    private static final String CACHE_DASHBOARD = "dashboard";

    private final LembreteRepository lembreteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PetService petService;
    private final SecurityUtils securityUtils;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CACHE_LEMBRETES, allEntries = true),
        @CacheEvict(value = CACHE_DASHBOARD, allEntries = true)
    })
    public LembreteResponse criar(LembreteRequest request) {
        Pet pet = petService.findById(request.getPetId());
        Usuario veterinario = usuarioRepository.findById(securityUtils.getUsuarioAutenticado().getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));

        Lembrete lembrete = lembreteRepository.save(Lembrete.builder()
                .titulo(request.getTitulo())
                .descricao(request.getDescricao())
                .dataLembrete(request.getDataLembrete())
                .tipo(request.getTipo())
                .status(StatusLembrete.PENDENTE)
                .recorrenciaDias(request.getRecorrenciaDias())
                .criadoPor(veterinario)
                .pet(pet)
                .build());
        return toResponse(lembrete);
    }

    @Transactional(readOnly = true)
    public Page<LembreteResponse> listar(StatusLembrete status, Pageable pageable) {
        UsuarioPrincipal principal = securityUtils.getUsuarioAutenticado();
        if (principal.isTutor()) {
            Page<Lembrete> page = status != null
                    ? lembreteRepository.findByPetTutorIdAndStatus(principal.getTutorId(), status, pageable)
                    : lembreteRepository.findByPetTutorId(principal.getTutorId(), pageable);
            return page.map(this::toResponse);
        }
        Page<Lembrete> page = status != null
                ? lembreteRepository.findByStatus(status, pageable)
                : lembreteRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LembreteResponse buscarPorId(Long id) {
        Lembrete lembrete = findById(id);
        securityUtils.exigirTutorDono(lembrete.getPet().getTutor().getId(), "Este cuidado não pertence a um pet do tutor autenticado.");
        return toResponse(lembrete);
    }

    @Transactional(readOnly = true)
    public List<LembreteResponse> listarPendentesPorPet(Long petId) {
        Pet pet = petService.findById(petId);
        securityUtils.exigirTutorDono(pet.getTutor().getId(), "Este cuidado não pertence a um pet do tutor autenticado.");
        return lembreteRepository.findByPetIdAndStatus(petId, StatusLembrete.PENDENTE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LembreteResponse> listarDoTutorAutenticado() {
        Long tutorId = securityUtils.getTutorIdAutenticadoOuFalha();
        return lembreteRepository.findByPetTutorId(tutorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CACHE_LEMBRETES, allEntries = true),
        @CacheEvict(value = CACHE_DASHBOARD, allEntries = true)
    })
    public LembreteResponse concluir(Long id) {
        Lembrete lembrete = findById(id);
        securityUtils.getTutorIdAutenticadoOuFalha();
        securityUtils.exigirTutorDono(lembrete.getPet().getTutor().getId(), "Este cuidado não pertence a um pet do tutor autenticado.");

        Usuario tutorUsuario = usuarioRepository.findById(securityUtils.getUsuarioAutenticado().getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));

        String titulo = lembrete.getTitulo();
        String descricao = lembrete.getDescricao();
        String tipo = lembrete.getTipo();
        Integer recorrenciaDias = lembrete.getRecorrenciaDias();
        LocalDate proximaData = lembrete.getDataLembrete();
        Usuario criadoPor = lembrete.getCriadoPor();
        var pet = lembrete.getPet();
        StatusLembrete statusAnterior = lembrete.getStatus();

        int atualizados = lembreteRepository.concluirSeAindaPendente(id, LocalDate.now(), tutorUsuario);
        if (atualizados == 0) {
            throw new ConflitoOperacaoException(
                    "Cuidado não pode ser concluído: status atual é " + statusAnterior + ".");
        }

        if (recorrenciaDias != null) {
            lembreteRepository.save(Lembrete.builder()
                    .titulo(titulo)
                    .descricao(descricao)
                    .dataLembrete(proximaData.plusDays(recorrenciaDias))
                    .tipo(tipo)
                    .status(StatusLembrete.PENDENTE)
                    .recorrenciaDias(recorrenciaDias)
                    .criadoPor(criadoPor)
                    .origemLembrete(Lembrete.builder().id(id).build())
                    .pet(pet)
                    .build());
        }

        return toResponse(findById(id));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CACHE_LEMBRETES, allEntries = true),
        @CacheEvict(value = CACHE_DASHBOARD, allEntries = true)
    })
    public LembreteResponse cancelar(Long id) {
        Lembrete lembrete = findById(id);
        int atualizados = lembreteRepository.cancelarSeAindaPendente(id);
        if (atualizados == 0) {
            throw new ConflitoOperacaoException(
                    "Cuidado não pode ser cancelado: status atual é " + lembrete.getStatus() + ".");
        }
        return toResponse(findById(id));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CACHE_LEMBRETES, allEntries = true),
        @CacheEvict(value = CACHE_DASHBOARD, allEntries = true)
    })
    public LembreteResponse atualizar(Long id, LembreteRequest request) {
        Lembrete lembrete = findById(id);
        Pet pet = petService.findById(request.getPetId());
        lembrete.setTitulo(request.getTitulo());
        lembrete.setDescricao(request.getDescricao());
        lembrete.setDataLembrete(request.getDataLembrete());
        lembrete.setTipo(request.getTipo());
        lembrete.setRecorrenciaDias(request.getRecorrenciaDias());
        lembrete.setPet(pet);
        return toResponse(lembreteRepository.save(lembrete));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CACHE_LEMBRETES, allEntries = true),
        @CacheEvict(value = CACHE_DASHBOARD, allEntries = true)
    })
    public void deletar(Long id) {
        lembreteRepository.delete(findById(id));
    }

    Lembrete findById(Long id) {
        return lembreteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lembrete não encontrado com id: " + id));
    }

    private LembreteResponse toResponse(Lembrete l) {
        return LembreteResponse.builder()
                .id(l.getId())
                .titulo(l.getTitulo())
                .descricao(l.getDescricao())
                .dataLembrete(l.getDataLembrete())
                .tipo(l.getTipo())
                .status(l.getStatus())
                .petId(l.getPet().getId())
                .petNome(l.getPet().getNome())
                .recorrenciaDias(l.getRecorrenciaDias())
                .dataConclusao(l.getDataConclusao())
                .criadoPorNome(l.getCriadoPor() != null ? l.getCriadoPor().getNome() : null)
                .concluidoPorNome(l.getConcluidoPor() != null ? l.getConcluidoPor().getNome() : null)
                .atrasado(l.getStatus() == StatusLembrete.PENDENTE && l.getDataLembrete().isBefore(LocalDate.now()))
                .build();
    }
}
