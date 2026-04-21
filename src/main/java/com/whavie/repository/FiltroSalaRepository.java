package com.whavie.repository;

import com.whavie.model.FiltroSala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FiltroSalaRepository extends JpaRepository<FiltroSala, Long> {
    Optional<FiltroSala> findBySalaCodigo(String codigoSala);
}
