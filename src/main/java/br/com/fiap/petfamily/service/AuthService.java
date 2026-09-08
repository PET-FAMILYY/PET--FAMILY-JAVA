package br.com.fiap.petfamily.service;

import br.com.fiap.petfamily.dto.request.LoginRequest;
import br.com.fiap.petfamily.dto.request.RegistroRequest;
import br.com.fiap.petfamily.dto.response.LoginResponse;
import br.com.fiap.petfamily.dto.response.UsuarioResponse;
import br.com.fiap.petfamily.entity.Tutor;
import br.com.fiap.petfamily.entity.Usuario;
import br.com.fiap.petfamily.exception.EmailJaCadastradoException;
import br.com.fiap.petfamily.repository.TutorRepository;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import br.com.fiap.petfamily.security.JwtService;
import br.com.fiap.petfamily.security.SecurityUtils;
import br.com.fiap.petfamily.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityUtils securityUtils;

    @Transactional
    public LoginResponse registrar(RegistroRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email) || tutorRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        Tutor tutor = tutorRepository.save(Tutor.builder()
                .nome(request.getNome().trim())
                .email(email)
                .telefone(request.getTelefone())
                .build());

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(request.getNome().trim())
                .email(email)
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(Usuario.Role.TUTOR)
                .tutor(tutor)
                .criadoEm(LocalDateTime.now())
                .build());

        return gerarLoginResponse(usuario);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getSenha()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        return gerarLoginResponse(usuario);
    }

    public UsuarioResponse me() {
        UsuarioPrincipal principal = securityUtils.getUsuarioAutenticado();
        Usuario usuario = usuarioRepository.findById(principal.getUsuarioId())
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));
        return UsuarioResponse.builder()
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .role(usuario.getRole())
                .tutorId(usuario.getTutor() != null ? usuario.getTutor().getId() : null)
                .build();
    }

    private LoginResponse gerarLoginResponse(Usuario usuario) {
        Long tutorId = usuario.getTutor() != null ? usuario.getTutor().getId() : null;
        String token = jwtService.gerarToken(usuario.getId(), usuario.getEmail(), usuario.getRole().name(), tutorId);
        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .expiraEmMs(jwtService.getExpirationMs())
                .usuarioId(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .role(usuario.getRole())
                .tutorId(tutorId)
                .build();
    }
}
