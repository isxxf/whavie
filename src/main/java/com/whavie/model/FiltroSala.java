package com.whavie.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "filtro_sala")
public class FiltroSala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filtro_sala_generos", joinColumns = @JoinColumn(name = "filtro_id"))
    @Column(name = "genero_tmdb_id")
    private List<Integer> generos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filtro_sala_idiomas", joinColumns = @JoinColumn(name = "filtro_id"))
    @Column(name = "idioma_iso")
    private List<String> idiomas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filtro_sala_actores", joinColumns = @JoinColumn(name = "filtro_id"))
    @Column(name = "actor_tmdb_id")
    private List<Integer> actoresIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filtro_sala_directores", joinColumns = @JoinColumn(name = "filtro_id"))
    @Column(name = "director_tmdb_id")
    private List<Integer> directoresIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filtro_sala_plataformas", joinColumns = @JoinColumn(name = "filtro_id"))
    @Column(name = "plataforma_tmdb_id")
    private List<Integer> plataformasIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "filtro_sala_epocas", joinColumns = @JoinColumn(name = "filtro_id"))
    @Column(name = "epoca_id")
    private List<String> epocasIds = new ArrayList<>();

    private String fechaGte;
    private String fechaLte;

    private String certificacion;
    private String edadMinima;
    private Boolean desnudos = false;

    public FiltroSala() {
    }

    public FiltroSala(Sala sala, List<Integer> generos, List<String> idiomas, List<Integer> actoresIds,
                      List<Integer> directoresIds, String fechaGte, String fechaLte,
                      String certificacion, String edadMinima, Boolean desnudos) {
        this.sala = sala;
        this.generos = generos;
        this.idiomas = idiomas;
        this.actoresIds = actoresIds;
        this.directoresIds = directoresIds;
        this.fechaGte = fechaGte;
        this.fechaLte = fechaLte;
        this.certificacion = certificacion;
        this.edadMinima = edadMinima;
        this.desnudos = desnudos;
    }

    public Long getId() {
        return id;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public List<Integer> getGeneros() {
        return generos;
    }

    public void setGeneros(List<Integer> generos) {
        this.generos = generos;
    }

    public List<String> getIdiomas() {
        return idiomas;
    }

    public void setIdiomas(List<String> idiomas) {
        this.idiomas = idiomas;
    }

    public List<Integer> getActoresIds() {
        return actoresIds;
    }

    public void setActoresIds(List<Integer> actoresIds) {
        this.actoresIds = actoresIds;
    }

    public List<Integer> getDirectoresIds() {
        return directoresIds;
    }

    public void setDirectoresIds(List<Integer> directoresIds) {
        this.directoresIds = directoresIds;
    }

    public String getEdadMinima() {
        return edadMinima;
    }

    public void setEdadMinima(String edadMinima) {
        this.edadMinima = edadMinima;
    }

    public Boolean isDesnudos() {
        return desnudos;
    }

    public void setDesnudos(Boolean desnudos) {
        this.desnudos = desnudos;
    }

    public String getFechaGte() {
        return fechaGte;
    }

    public void setFechaGte(String fechaGte) {
        this.fechaGte = fechaGte;
    }

    public String getFechaLte() {
        return fechaLte;
    }

    public void setFechaLte(String fechaLte) {
        this.fechaLte = fechaLte;
    }

    public String getCertificacion() {
        return certificacion;
    }

    public void setCertificacion(String certificacion) {
        this.certificacion = certificacion;
    }

    public Boolean getDesnudos() {
        return desnudos;
    }

    public List<Integer> getPlataformasIds() {
        return plataformasIds;
    }

    public void setPlataformasIds(List<Integer> plataformasIds) {
        this.plataformasIds = plataformasIds;
    }

    public List<String> getEpocasIds() {
        return epocasIds;
    }

    public void setEpocasIds(List<String> epocasIds) {
        this.epocasIds = epocasIds;
    }
}