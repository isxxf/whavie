package com.whavie.dto;

import com.whavie.model.EstadoSala;
import com.whavie.model.Sala;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaDTO {
    private Long id;
    private String codigo;
    private UsuarioDTO creador;
    private EstadoSala estado;
    private Pelicula peliculaActual;
    private FiltroSalaDTO filtroSala;
    private int maxParticipantes;

    public static SalaDTO fromEntity(Sala sala) {
        return fromEntity(sala, null);
    }

    public static SalaDTO fromEntity(Sala sala, Pelicula pelicula) {
        SalaDTO dto = SalaDTO.builder()
                .id(sala.getId())
                .codigo(sala.getCodigo())
                .estado(sala.getEstado())
                .creador(UsuarioDTO.fromEntity(sala.getCreador()))
                .peliculaActual(pelicula)
                .filtroSala(sala.getFiltroSala()!=null ?
                        FiltroSalaDTO.fromEntity(sala.getFiltroSala()) : null)
                .maxParticipantes(sala.getMaxParticipantes())
                .build();

        if (pelicula != null) {
            dto.setPeliculaActual(pelicula);
        }

        return dto;
    }
}
