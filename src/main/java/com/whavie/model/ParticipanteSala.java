package com.whavie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "participantes_sala",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "sala_id"}),
        }
)
public class ParticipanteSala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoParticipante tipo;

    @Size(max = 20, message = "El nombre no puede tener más de 20 caracteres")
    @Pattern(regexp = "^[a-z0-9áéíóúñ _-]+$", message = "El nombre contiene caracteres no permitidos")
    @Column(length = 50)
    private String nombreInvitado;

    @Column(unique = true)
    private String tokenInvitado;

    public ParticipanteSala() {
    }

    public ParticipanteSala(Sala sala, String nombreInvitado, String tokenInvitado) {
        this.sala = sala;
        this.tipo = TipoParticipante.INVITADO;
        this.nombreInvitado = nombreInvitado != null ? nombreInvitado.trim().toLowerCase() : null;
        this.tokenInvitado = tokenInvitado;
    }

    public ParticipanteSala(Usuario usuario, Sala sala) {
        this.usuario = usuario;
        this.sala = sala;
        this.tipo = TipoParticipante.REGISTRADO;
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

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public TipoParticipante getTipo() {
        return tipo;
    }

    public void setTipo(TipoParticipante tipo) {
        this.tipo = tipo;
    }

    public String getNombreInvitado() {
        return nombreInvitado;
    }

    public void setNombreInvitado(String nombreInvitado) {
        this.nombreInvitado = nombreInvitado != null ? nombreInvitado.trim().toLowerCase() : null;
    }

    public String getTokenInvitado() {
        return tokenInvitado;
    }

    public void setTokenInvitado(String tokenInvitado) {
        this.tokenInvitado = tokenInvitado;
    }
}
