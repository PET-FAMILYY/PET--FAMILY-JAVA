package br.com.fiap.petfamily.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InteracaoIARequest {

    @NotBlank(message = "Pergunta é obrigatória")
    @Size(max = 2000, message = "Pergunta deve ter no máximo 2000 caracteres")
    private String pergunta;

    @Size(max = 100, message = "Categoria deve ter no máximo 100 caracteres")
    private String categoria;

    @NotNull(message = "ID do pet é obrigatório")
    private Long petId;
}