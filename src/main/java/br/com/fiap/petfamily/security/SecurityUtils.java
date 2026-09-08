package br.com.fiap.petfamily.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public UsuarioPrincipal getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança.");
        }
        return principal;
    }

    public Long getTutorIdAutenticadoOuFalha() {
        UsuarioPrincipal principal = getUsuarioAutenticado();
        if (!principal.isTutor() || principal.getTutorId() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Esta operação é restrita a tutores.");
        }
        return principal.getTutorId();
    }

    public void exigirTutorDono(Long tutorIdDoDono, String mensagemSeNegado) {
        UsuarioPrincipal principal = getUsuarioAutenticado();
        if (principal.isTutor() && !tutorIdDoDono.equals(principal.getTutorId())) {
            throw new org.springframework.security.access.AccessDeniedException(mensagemSeNegado);
        }
    }
}
