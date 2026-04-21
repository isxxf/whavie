package com.whavie.service;

import com.whavie.model.AuthProvider;
import com.whavie.model.Usuario;
import com.whavie.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {
    private static final Logger log = LoggerFactory.getLogger(OAuth2UserServiceImpl.class);
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public OAuth2UserServiceImpl(UsuarioRepository usuarioRepository, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    /**
     * Este es el método que se dispara automáticamente cuando Google nos manda los datos.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // super.loadUser() va a los servidores de Google
        // y trae toda la información pública del usuario.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        // Extraemos los datos exactos que nos interesan
        String email = oAuth2User.getAttribute("email");
        String nombre = oAuth2User.getAttribute("name");
        String avatar = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");
        // Limpiamos el email por seguridad
        String emailNormalizado = email != null ? email.trim().toLowerCase() : null;

        try {
            // Le preguntamos a la base de datos si ya existe alguien con este mail
            usuarioRepository.findByEmailOrUsername(emailNormalizado, emailNormalizado).orElseGet(() -> {
                // El usuario es nuevo
                try {
                    // Como en Google no hay "nombres de usuario",
                    // inventamos uno a partir de su nombre real.
                    String username = generarUsername(nombre);
                    // Creamos el nuevo usuario.
                    // La contraseña se pasa como 'null' porque el inicio
                    // de sesión dependerá siempre de Google, no de nuestra base de datos.
                    Usuario nuevoUsuario = new Usuario(
                            username, emailNormalizado, null, AuthProvider.GOOGLE, avatar
                    );
                    nuevoUsuario.setGoogleId(googleId);
                    // Guardamos en la base de datos
                    Usuario guardado = usuarioRepository.save(nuevoUsuario);
                    try {
                        emailService.enviarBienvenida(emailNormalizado, username);
                    } catch (Exception e) {
                        log.warn("Advertencia al enviar email de bienvenida a {}: {}", emailNormalizado, e.getMessage());
                    }
                    log.info("Nuevo usuario OAuth2 creado: {}", username);
                    return guardado;
                } catch (Exception e) {
                    log.error("Error al crear usuario OAuth2 con email {}: {}", emailNormalizado, e.getMessage(), e);
                    throw new OAuth2AuthenticationException("No se pudo crear la cuenta. Intenta nuevamente.");
                }
            });
        } catch (Exception e) {
            log.error("Error crítico en autenticación OAuth2: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Error en la autenticación OAuth2");
        }
        return oAuth2User;
    }

    /**
     * Metodo auxiliar: Creamos los usernames
     * Toma el nombre de Google y lo convierte en un username válido para nuestra base de datos.
     * Si el usuario creado ya existe en nuestra base de datos, le agrega números
     * hasta encontrar uno libre ("juanperez1", "juanperez2", etc.).
     */
    private String generarUsername(String nombre) {
        String base = nombre.toLowerCase().replaceAll("\\s+", "");
        String username = base;
        int i = 1;
        while (usuarioRepository.existsByUsername(username)) {
            username = base + i;
            i++;
        }
        return username;
    }
}