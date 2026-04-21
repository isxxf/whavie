package com.whavie.config;

import com.whavie.service.ParticipanteSalaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ParticipanteSalaService participanteSalaService;

    @Value("${app.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    public WebSocketConfig(ParticipanteSalaService participanteSalaService) {
        this.participanteSalaService = participanteSalaService;
    }

    /**
     * Configura las reglas para el intercambio de mensajes por WebSocket.
     * - Latidos (Heartbeats): Configura una tarea automática que mantiene la conexión
     * despierta, comprobando que cliente y servidor sigan comunicándose.
     * - De Servidor a Usuario ("/topic"): Define la ruta por donde el servidor va a
     * enviarle la información a la pantalla de los usuarios.
     * - De Usuario a Servidor ("/app"): Define el prefijo que deben ponerle los usuarios
     * a sus mensajes cuando quieren pedirle al servidor que ejecute alguna acción.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-scheduler-");
        scheduler.initialize();
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(scheduler);
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra los endpoints de STOMP a los que se conectarán los usuarios.
     * - Define la ruta "/ws" como el endpoint principal de conexión.
     * - Permite conexiones desde localhost (útil para desarrollo/CORS).
     * - Agrega un interceptor que copia los atributos de la sesión HTTP estándar a la sesión de WebSocket
     * permitiendo acceder a ellos luego.
     * - Habilita SockJS como mecanismo de respaldo por si el navegador no soporta WebSockets nativos.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    /**
     * Configura el canal de entrada de mensajes del cliente hacia el servidor.
     * - Se añade un interceptor que actúa como una capa de seguridad: intercepta los
     * mensajes antes de ser procesados para validar la conexión inicial.
     * - Revisa que la sesión tenga un ID de usuario válido o un token de invitado que
     * exista en la base de datos a través del servicio inyectado.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                // Solo nos interesa interceptar y validar cuando el cliente intenta conectarse
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    // Verificamos que existan atributos de sesión
                    if (sessionAttributes == null || sessionAttributes.isEmpty()) {
                        throw new IllegalArgumentException("Sesión inválida");
                    }
                    boolean tieneToken = sessionAttributes.get("tokenInvitado") != null;
                    boolean tieneUsuario = sessionAttributes.get("usuarioId") != null;
                    // Debe haber al menos un token de invitado o un ID de usuario
                    if (!tieneToken && !tieneUsuario) {
                        throw new IllegalArgumentException("Credenciales incompletas");
                    }
                    // Si se conecta como invitado (tiene token, pero no usuario),
                    // validamos que el token exista en el sistema.
                    if (tieneToken && !tieneUsuario) {
                        String tokenInvitado = (String) sessionAttributes.get("tokenInvitado");
                        if (!participanteSalaService.existeTokenValido(tokenInvitado)) {
                            throw new IllegalArgumentException("Token de invitado inválido");
                        }
                    }
                }
                return message;
            }
        });
    }
}


