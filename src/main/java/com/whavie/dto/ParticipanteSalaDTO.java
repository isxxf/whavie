package com.whavie.dto;

import com.whavie.model.ParticipanteSala;
import com.whavie.model.TipoParticipante;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteSalaDTO {
    private Long id;
    private TipoParticipante tipo;
    private String displayName;
    private String avatarUrl;

    public static ParticipanteSalaDTO fromEntity(ParticipanteSala participante) {
        String nombreAMostrar;
        String avatar;

        if (participante.getTipo() == TipoParticipante.REGISTRADO && participante.getUsuario() != null) {
            nombreAMostrar = participante.getUsuario().getUsername();
            avatar = "https://api.dicebear.com/7.x/bottts/svg?seed=" + nombreAMostrar;
        } else {
            nombreAMostrar = (participante.getNombreInvitado() != null) ? participante.getNombreInvitado() : "Invitado";
            // Genera un avatar único basado en el nombre del invitado
            avatar = "https://api.dicebear.com/7.x/bottts/svg?seed=" + nombreAMostrar;
        }

        return ParticipanteSalaDTO.builder()
                .id(participante.getId())
                .tipo(participante.getTipo())
                .displayName(nombreAMostrar)
                .avatarUrl(avatar)
                .build();
    }
}

