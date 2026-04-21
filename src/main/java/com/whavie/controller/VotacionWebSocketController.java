package com.whavie.controller;

import com.whavie.dto.ResultadoRondaDTO;
import com.whavie.service.VotoPeliculaService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class VotacionWebSocketController {
    private final VotoPeliculaService votoPeliculaService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public VotacionWebSocketController(VotoPeliculaService votoPeliculaService,
                                       SimpMessagingTemplate simpMessagingTemplate) {
        this.votoPeliculaService = votoPeliculaService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * Procesa el voto de un participante en una sala.
     * Este método se dispara inmediatamente después de que un usuario envía su voto,
     * se encarga de actualizar el estado de la ronda y notificar
     * a todos los participantes si la ronda ha finalizado.
     */
    @MessageMapping("/sala/{codigo}/votar")
    public void procesarVoto(@DestinationVariable String codigo, @Payload Map<String, Object> payload,
                             SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            throw new IllegalStateException("Sesión inválida");
        }
        Long participanteId = (Long) headerAccessor.getSessionAttributes().get("participanteId");
        if (participanteId == null) {
            throw new IllegalStateException("Sesión inválida, no se encontró el participante");
        }
        boolean esLike = "LIKE".equalsIgnoreCase((String) payload.get("voto"));
        ResultadoRondaDTO resultado = votoPeliculaService.votarPelicula(participanteId, esLike);
        if (!resultado.getAccion().equals("ESPERANDO")) {
            simpMessagingTemplate.convertAndSend("/topic/sala/" + codigo, resultado);
        }
    }
}
