package com.whavie.controller;

import com.whavie.dto.RegisterRequest;
import com.whavie.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.registerLocal(registerRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> solicitarRecuperacion(@RequestParam String email) {
        authService.solicitarRecuperacionPassword(email);
        return ResponseEntity.ok("Si el correo existe, recibirás un enlace de recuperación.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> cambiarPassword(@RequestParam String token, @RequestParam String nuevaPassword) {
        authService.cambiarPasswordConToken(token,nuevaPassword);
        return ResponseEntity.ok("Contraseña actualizada correctamente.");
    }
}
