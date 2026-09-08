package br.com.fiap.petfamily.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RealizarConsultaRequest {

    @NotBlank(message = "Observações do atendimento são obrigatórias")
    @Size(max = 2000, message = "Observações devem ter no máximo 2000 caracteres")
    private String observacoes;
}
