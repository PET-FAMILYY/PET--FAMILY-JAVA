package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.dto.request.PetRequest;
import br.com.fiap.petfamily.dto.response.PetResponse;
import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.entity.Pet;
import br.com.fiap.petfamily.entity.Tutor;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.repository.LembreteRepository;
import br.com.fiap.petfamily.repository.PetRepository;
import br.com.fiap.petfamily.security.SecurityUtils;
import br.com.fiap.petfamily.security.UsuarioPrincipal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final TutorService tutorService;
    private final LembreteRepository lembreteRepository;
    private final SecurityUtils securityUtils;

    private final PetService self;

    public PetService(PetRepository petRepository, TutorService tutorService,
                       LembreteRepository lembreteRepository, SecurityUtils securityUtils,
                       @Lazy PetService self) {
        this.petRepository = petRepository;
        this.tutorService = tutorService;
        this.lembreteRepository = lembreteRepository;
        this.securityUtils = securityUtils;
        this.self = self;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "pets", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public PetResponse criar(PetRequest request) {
        Long tutorId = securityUtils.getTutorIdAutenticadoOuFalha();
        Tutor tutor = tutorService.findById(tutorId);
        Pet pet = Pet.builder()
                .nome(request.getNome())
                .especie(request.getEspecie())
                .raca(request.getRaca())
                .idade(request.getIdade())
                .peso(request.getPeso())
                .observacoesSaude(request.getObservacoesSaude())
                .tutor(tutor)
                .build();
        return toResponse(petRepository.save(pet));
    }

    @Transactional(readOnly = true)
    public Page<PetResponse> listar(Long tutorId, String especie, Pageable pageable) {
        UsuarioPrincipal principal = securityUtils.getUsuarioAutenticado();
        Long tutorFiltro = principal.isTutor() ? principal.getTutorId() : tutorId;

        if (tutorFiltro != null) {
            return petRepository.findByTutorId(tutorFiltro, pageable).map(this::toResponse);
        }
        if (especie != null && !especie.isBlank()) {
            return petRepository.findByEspecieIgnoreCase(especie, pageable).map(this::toResponse);
        }
        return petRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pets", key = "#id")
    public PetResponse buscarPorIdSemAutorizacao(Long id) {
        return toResponse(findById(id));
    }

    public PetResponse buscarPorId(Long id) {
        PetResponse response = self.buscarPorIdSemAutorizacao(id);
        securityUtils.exigirTutorDono(response.getTutorId(), "Este pet não pertence ao tutor autenticado.");
        return response;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "pets", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public PetResponse atualizar(Long id, PetRequest request) {
        Pet pet = findById(id);
        securityUtils.exigirTutorDono(pet.getTutor().getId(), "Este pet não pertence ao tutor autenticado.");
        pet.setNome(request.getNome());
        pet.setEspecie(request.getEspecie());
        pet.setRaca(request.getRaca());
        pet.setIdade(request.getIdade());
        pet.setPeso(request.getPeso());
        pet.setObservacoesSaude(request.getObservacoesSaude());
        return toResponse(petRepository.save(pet));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "pets", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void deletar(Long id) {
        Pet pet = findById(id);
        securityUtils.exigirTutorDono(pet.getTutor().getId(), "Este pet não pertence ao tutor autenticado.");
        petRepository.delete(pet);
    }

    Pet findById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado com id: " + id));
    }

    private PetResponse toResponse(Pet pet) {
        long lembretesPendentes = lembreteRepository.findByPetIdAndStatus(pet.getId(), StatusLembrete.PENDENTE).size();
        return PetResponse.builder()
                .id(pet.getId())
                .nome(pet.getNome())
                .especie(pet.getEspecie())
                .raca(pet.getRaca())
                .idade(pet.getIdade())
                .peso(pet.getPeso())
                .observacoesSaude(pet.getObservacoesSaude())
                .tutorId(pet.getTutor().getId())
                .tutorNome(pet.getTutor().getNome())
                .totalConsultas(pet.getConsultas().size())
                .totalLembretesPendentes((int) lembretesPendentes)
                .build();
    }
}
