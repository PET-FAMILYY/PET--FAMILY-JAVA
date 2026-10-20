package br.com.fiap.petfamily.security;

import br.com.fiap.petfamily.entity.Usuario;
import br.com.fiap.petfamily.exception.ResourceNotFoundException;
import br.com.fiap.petfamily.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioAtual {

    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;

    public Usuario entidade() {
        Long id = securityUtils.getUsuarioAutenticado().getUsuarioId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }
}
