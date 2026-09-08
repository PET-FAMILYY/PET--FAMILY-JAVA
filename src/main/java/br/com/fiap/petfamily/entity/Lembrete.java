package br.com.fiap.petfamily.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "lembretes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "pet")
public class Lembrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private LocalDate dataLembrete;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusLembrete status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "recorrencia_dias")
    private Integer recorrenciaDias;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concluido_por_usuario_id")
    private Usuario concluidoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origem_lembrete_id")
    private Lembrete origemLembrete;

    public enum StatusLembrete {
        PENDENTE, CONCLUIDO, CANCELADO
    }
}
