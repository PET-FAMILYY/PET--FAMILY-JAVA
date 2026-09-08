package br.com.fiap.petfamily.repository;

import br.com.fiap.petfamily.entity.Consulta;
import br.com.fiap.petfamily.entity.Consulta.StatusConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    Page<Consulta> findByStatus(StatusConsulta status, Pageable pageable);

    Page<Consulta> findByPetTutorId(Long tutorId, Pageable pageable);

    Page<Consulta> findByPetTutorIdAndStatus(Long tutorId, StatusConsulta status, Pageable pageable);

    @Query("SELECT c FROM Consulta c WHERE c.data >= :hoje ORDER BY c.data ASC, c.horario ASC")
    Page<Consulta> findConsultasFuturas(@Param("hoje") LocalDate hoje, Pageable pageable);

    long countByStatus(StatusConsulta status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Consulta c SET c.status = br.com.fiap.petfamily.entity.Consulta$StatusConsulta.REALIZADA, " +
           "c.observacoes = :observacoes WHERE c.id = :id AND c.status = br.com.fiap.petfamily.entity.Consulta$StatusConsulta.AGENDADA")
    int realizarSeAgendada(@Param("id") Long id, @Param("observacoes") String observacoes);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Consulta c SET c.status = br.com.fiap.petfamily.entity.Consulta$StatusConsulta.CANCELADA " +
           "WHERE c.id = :id AND c.status = br.com.fiap.petfamily.entity.Consulta$StatusConsulta.AGENDADA")
    int cancelarSeAgendada(@Param("id") Long id);
}
