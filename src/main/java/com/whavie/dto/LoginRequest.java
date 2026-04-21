package com.whavie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "El email o nombre de usuario es obligatorio")
    private String identificador;
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
