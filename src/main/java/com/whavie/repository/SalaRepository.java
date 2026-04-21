package com.whavie.repository;

import com.whavie.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {
    boolean existsByCodigo(String codigo);
    Optional<Sala> findByCodigo(String codigo);
    void deleteByCodigo(String codigo);
    List<Sala> findByFechaCreacionBefore(LocalDateTime fecha);
}
