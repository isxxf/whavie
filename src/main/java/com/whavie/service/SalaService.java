package com.whavie.service;

import com.whavie.dto.ResultadoRondaDTO;
import com.whavie.dto.SalaDTO;
import com.whavie.model.FiltroSala;
import com.whavie.model.Sala;

import java.util.List;

public interface SalaService {
    String generarCodigoSala();
    Sala crearSala(Long creadorId, int maxParticipantes);
    void unirseSalaRegistrado(String codigo, Long usuarioId);
    String unirseSalaInvitado(String codigo, String nombreInvitado);
    void salirSalaRegistrado(String codigo, Long usuarioId);
    void salirSalaInvitado(String tokenInvitado);
    void eliminarSala(String codigo, Long usuarioId);
    void eliminarSalaAbandono(String codigo);
    Sala obtenerSala(String codigo);
    SalaDTO iniciarVotacion(String codigo, Long usuarioId);
    ResultadoRondaDTO resolverVotacion(Sala sala);
    boolean matchPelicula(Sala sala);
    Long siguientePelicula(Sala sala);
    boolean esAnfitrion(String codigoSala, String username);
    void guardarPeliculasVotacion(String codigo, List<Long> tmdbIds, FiltroSala filtroEntidad);
    void guardarVotacionEnBD(String codigo, Long usuarioId, List<Long> tmdbIds, FiltroSala filtroEntidad);
}
