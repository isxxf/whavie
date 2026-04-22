package com.whavie.controller;

import com.whavie.config.WebSocketEventListener;
import com.whavie.dto.FiltroSalaDTO;
import com.whavie.dto.Pelicula;
import com.whavie.dto.UsuarioDTO;
import com.whavie.model.FiltroSala;
import com.whavie.service.FiltroSalaService;
import com.whavie.service.ParticipanteSalaService;
import com.whavie.service.SalaService;
import com.whavie.service.TMDbService;
import com.whavie.service.UsuarioService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.*;

@Controller
public class SalaWebSocketController {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final WebSocketEventListener webSocketEventListener;
    private final TMDbService tmDbService;
    private final SalaService salaService;
    private final ParticipanteSalaService participanteSalaService;
    private final UsuarioService usuarioService;
    private final FiltroSalaService filtroSalaService;

    public SalaWebSocketController(SimpMessagingTemplate simpMessagingTemplate,
                                   WebSocketEventListener webSocketEventListener,
                                   TMDbService tmDbService,
                                   SalaService salaService,
                                   ParticipanteSalaService participanteSalaService,
                                   UsuarioService usuarioService,
                                   FiltroSalaService filtroSalaService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.webSocketEventListener = webSocketEventListener;
        this.tmDbService = tmDbService;
        this.salaService = salaService;
        this.participanteSalaService = participanteSalaService;
        this.usuarioService = usuarioService;
        this.filtroSalaService = filtroSalaService;
    }

