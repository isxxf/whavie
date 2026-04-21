package com.whavie.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class PreferenciaUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true, nullable = false)
    private Usuario usuario;

    @ElementCollection
    @CollectionTable(name = "preferencia_generos", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "genero_id")
    private List<Integer> generosFavoritos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencia_actores", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "actor_id")
    private List<Integer> actoresFavoritos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencia_directores", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "director_id")
    private List<Integer> directoresFavoritos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencia_idiomas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "idioma")
    private List<String> idiomasPreferidos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencia_plataformas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "plataforma_id")
    private List<Integer> plataformasIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencia_epocas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "epoca_id")
    private List<String> epocasIds = new ArrayList<>();

    private Double puntuacionMinima;

    private String clasificacionEdad;

    private String fechaGte;
    private String fechaLte;

    public PreferenciaUsuario() {
    }

    public PreferenciaUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Integer> getGenerosFavoritos() {
        return generosFavoritos;
    }

    public void setGenerosFavoritos(List<Integer> generosFavoritos) {
        this.generosFavoritos = generosFavoritos;
    }

    public List<Integer> getActoresFavoritos() {
        return actoresFavoritos;
    }

    public void setActoresFavoritos(List<Integer> actoresFavoritos) {
        this.actoresFavoritos = actoresFavoritos;
    }

    public List<Integer> getDirectoresFavoritos() {
        return directoresFavoritos;
    }

    public void setDirectoresFavoritos(List<Integer> directoresFavoritos) {
        this.directoresFavoritos = directoresFavoritos;
    }

    public List<String> getIdiomasPreferidos() {
        return idiomasPreferidos;
    }

    public void setIdiomasPreferidos(List<String> idiomasPreferidos) {
        this.idiomasPreferidos = idiomasPreferidos;
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

    public Double getPuntuacionMinima() {
        return puntuacionMinima;
    }

    public void setPuntuacionMinima(Double puntuacionMinima) {
        this.puntuacionMinima = puntuacionMinima;
    }

    public String getClasificacionEdad() {
        return clasificacionEdad;
    }

    public void setClasificacionEdad(String clasificacionEdad) {
        this.clasificacionEdad = clasificacionEdad;
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
}
