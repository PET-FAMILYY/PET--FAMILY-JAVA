package br.com.fiap.petfamily.dto.response;

import br.com.fiap.petfamily.entity.Usuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tipo;
    private long expiraEmMs;
    private Long usuarioId;
    private String email;
    private String nome;
    private Usuario.Role role;
    private Long tutorId;
}
