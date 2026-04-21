package com.whavie.service;

import com.whavie.dto.PreferenciaUsuarioDTO;
import com.whavie.model.PreferenciaUsuario;
import com.whavie.model.Usuario;
import com.whavie.repository.PreferenciaUsuarioRepository;
import com.whavie.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class PreferenciaUsuarioServiceImpl implements PreferenciaUsuarioService {
    private final PreferenciaUsuarioRepository preferenciaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PreferenciaUsuarioServiceImpl(PreferenciaUsuarioRepository preferenciaUsuarioRepository, UsuarioRepository usuarioRepository) {
        this.preferenciaUsuarioRepository = preferenciaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean existePreferenciaUsuario(Long usuarioId) {
        return preferenciaUsuarioRepository.existsByUsuarioId(usuarioId);
    }

    @Override
    public PreferenciaUsuarioDTO obtenerPreferenciaUsuarioDTO(Long usuarioId) {
        return preferenciaUsuarioRepository
                .findByUsuarioId(usuarioId)
                .map(PreferenciaUsuarioDTO::fromEntity)
                .orElse(PreferenciaUsuarioDTO.builder()
                        .generosFavoritos(new ArrayList<>())
                        .actoresFavoritos(new ArrayList<>())
                        .directoresFavoritos(new ArrayList<>())
                        .idiomasPreferidos(new ArrayList<>())
                        .plataformasIds(new ArrayList<>())
                        .epocasIds(new ArrayList<>())
                        .build());
    }

    @Override
    @Transactional
    public PreferenciaUsuarioDTO guardarPreferenciaUsuario(Long usuarioId, PreferenciaUsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        PreferenciaUsuario preferencia = preferenciaUsuarioRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    PreferenciaUsuario nuevaPreferencia = new PreferenciaUsuario();
                    nuevaPreferencia.setUsuario(usuario);
                    return nuevaPreferencia;
                });
        dto.updateEntity(preferencia);
        PreferenciaUsuario guardada = preferenciaUsuarioRepository.save(preferencia);
        return PreferenciaUsuarioDTO.fromEntity(guardada);
    }
}
