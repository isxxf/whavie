package com.whavie.service;

import com.whavie.dto.ParticipanteSalaDTO;

import java.util.List;

public interface ParticipanteSalaService {
    List<ParticipanteSalaDTO> listarParticipantes(String codigoSala);
    void agregarParticipanteRegistrado(String codigoSala, Long usuarioId);
    String agregarParticipanteInvitado(String codigoSala, String nombreInvitado);
    void eliminarParticipanteRegistrado(String codigoSala, Long usuarioId);
    void eliminarParticipanteInvitado(String tokenInvitado);
    void eliminarParticipantePorAbandono(String codigoSala, String nombre);
    long contarParticipantes(String codigoSala);
    Long buscarParticipanteInvitado(String tokenInvitado);
    Long buscarParticipanteRegistrado(String codigoSala, Long usuarioId);
    boolean existeTokenValido(String tokenInvitado);
}
