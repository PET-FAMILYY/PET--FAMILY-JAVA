package br.com.fiap.petfamily.repository;

import br.com.fiap.petfamily.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    Page<Pet> findByTutorId(Long tutorId, Pageable pageable);

    Page<Pet> findByEspecieIgnoreCase(String especie, Pageable pageable);

    long countByTutorId(Long tutorId);

    Page<Pet> findByTutorIdAndEspecieIgnoreCase(Long tutorId, String especie, Pageable pageable);
}