    /**
     * UN USUARIO SE CONECTA AL SOCKET (/app/sala/{codigo})
     * Esto se ejecuta en cuanto el frontend logra abrir la conexión WebSocket en la sala.
     */
    @MessageMapping("/sala/{codigo}")
    public void nuevoParticipante(@DestinationVariable String codigo,
                                  @Payload Map<String, String> payload,
                                  SimpMessageHeaderAccessor headerAccessor,
                                  Principal principal) {
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session == null) {
            throw new IllegalStateException("Sesión WebSocket no disponible");
        }
        // Guardamos usuarioId si es usuario autenticado
        if (principal != null) {
            Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
            session.put("usuarioId", usuarioId);
        }
        String nombre = obtenerNombreUsuario(principal, headerAccessor);
        String avatar = payload.get("avatar");
        boolean eraF5Anfitrion = false;
        boolean eraF5Participante = webSocketEventListener.cancelarBorradoParticipante(codigo, nombre);
        if (salaService.esAnfitrion(codigo, nombre)) {
            eraF5Anfitrion = webSocketEventListener.cancelarBorradoSiExiste(codigo);
        }
        boolean reconexionRapida = eraF5Anfitrion || eraF5Participante;
        // Guardamos datos básicos de sesión
        session.put("nombre", nombre);
        session.put("codigo", codigo);
        if (!reconexionRapida) {
            Map<String, String> mensaje = Map.of(
                    "accion", "NUEVO_USUARIO",
                    "nombre", nombre,
                    "avatar", avatar
            );
            simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, mensaje);
        }
    }

    // Cuando el anfitrión le da al botón "Empezar". Esto es lo que va a buscar las películas.
    @MessageMapping("/sala/{codigo}/iniciar")
    public void iniciarVotacion(@DestinationVariable String codigo, @Payload Map<String, Object> payload,
                                SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session == null) {
            throw new IllegalStateException("Sesión inválida");
        }
        Long usuarioId = (Long) session.get("usuarioId");
        if (usuarioId == null) {
            usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
        }
        long participantesCount = participanteSalaService.contarParticipantes(codigo);
        if (participantesCount < 2) {
            return;
        }
        if (usuarioId == null) {
            throw new IllegalStateException("No se encontró el usuario en sesión");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> filtrosRecibidos = (List<Map<String, String>>) payload.get("filtros");
        FiltroSalaDTO filtroDTO = filtroSalaService.convertirFiltrosDelWebSocket(filtrosRecibidos);
        FiltroSala filtroEntidad = new FiltroSala();
        filtroDTO.updateEntity(filtroEntidad);

        List<Pelicula> peliculas;
        if (filtroDTO.getEpocasIds() != null && !filtroDTO.getEpocasIds().isEmpty()) {
            peliculas = tmDbService.obtenerListaPeliculasFiltroMultiplesEpocas(filtroEntidad, filtroDTO.getEpocasIds(), 20);
        } else {
            peliculas = tmDbService.obtenerListaPeliculasFiltro(filtroEntidad);
        }
        List<Long> peliculasIds = peliculas.stream()
                .map(Pelicula::getTmdbId)
                .toList();

        salaService.guardarVotacionEnBD(codigo, usuarioId, peliculasIds, filtroEntidad);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("accion", "INICIAR_VOTACION");
        simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, (Object) respuesta);
    }

    /**
     * SINCRONIZACIÓN DE FILTROS EN TIEMPO REAL
     * Si el anfitrión tilda algún filtro, este método agarra ese evento y lo muestra
     * inmediatamente al resto de las personas en la sala para que en sus pantallas también
     * muestren el filtro tildado en tiempo real.
     */
    @MessageMapping("/sala/{codigo}/filtros")
    public void aplicarFiltros(@DestinationVariable String codigo, @Payload Map<String, Object> payload) {
        payload.put("accion", "APLICAR_FILTROS");
        simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, (Object) payload);
    }

    /**
     * EFECTO DE CARGA
     * Como buscar las películas puede tardar un par de segundos, mandamos este aviso
     * primero para que el frontend muestre una pantalla de carga y la gente no
     * piense que la app se congeló.
     */
    @MessageMapping("/sala/{codigo}/avisar-inicio")
    public void avisarInicio(@DestinationVariable String codigo) {
        Map<String, String> aviso = Map.of("accion", "MOSTRAR_CARGANDO");
        simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, aviso);
    }

    /**
     * LLEGADA A LA PANTALLA DE VOTACIÓN
     * Al pasar de la sala de espera a la pantalla de votación, el navegador a veces corta
     * y reinicia el WebSocket. Este metodo sirve para que no se desconecten los usuarios
     * al llegar a la pantalla de votación y para registrar que llegaron y no se quedaron en la sala de espera.
     */
    @MessageMapping("/sala/{codigo}/presencia")
    public void registrarPresenciaVotacion(@DestinationVariable String codigo,
                                           SimpMessageHeaderAccessor headerAccessor,
                                           Principal principal) {
        if (headerAccessor.getSessionAttributes() == null) {
            throw new IllegalStateException("Sesión no disponible");
        }

        String nombre = obtenerNombreUsuario(principal, headerAccessor);
        headerAccessor.getSessionAttributes().put("nombre", nombre);
        headerAccessor.getSessionAttributes().put("codigo", codigo);

        try {
            Long participanteId;
            if (principal != null) {
                Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
                participanteId = participanteSalaService.buscarParticipanteRegistrado(codigo, usuarioId);
            } else {
                String tokenInvitado = (String) headerAccessor.getSessionAttributes().get("tokenInvitado");
                if (tokenInvitado == null) {
                    throw new IllegalStateException("Token de invitado no disponible");
                }
                participanteId = participanteSalaService.buscarParticipanteInvitado(tokenInvitado);
            }
            if (participanteId == null) {
                throw new IllegalStateException("No se pudo obtener participante ID para: " + nombre);
            }
            headerAccessor.getSessionAttributes().put("participanteId", participanteId);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo obtener participante para votación: " + e.getMessage(), e);
        }

        webSocketEventListener.cancelarBorradoSiExiste(codigo);
        webSocketEventListener.cancelarBorradoParticipante(codigo, nombre);
    }

    // Metodos auxiliares
    private String obtenerNombreUsuario(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            if (principal instanceof OAuth2AuthenticationToken oauthToken) {
                var oauthUser = oauthToken.getPrincipal();
                String email = oauthUser.getAttribute("email");
                if (email == null) {
                    throw new IllegalStateException("No se pudo obtener el email del usuario de OAuth2.");
                }
                return usuarioService.obtenerUsuarioPorEmail(email)
                        .getUsername()
                        .trim()
                        .toLowerCase();
            }
            return principal.getName().trim().toLowerCase();
        }
        if (headerAccessor.getSessionAttributes() == null) {
            throw new IllegalStateException("Sesión no disponible");
        }
        String nombre = (String) headerAccessor.getSessionAttributes().get("nombreInvitado");
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("Nombre de invitado no disponible");
        }
        return nombre.trim().toLowerCase();
    }

    private Long obtenerUsuarioIdDesdePrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            String email = oauthToken.getPrincipal().getAttribute("email");
            if (email == null) {
                throw new IllegalStateException("Email no disponible en OAuth2");
            }
            return usuarioService.obtenerUsuarioPorEmail(email).getId();
        }
        return usuarioService.obtenerUsuarioPorUsername(principal.getName()).getId();
    }
}