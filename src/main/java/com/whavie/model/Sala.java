package com.whavie.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Sala {
    public static final int MAX_PARTICIPANTES_DEFAULT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 7)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    @Enumerated(EnumType.STRING)
    private EstadoSala estado;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "sala", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipanteSala> participantes = new ArrayList<>();

    @Version
    private Long version;

    private int maxParticipantes = MAX_PARTICIPANTES_DEFAULT;

    @Column(name = "pelicula_actual_tmdb_id")
    private Long peliculaActualTmdbId;

    private int peliculasMostradas;

    @ElementCollection
    @CollectionTable(name = "sala_historial_peliculas", joinColumns = @JoinColumn(name = "sala_id"))
    @Column(name = "tmdb_id")
    private List<Long> historialPeliculasIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "sala_peliculas_votacion", joinColumns = @JoinColumn(name = "sala_id"))
    @Column(name = "tmdb_id")
    private List<Long> peliculasParaVotarIds = new ArrayList<>();

    @OneToOne(mappedBy = "sala", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FiltroSala filtroSala;

    public Sala() {
    }

    public Sala(String codigo, Usuario creador, EstadoSala estado) {
        this.codigo = codigo;
        this.creador = creador;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Usuario getCreador() {
        return creador;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }

    public EstadoSala getEstado() {
        return estado;
    }

    public void setEstado(EstadoSala estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<ParticipanteSala> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<ParticipanteSala> participantes) {
        this.participantes = participantes;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public int getMaxParticipantes() {
        return maxParticipantes;
    }

    public void setMaxParticipantes(int maxParticipantes) {
        this.maxParticipantes = maxParticipantes;
    }

    public Long getPeliculaActualTmdbId() {
        return peliculaActualTmdbId;
    }

    public void setPeliculaActualTmdbId(Long peliculaActualTmdbId) {
        this.peliculaActualTmdbId = peliculaActualTmdbId;
    }

    public int getPeliculasMostradas() {
        return peliculasMostradas;
    }

    public void setPeliculasMostradas(int peliculasMostradas) {
        this.peliculasMostradas = peliculasMostradas;
    }

    public FiltroSala getFiltroSala() {
        return filtroSala;
    }

    public void setFiltroSala(FiltroSala filtroSala) {
        this.filtroSala = filtroSala;
    }

    public List<Long> getHistorialPeliculasIds() {
        return historialPeliculasIds;
    }

    public void setHistorialPeliculasIds(List<Long> historialPeliculasIds) {
        this.historialPeliculasIds = historialPeliculasIds;
    }

    public List<Long> getPeliculasParaVotarIds() {
        return peliculasParaVotarIds;
    }

    public void setPeliculasParaVotarIds(List<Long> peliculasParaVotarIds) {
        this.peliculasParaVotarIds = peliculasParaVotarIds;
    }
}
