package br.com.fiap.petfamily.repository;

import br.com.fiap.petfamily.entity.ConsultaSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaSlotRepository extends JpaRepository<ConsultaSlot, Long> {

    void deleteByConsultaId(Long consultaId);
}
