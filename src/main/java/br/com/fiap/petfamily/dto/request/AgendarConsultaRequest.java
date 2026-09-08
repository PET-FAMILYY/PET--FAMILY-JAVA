package br.com.fiap.petfamily.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AgendarConsultaRequest {

    @NotNull(message = "ID do pet é obrigatório")
    private Long petId;

    @NotBlank(message = "Tipo da consulta é obrigatório")
    @Size(max = 150, message = "Tipo da consulta deve ter no máximo 150 caracteres")
    private String tipoConsulta;

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    @NotNull(message = "Horário é obrigatório")
    private LocalTime horario;

    @Size(max = 2000, message = "Observações devem ter no máximo 2000 caracteres")
    private String observacoes;
}
