package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.config.InvalidaCaches;
import br.com.fiap.petfamily.dto.request.TutorRequest;
import br.com.fiap.petfamily.dto.response.TutorResponse;
import br.com.fiap.petfamily.entity.Tutor;
import br.com.fiap.petfamily.exception.EmailJaCadastradoException;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.repository.PetRepository;
import br.com.fiap.petfamily.repository.TutorRepository;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import br.com.fiap.petfamily.security.SecurityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TutorService {

    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final PetRepository petRepository;

    private final TutorService self;

    public TutorService(TutorRepository tutorRepository, UsuarioRepository usuarioRepository,
                        PetRepository petRepository, SecurityUtils securityUtils, @Lazy TutorService self) {
        this.tutorRepository = tutorRepository;
        this.usuarioRepository = usuarioRepository;
        this.petRepository = petRepository;
        this.securityUtils = securityUtils;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public Page<TutorResponse> listar(String nome, Pageable pageable) {
        if (nome != null && !nome.isBlank()) {
            return tutorRepository.findByNomeContainingIgnoreCase(nome, pageable).map(this::toResponse);
        }
        return tutorRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tutores", key = "#id")
    public TutorResponse buscarPorIdSemAutorizacao(Long id) {
        return toResponse(findById(id));
    }

    public TutorResponse buscarPorId(Long id) {
        securityUtils.exigirTutorDono(id, "Você só pode acessar o próprio cadastro de tutor.");
        return self.buscarPorIdSemAutorizacao(id);
    }

    @Transactional
    @InvalidaCaches
    public TutorResponse atualizar(Long id, TutorRequest request) {
        securityUtils.exigirTutorDono(id, "Você só pode acessar o próprio cadastro de tutor.");
        Tutor tutor = findById(id);

        String novoEmail = request.getEmail().trim().toLowerCase();
        boolean mudouEmail = !novoEmail.equalsIgnoreCase(tutor.getEmail());
        // Confere TAMBÉM a tabela de usuários (o e-mail é o login e também é único lá).
        if (mudouEmail && (tutorRepository.existsByEmail(novoEmail)
                || usuarioRepository.existsByEmailIgnoreCase(novoEmail))) {
            throw new EmailJaCadastradoException(novoEmail);
        }

        tutor.setNome(request.getNome());
        tutor.setEmail(novoEmail);
        tutor.setTelefone(request.getTelefone());
        Tutor salvo = tutorRepository.save(tutor);

        // BUG corrigido: antes buscava o usuário pelo e-mail NOVO (que ainda não existia),
        // então o login nunca era atualizado. Agora busca pelo vínculo tutor_id.
        usuarioRepository.findByTutorId(id).ifPresent(usuario -> {
            usuario.setNome(request.getNome());
            usuario.setEmail(novoEmail);
            usuarioRepository.save(usuario);
        });

        return toResponse(salvo);
    }

    @Transactional
    @InvalidaCaches
    public void deletar(Long id) {
        securityUtils.exigirTutorDono(id, "Você só pode acessar o próprio cadastro de tutor.");
        Tutor tutor = findById(id);

        // Remove o login vinculado ANTES do tutor, para o token deixar de valer.
        usuarioRepository.findByTutorId(id).ifPresent(usuarioRepository::delete);

        tutorRepository.delete(tutor);
    }

    Tutor findById(Long id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor não encontrado com id: " + id));
    }

    private TutorResponse toResponse(Tutor tutor) {
        return TutorResponse.builder()
                .id(tutor.getId())
                .nome(tutor.getNome())
                .email(tutor.getEmail())
                .telefone(tutor.getTelefone())
                .totalPets((int) petRepository.countByTutorId(tutor.getId()))
                .build();
    }
}