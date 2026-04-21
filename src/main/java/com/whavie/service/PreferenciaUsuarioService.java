package com.whavie.service;

import com.whavie.dto.PreferenciaUsuarioDTO;

public interface PreferenciaUsuarioService {
    boolean existePreferenciaUsuario(Long usuarioId);
    PreferenciaUsuarioDTO obtenerPreferenciaUsuarioDTO(Long usuarioId);
    PreferenciaUsuarioDTO guardarPreferenciaUsuario(Long usuarioId, PreferenciaUsuarioDTO preferenciaUsuario);
}
