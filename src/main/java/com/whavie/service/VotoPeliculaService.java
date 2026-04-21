package com.whavie.service;

import com.whavie.dto.ResultadoRondaDTO;


public interface VotoPeliculaService {
    ResultadoRondaDTO votarPelicula(Long participanteId, boolean voto);
}
