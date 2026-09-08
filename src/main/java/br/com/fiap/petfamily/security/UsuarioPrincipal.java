package br.com.fiap.petfamily.security;

import br.com.fiap.petfamily.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long usuarioId;
    private final String email;
    private final String senhaHash;
    private final Usuario.Role role;
    private final Long tutorId;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.role = usuario.getRole();
        this.tutorId = usuario.getTutor() != null ? usuario.getTutor().getId() : null;
    }

    public boolean isTutor() {
        return role == Usuario.Role.TUTOR;
    }

    public boolean isVeterinario() {
        return role == Usuario.Role.VETERINARIO;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
