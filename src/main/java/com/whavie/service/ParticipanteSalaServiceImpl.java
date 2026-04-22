package com.whavie.service;

import com.whavie.dto.ParticipanteSalaDTO;
import com.whavie.model.ParticipanteSala;
import com.whavie.model.Sala;
import com.whavie.model.Usuario;
import com.whavie.repository.ParticipanteSalaRepository;
import com.whavie.repository.SalaRepository;
import com.whavie.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ParticipanteSalaServiceImpl implements ParticipanteSalaService {
    private final ParticipanteSalaRepository participanteSalaRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;

    public ParticipanteSalaServiceImpl(ParticipanteSalaRepository participanteSalaRepository,
                                       SalaRepository salaRepository,
                                       UsuarioRepository usuarioRepository) {
        this.participanteSalaRepository = participanteSalaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipanteSalaDTO> listarParticipantes(String codigoSala) {
        Sala sala = obtenerSalaPorCodigo(codigoSala);
        return participanteSalaRepository.findBySalaIdWithUsuario(sala.getId())
                .stream()
                .map(ParticipanteSalaDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void agregarParticipanteRegistrado(String codigoSala, Long usuarioId) {
        Sala sala = obtenerSalaPorCodigo(codigoSala);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        if (participanteSalaRepository.existsByUsuarioIdAndSalaId(usuarioId, sala.getId())) {
            throw new IllegalStateException("El usuario ya es participante de la sala");
        }

        validarSalaNoLlena(sala);

        ParticipanteSala participante = new ParticipanteSala(usuario, sala);
        participanteSalaRepository.save(participante);
    }

    @Override
    @Transactional
    public String agregarParticipanteInvitado(String codigoSala, String nombreInvitado) {
        Sala sala = obtenerSalaPorCodigo(codigoSala);
        validarSalaNoLlena(sala);
        String nombreNormalizado = nombreInvitado != null ? nombreInvitado.trim().toLowerCase() : "";
        if (nombreNormalizado.isBlank()) {
            throw new IllegalArgumentException("El nombre del invitado no puede estar vacío");
        }
        if (participanteSalaRepository.existeNombreEnSala(nombreNormalizado, sala.getId())) {
            throw new IllegalStateException("Ya existe un participante con ese nombre en la sala");
        }
        String tokenInvitado = UUID.randomUUID().toString();
        ParticipanteSala participante = new ParticipanteSala(
                sala,
                nombreNormalizado,
                tokenInvitado
        );
        participanteSalaRepository.save(participante);
        return tokenInvitado;
    }

    @Override
    @Transactional
    public void eliminarParticipanteRegistrado(String codigoSala, Long usuarioId) {
        Sala sala = obtenerSalaPorCodigo(codigoSala);
        ParticipanteSala participante = participanteSalaRepository
                .findByUsuarioIdAndSalaId(usuarioId, sala.getId())
                .orElseThrow(() -> new IllegalArgumentException("El participante no existe en esta sala"));
        participanteSalaRepository.delete(participante);
    }

    @Override
    @Transactional
    public void eliminarParticipanteInvitado(String tokenInvitado) {
        ParticipanteSala participante = participanteSalaRepository
                .findByTokenInvitado(tokenInvitado)
                .orElseThrow(() -> new IllegalArgumentException("El invitado no existe"));
        participanteSalaRepository.delete(participante);
    }

    @Override
    @Transactional
    public void eliminarParticipantePorAbandono(String codigoSala, String nombre) {
        Sala sala = obtenerSalaPorCodigo(codigoSala);
        ParticipanteSala participante = participanteSalaRepository
                .findByNombreAndSalaId(nombre, sala.getId())
                .orElseThrow(() -> new IllegalArgumentException("El participante no existe en esta sala"));
        participanteSalaRepository.delete(participante);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarParticipantes(String codigoSala) {
        Sala sala = obtenerSalaPorCodigo(codigoSala);
        return participanteSalaRepository.countBySalaId(sala.getId());
    }

    @Override
    public Long buscarParticipanteInvitado(String tokenInvitado) {
        return participanteSalaRepository.findByTokenInvitado(tokenInvitado)
                .orElseThrow(() -> new IllegalArgumentException("El invitado no existe"))
                .getId();
    }

    @Override
    public Long buscarParticipanteRegistrado(String codigoSala, Long usuarioId) {
        return participanteSalaRepository.findByUsuarioIdAndSalaCodigo(usuarioId, codigoSala)
                .orElseThrow(() -> new IllegalArgumentException("El participante registrado no existe"))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeTokenValido(String tokenInvitado) {
        if (tokenInvitado == null || tokenInvitado.isBlank()) {
            return false;
        }
        return participanteSalaRepository.findByTokenInvitado(tokenInvitado).isPresent();
    }

    private Sala obtenerSalaPorCodigo(String codigoSala) {
        if (codigoSala == null || codigoSala.isBlank()) {
            throw new IllegalArgumentException("El código de sala no puede ser nulo o vacío");
        }
        return salaRepository.findByCodigo(codigoSala)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
    }

    private void validarSalaNoLlena(Sala sala) {
        long actual = participanteSalaRepository.countBySalaId(sala.getId());
        if (actual >= sala.getMaxParticipantes()) {
            throw new IllegalStateException("La sala está llena");
        }
    }
}
