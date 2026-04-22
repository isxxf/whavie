package com.whavie.config;

import com.whavie.service.ParticipanteSalaService;
import com.whavie.service.SalaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.*;

@Component
public class WebSocketEventListener {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SalaService salaService;
    private final ParticipanteSalaService participanteSalaService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> tareasDeBorrado = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate simpMessagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate simpMessagingTemplate, SalaService salaService, ParticipanteSalaService participanteSalaService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.salaService = salaService;
        this.participanteSalaService = participanteSalaService;
    }

    /**
     * Si el creador de la sala se desconectó pero regresó antes de que pasen 5 segundos,
     * usamos este método para frenar la cuenta regresiva que iba a borrar la sala.
     */
    public boolean cancelarBorradoSiExiste(String codigo) {
        ScheduledFuture<?> tarea = tareasDeBorrado.remove(codigo);
        if (tarea != null) {
            tarea.cancel(false);
            return true;
        }
        return false;
    }

    /**
     * Igual que el método anterior, pero frena la cuenta regresiva que iba a
     * expulsar a un participante normal de la sala.
     */
    public boolean cancelarBorradoParticipante(String codigo, String nombre) {
        String clave = codigo + ":" + nombre;
        ScheduledFuture<?> tarea = tareasDeBorrado.remove(clave);
        if (tarea != null) {
            tarea.cancel(false);
            return true;
        }
        return false;
    }

    /**
     * Este método se dispara automáticamente cada vez que alguien pierde la conexión WebSocket
     * (ya sea porque cerró la pestaña, recargó la página o se le cortó el internet).
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // Obtenemos los datos de la sesión del usuario que se acaba de desconectar
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String nombre = (String) accessor.getSessionAttributes().get("nombre");
        String codigo = (String) accessor.getSessionAttributes().get("codigo");
        // Si sabemos quién es y en qué sala estaba, verificamos si era el anfitrión o
        // un participante normal y programamos la tarea de borrado correspondiente
        if (nombre != null && codigo != null) {
            boolean esAnfitrion = false;
            try {
                esAnfitrion = salaService.esAnfitrion(codigo, nombre);
            } catch (Exception e) {
                log.warn("Error al verificar anfitrión en sala {}: {}", codigo, e.getMessage());
            }
            if (esAnfitrion) {
                // Si el usuario que se fue era el anfitrion,
                // tiene 5 segundos para volver a entrar, si no vuelve en ese tiempo,
                // se borra la sala y se avisa a los participantes que la sesión ha finalizado.
                ScheduledFuture<?> tarea = scheduler.schedule(() -> {
                    try {
                        if (salaService.obtenerSala(codigo).getEstado() == com.whavie.model.EstadoSala.FINALIZADA) {
                            tareasDeBorrado.remove(codigo);
                            return;
                        }
                        // Avisamos a todos los que estaban en la sala que el anfitrión se fue y que la sala se cerrará
                        simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, (Object) Map.of(
                                "accion", "SALA_CERRADA",
                                "mensaje", "El anfitrión ha abandonado la sala. La sesión ha finalizado."
                        ));
                        // Borramos la sala de la base de datos
                        salaService.eliminarSalaAbandono(codigo);
                        tareasDeBorrado.remove(codigo);
                    } catch (Exception e) {
                        log.error("Error al eliminar sala {}: {}", codigo, e.getMessage());
                    }
                }, 30, TimeUnit.SECONDS);
                // Guardamos la tarea programada para poder cancelarla si el anfitrión regresa antes de que se ejecute
                tareasDeBorrado.put(codigo, tarea);
            } else {
                // Si el usuario que se fue era un participante normal,
                // tiene 8 segundos para volver a entrar, si no vuelve en ese tiempo
                // se lo expulsa de la sala y se avisa a los demás participantes que esa persona abandonó la sala.
                String claveParticipante = codigo + ":" + nombre;
                ScheduledFuture<?> tarea = scheduler.schedule(() -> {
                    try {
                        participanteSalaService.eliminarParticipantePorAbandono(codigo, nombre);
                        // Avisamos a todos los que estaban en la sala que el participante se fue
                        simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, (Object) Map.of(
                                "accion", "PARTICIPANTE_ABANDONO", "nombre", nombre
                        ));
                        tareasDeBorrado.remove(claveParticipante);
                    } catch (Exception e) {
                        log.error("Error al eliminar participante de sala {}: {}", codigo, e.getMessage());
                    }
                }, 30, TimeUnit.SECONDS);
                tareasDeBorrado.put(claveParticipante, tarea);
            }
        }
    }
}

