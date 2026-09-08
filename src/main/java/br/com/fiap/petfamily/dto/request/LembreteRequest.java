package br.com.fiap.petfamily.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LembreteRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
    private String titulo;

    @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
    private String descricao;

    @NotNull(message = "Data do cuidado é obrigatória")
    @Future(message = "Data do cuidado deve ser futura")
    private LocalDate dataLembrete;

    @NotBlank(message = "Tipo é obrigatório")
    @Size(max = 100, message = "Tipo deve ter no máximo 100 caracteres")
    private String tipo;

    @NotNull(message = "ID do pet é obrigatório")
    private Long petId;

    @Min(value = 1, message = "Recorrência deve ser de pelo menos 1 dia")
    private Integer recorrenciaDias;
}
