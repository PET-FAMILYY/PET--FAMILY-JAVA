package br.com.fiap.petfamily.repository;

import br.com.fiap.petfamily.entity.Lembrete;
import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LembreteRepository extends JpaRepository<Lembrete, Long> {

    Page<Lembrete> findByStatus(StatusLembrete status, Pageable pageable);

    List<Lembrete> findByPetIdAndStatus(Long petId, StatusLembrete status);

    List<Lembrete> findByPetTutorId(Long tutorId);

    Page<Lembrete> findByPetTutorId(Long tutorId, Pageable pageable);

    Page<Lembrete> findByPetTutorIdAndStatus(Long tutorId, StatusLembrete status, Pageable pageable);

    long countByStatus(StatusLembrete status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lembrete l SET l.status = br.com.fiap.petfamily.entity.Lembrete$StatusLembrete.CONCLUIDO, " +
           "l.dataConclusao = :dataConclusao, l.concluidoPor = :usuario " +
           "WHERE l.id = :id AND l.status = br.com.fiap.petfamily.entity.Lembrete$StatusLembrete.PENDENTE")
    int concluirSeAindaPendente(@Param("id") Long id,
                                 @Param("dataConclusao") LocalDate dataConclusao,
                                 @Param("usuario") Usuario usuario);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lembrete l SET l.status = br.com.fiap.petfamily.entity.Lembrete$StatusLembrete.CANCELADO " +
           "WHERE l.id = :id AND l.status = br.com.fiap.petfamily.entity.Lembrete$StatusLembrete.PENDENTE")
    int cancelarSeAindaPendente(@Param("id") Long id);
}
