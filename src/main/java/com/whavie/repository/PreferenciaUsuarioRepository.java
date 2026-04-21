package com.whavie.repository;

import com.whavie.model.PreferenciaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferenciaUsuarioRepository extends JpaRepository<PreferenciaUsuario, Long> {
    Optional<PreferenciaUsuario> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioId(Long usuarioId);
}
