package com.whavie.dto;

import com.whavie.model.Usuario;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String avatar;

    public static UsuarioDTO fromEntity(Usuario usuario) {
        String seed = usuario.getUsername();
        String defaultAvatar = "https://api.dicebear.com/7.x/bottts/svg?seed=" + seed;
        String avatarFinal = (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty())
                ? usuario.getAvatar()
                : defaultAvatar;

        return UsuarioDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .avatar(avatarFinal)
                .build();
    }
}
