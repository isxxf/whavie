package com.whavie.service;

import com.whavie.dto.RegisterRequest;
import com.whavie.dto.UsuarioDTO;

public interface AuthService {
    UsuarioDTO registerLocal(RegisterRequest registerRequest);
    void solicitarRecuperacionPassword(String email);
    void cambiarPasswordConToken(String token, String nuevaPassword);
}
