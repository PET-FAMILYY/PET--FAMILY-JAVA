package br.com.fiap.petfamily.dto.response;

import br.com.fiap.petfamily.entity.Consulta.StatusConsulta;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ConsultaResponse {
    private Long id;
    private LocalDate data;
    private LocalTime horario;
    private String tipoConsulta;
    private StatusConsulta status;
    private String observacoes;
    private Long petId;
    private String petNome;
    private Long tutorId;
    private String tutorNome;
}
