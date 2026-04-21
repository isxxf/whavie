package com.whavie.model;

import jakarta.persistence.*;

@Entity
@Table(name = "peliculas_vistas",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"usuario_id", "tmdb_id"})
        }
)
public class PeliculaVista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public PeliculaVista() {
    }

    public PeliculaVista(Usuario usuario, Long tmdbId) {
        this.usuario = usuario;
        this.tmdbId = tmdbId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

}

