package com.whavie.service;

import com.whavie.dto.UsuarioDTO;
import com.whavie.exception.BadRequestException;
import com.whavie.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UsuarioDTO obtenerUsuarioPorUsername(String identifier) {
        String normalized = identifier.trim().toLowerCase();
        return usuarioRepository.findByEmailOrUsername(normalized, normalized)
                .or(() -> usuarioRepository.findByGoogleId(normalized))
                .map(UsuarioDTO::fromEntity)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado en el sistema"));
    }

    @Override
    public UsuarioDTO obtenerUsuarioPorEmail(String email) {
        String normalized = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(normalized)
                .map(UsuarioDTO::fromEntity)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado en el sistema"));
    }
}
