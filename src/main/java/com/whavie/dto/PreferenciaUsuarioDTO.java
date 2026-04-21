package com.whavie.dto;

import com.whavie.model.PreferenciaUsuario;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaUsuarioDTO {
    private List<Integer> generosFavoritos;
    private List<Integer> actoresFavoritos;
    private List<Integer> directoresFavoritos;
    @Size(max = 5, message = "El código de idioma es demasiado largo (ej: 'es' o 'en')")
    private List<String> idiomasPreferidos;
    private List<Integer> plataformasIds;
    private List<String> epocasIds;
    @Min(value = 0, message = "La puntuación mínima no puede ser menor a 0")
    @Max(value = 10, message = "La puntuación mínima no puede ser mayor a 10")
    private Double puntuacionMinima;
    @Size(max = 5, message = "La clasificación de edad es muy corta (ej: 'PG-13')")
    private String clasificacionEdad;
    private String fechaGte;
    private String fechaLte;

    public static PreferenciaUsuarioDTO fromEntity(PreferenciaUsuario preferencia) {
        return PreferenciaUsuarioDTO.builder()
                .generosFavoritos(preferencia.getGenerosFavoritos())
                .actoresFavoritos(preferencia.getActoresFavoritos())
                .directoresFavoritos(preferencia.getDirectoresFavoritos())
                .idiomasPreferidos(preferencia.getIdiomasPreferidos())
                .plataformasIds(preferencia.getPlataformasIds())
                .epocasIds(preferencia.getEpocasIds())
                .puntuacionMinima(preferencia.getPuntuacionMinima())
                .clasificacionEdad(preferencia.getClasificacionEdad())
                .fechaGte(preferencia.getFechaGte())
                .fechaLte(preferencia.getFechaLte())
                .build();
    }

    public void updateEntity(PreferenciaUsuario preferencia) {
        if (this.generosFavoritos != null) {
            preferencia.getGenerosFavoritos().clear();
            preferencia.getGenerosFavoritos().addAll(this.generosFavoritos);
        }
        if (this.actoresFavoritos != null) {
            preferencia.getActoresFavoritos().clear();
            preferencia.getActoresFavoritos().addAll(this.actoresFavoritos);
        }
        if (this.directoresFavoritos != null) {
            preferencia.getDirectoresFavoritos().clear();
            preferencia.getDirectoresFavoritos().addAll(this.directoresFavoritos);
        }
        if (this.idiomasPreferidos != null) {
            preferencia.getIdiomasPreferidos().clear();
            preferencia.getIdiomasPreferidos().addAll(this.idiomasPreferidos);
        }
        if (this.plataformasIds != null) {
            preferencia.getPlataformasIds().clear();
            preferencia.getPlataformasIds().addAll(this.plataformasIds);
        }
        if (this.epocasIds != null) {
            preferencia.getEpocasIds().clear();
            preferencia.getEpocasIds().addAll(this.epocasIds);
        }
        boolean usarFechas = this.epocasIds == null || this.epocasIds.isEmpty();
        preferencia.setPuntuacionMinima(this.puntuacionMinima);
        preferencia.setClasificacionEdad(this.clasificacionEdad);
        if (usarFechas) {
            preferencia.setFechaGte(this.fechaGte);
            preferencia.setFechaLte(this.fechaLte);
        } else {
            preferencia.setFechaGte(null);
            preferencia.setFechaLte(null);
        }
    }
}
