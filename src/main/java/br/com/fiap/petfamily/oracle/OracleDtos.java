package br.com.fiap.petfamily.oracle;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** DTOs dos endpoints que chamam a package PKG_PETFAMILY_S4. */
public final class OracleDtos {

    private OracleDtos() {}

    public record TutorOracleRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
            String telefone) {}

    public record PetOracleRequest(
            @NotNull(message = "ID do tutor é obrigatório") Long tutorId,
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "Espécie é obrigatória") String especie) {}

    public record AgendamentoOracleRequest(
            @NotNull(message = "ID do pet é obrigatório") Long petId,
            @NotNull(message = "Data é obrigatória") LocalDate data,
            @NotNull(message = "Horário é obrigatório") LocalTime horario,
            @NotBlank(message = "Tipo da consulta é obrigatório") String tipo) {}

    public record StatusOracleRequest(
            @NotBlank(message = "Status é obrigatório") String status) {}

    public record IdResponse(Long id) {}

    public record QuantidadeResponse(Long tutorId, long quantidade) {}

    public record StatusValidoResponse(String status, boolean valido) {}

    public record ExportacaoResponse(String colecao, String arquivo, int bytes) {}
}