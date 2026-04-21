package com.whavie.service;

import com.whavie.dto.UsuarioDTO;

public interface UsuarioService {
    UsuarioDTO obtenerUsuarioPorUsername(String username);
    UsuarioDTO obtenerUsuarioPorEmail(String email);
}
