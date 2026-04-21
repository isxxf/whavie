package com.whavie.repository;

import com.whavie.model.VotoPelicula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoPeliculaRepository extends JpaRepository<VotoPelicula, Long> {
     long countBySalaIdAndTmdbIdAndVotoTrue(Long salaId, Long tmdbId);
     long countBySalaIdAndTmdbId(Long salaId, Long tmdbId);
     boolean existsByParticipanteSalaIdAndTmdbId(Long participanteSalaId, Long tmdbId);
     void deleteBySalaId(Long salaId);
}
