package br.com.fiap.petfamily.dto.response;

import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LembreteResponse {
    private Long id;
    private String titulo;
    private String descricao;
    private LocalDate dataLembrete;
    private String tipo;
    private StatusLembrete status;
    private Long petId;
    private String petNome;
    private Integer recorrenciaDias;
    private LocalDate dataConclusao;
    private String criadoPorNome;
    private String concluidoPorNome;
    private boolean atrasado;
}
