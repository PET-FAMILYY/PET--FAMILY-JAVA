package br.com.fiap.petfamily.repository;

import br.com.fiap.petfamily.entity.ConsultaSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ConsultaSlotRepository extends JpaRepository<ConsultaSlot, Long> {

    void deleteByConsultaId(Long consultaId);

    boolean existsByDataAndHorario(LocalDate data, LocalTime horario);
}