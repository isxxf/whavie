package com.whavie.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = true)
    private String password;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;
    @Column(unique = true, nullable = true)
    private String googleId;
    private String avatar;
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private PreferenciaUsuario preferencia;
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeliculaVista> peliculasVistas = new ArrayList<>();

    public Usuario() {
    }

    public Usuario(String username, String email, String password, AuthProvider authProvider, String avatar) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.authProvider = authProvider;
        this.avatar = avatar;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public PreferenciaUsuario getPreferencia() {
        return preferencia;
    }

    public void setPreferencia(PreferenciaUsuario preferencia) {
        this.preferencia = preferencia;
    }

    public List<PeliculaVista> getPeliculasVistas() {
        return peliculasVistas;
    }

    public void setPeliculasVistas(List<PeliculaVista> peliculasVistas) {
        this.peliculasVistas = peliculasVistas;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }
}
