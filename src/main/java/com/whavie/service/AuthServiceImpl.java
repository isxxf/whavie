package com.whavie.service;

import com.whavie.dto.RegisterRequest;
import com.whavie.dto.UsuarioDTO;
import com.whavie.exception.BadRequestException;
import com.whavie.model.AuthProvider;
import com.whavie.model.PasswordResetToken;
import com.whavie.model.Usuario;
import com.whavie.repository.PasswordResetTokenRepository;
import com.whavie.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 30;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           EmailService emailService, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    public UsuarioDTO registerLocal(RegisterRequest registerRequest) {
        String emailNormalizado = registerRequest.getEmail().trim().toLowerCase();
        String usernameNormalizado = registerRequest.getUsername().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new BadRequestException("El email ya está registrado");
        }
        if (usuarioRepository.existsByUsername(usernameNormalizado)) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }
        Usuario nuevoUsuario = new Usuario(
                usernameNormalizado,
                emailNormalizado,
                passwordEncoder.encode(registerRequest.getPassword()),
                AuthProvider.LOCAL,
                null
        );
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        emailService.enviarBienvenida(usuarioGuardado.getEmail(), usuarioGuardado.getUsername());
        return UsuarioDTO.fromEntity(usuarioGuardado);
    }

    @Override
    @Transactional
    public void solicitarRecuperacionPassword(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElse(null);
        if (usuario == null || usuario.getAuthProvider() == AuthProvider.GOOGLE) {
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime nuevaExpiracion = LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES);
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByUsuarioId(usuario.getId())
                .orElse(new PasswordResetToken());

        passwordResetToken.setToken(token);
        passwordResetToken.setUsuario(usuario);
        passwordResetToken.setFechaExpiracion(nuevaExpiracion);
        passwordResetTokenRepository.save(passwordResetToken);

        emailService.enviarRecuperacionPassword(usuario.getEmail(), usuario.getUsername(), token);
    }

    @Override
    @Transactional
    public void cambiarPasswordConToken(String token, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.trim().length() < 5) {
            throw new BadRequestException("La contraseña debe tener mínimo 5 caracteres");
        }
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("El link de recuperación es inválido"));
        if (resetToken.estaExpirado()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("El link ha expirado. Por favor, solicita uno nuevo.");
        }
        Usuario usuario = resetToken.getUsuario();
        if (passwordEncoder.matches(nuevaPassword, usuario.getPassword())) {
            throw new BadRequestException("La nueva contraseña no puede ser igual a la anterior");
        }
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        passwordResetTokenRepository.delete(resetToken);
    }
}
