package com.whavie.repository;

import com.whavie.model.ParticipanteSala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipanteSalaRepository extends JpaRepository<ParticipanteSala, Long> {
    boolean existsByUsuarioIdAndSalaId(Long usuarioId, Long salaId);
    Optional<ParticipanteSala> findByTokenInvitado(String tokenInvitado);
    Optional<ParticipanteSala> findByUsuarioIdAndSalaId(Long usuarioId, Long salaId);
    @Query("SELECT p FROM ParticipanteSala p LEFT JOIN p.usuario u WHERE p.sala.id = :salaId AND (p.nombreInvitado = :nombre OR u.username = :nombre)")
    Optional<ParticipanteSala> findByNombreAndSalaId(@Param("nombre") String nombre, @Param("salaId") Long salaId);
    long countBySalaId(Long salaId);
    @Query("SELECT p FROM ParticipanteSala p LEFT JOIN FETCH p.usuario WHERE p.sala.id = :salaId")
    List<ParticipanteSala> findBySalaIdWithUsuario(Long salaId);
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ParticipanteSala p " +
            "WHERE p.sala.id = :salaId " +
            "AND p.nombreInvitado = :nombre")
    boolean existsByNombreInvitadoAndSalaId(@Param("nombre") String nombreInvitado, @Param("salaId") Long salaId);
    @Query("SELECT p FROM ParticipanteSala p WHERE p.usuario.id = :usuarioId AND p.sala.codigo = :codigoSala")
    Optional<ParticipanteSala> findByUsuarioIdAndSalaCodigo(@Param("usuarioId") Long usuarioId,
                                                             @Param("codigoSala") String codigoSala);
    @Query("SELECT COUNT(p) > 0 FROM ParticipanteSala p WHERE p.sala.id = :salaId AND " +
            "(p.nombreInvitado = :nombre OR " +
            "(p.usuario IS NOT NULL AND LOWER(p.usuario.username) = :nombre))")
    boolean existeNombreEnSala(@Param("nombre") String nombre, @Param("salaId") Long salaId);
}
