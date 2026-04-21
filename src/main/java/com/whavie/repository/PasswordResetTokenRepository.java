package com.whavie.repository;

import com.whavie.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);
}
