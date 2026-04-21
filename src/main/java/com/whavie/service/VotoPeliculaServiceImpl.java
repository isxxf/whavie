package com.whavie.service;

import com.whavie.dto.ResultadoRondaDTO;
import com.whavie.model.EstadoSala;
import com.whavie.model.ParticipanteSala;
import com.whavie.model.Sala;
import com.whavie.model.VotoPelicula;
import com.whavie.repository.ParticipanteSalaRepository;
import com.whavie.repository.VotoPeliculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class VotoPeliculaServiceImpl implements VotoPeliculaService {
    private final VotoPeliculaRepository votoPeliculaRepository;
    private final ParticipanteSalaRepository participanteSalaRepository;
    private final SalaService salaService;

    public VotoPeliculaServiceImpl(VotoPeliculaRepository votoPeliculaRepository, ParticipanteSalaRepository participanteSalaRepository, SalaService salaService) {
        this.salaService = salaService;
        this.votoPeliculaRepository = votoPeliculaRepository;
        this.participanteSalaRepository = participanteSalaRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResultadoRondaDTO votarPelicula(Long participanteId, boolean voto) {
        ParticipanteSala participante = participanteSalaRepository.findById(participanteId)
                .orElseThrow(() -> new IllegalArgumentException("Participante no encontrado"));
        Sala sala = participante.getSala();
        if (sala.getEstado() != EstadoSala.EN_VOTACION) {
            throw new IllegalStateException("La sala no está en estado de votación");
        }
        Long tmdbIdActual = sala.getPeliculaActualTmdbId();
        if (tmdbIdActual == null) {
            throw new IllegalStateException("No hay ninguna película en votación");
        }
        if (participanteVotoPelicula(participanteId, tmdbIdActual)) {
            throw new IllegalStateException("Este participante ya votó esta película");
        }
        VotoPelicula votoPelicula = new VotoPelicula();
        votoPelicula.setSala(sala);
        votoPelicula.setParticipanteSala(participante);
        votoPelicula.setTmdbId(tmdbIdActual);
        votoPelicula.setVoto(voto);
        votoPeliculaRepository.save(votoPelicula);
        return salaService.resolverVotacion(sala);
    }

    private boolean participanteVotoPelicula(Long participanteId, Long tmdbId) {
        return votoPeliculaRepository.existsByParticipanteSalaIdAndTmdbId(participanteId, tmdbId);
    }
}
