package br.com.fiap.petfamily.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PetRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "Espécie é obrigatória")
    @Size(max = 100, message = "Espécie deve ter no máximo 100 caracteres")
    private String especie;

    @Size(max = 100, message = "Raça deve ter no máximo 100 caracteres")
    private String raca;

    @Positive(message = "Idade deve ser um valor positivo")
    private Integer idade;

    @Positive(message = "Peso deve ser um valor positivo")
    private Double peso;

    @Size(max = 2000, message = "Observações devem ter no máximo 2000 caracteres")
    private String observacoesSaude;
}
