package com.whavie.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "votos_pelicula",
        uniqueConstraints = @UniqueConstraint(columnNames = {"participante_sala_id", "tmdb_id"}))
public class VotoPelicula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tmdbId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_sala_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ParticipanteSala participanteSala;

    private boolean voto;

    public VotoPelicula() {
    }

    public VotoPelicula(Long tmdbId, Sala sala, boolean voto) {
        this.tmdbId = tmdbId;
        this.sala = sala;
        this.voto = voto;
    }

    public Long getId() {
        return id;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public ParticipanteSala getParticipanteSala() {
        return participanteSala;
    }

    public void setParticipanteSala(ParticipanteSala participanteSala) {
        this.participanteSala = participanteSala;
    }

    public boolean isVoto() {
        return voto;
    }

    public void setVoto(boolean voto) {
        this.voto = voto;
    }

}
