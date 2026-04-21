package com.whavie.dto;

import com.whavie.model.FiltroSala;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiltroSalaDTO {
    @Builder.Default
    private List<Integer> generos = new ArrayList<>();
    @Builder.Default
    private List<String> idiomas = new ArrayList<>();
    @Builder.Default
    private List<Integer> actoresIds = new ArrayList<>();
    @Builder.Default
    private List<Integer> directoresIds = new ArrayList<>();
    @Builder.Default
    private List<String> epocasIds = new ArrayList<>();
    private String fechaGte;
    private String fechaLte;
    private String certificacion;
    private String edadMinima;
    @Builder.Default
    private List<Integer> plataformasIds = new ArrayList<>();
    @Builder.Default
    private Boolean desnudos = false;

    public void updateEntity(FiltroSala filtro) {
        if (this.generos != null) {
            filtro.getGeneros().clear();
            filtro.getGeneros().addAll(this.generos);
        }
        if (this.idiomas != null) {
            filtro.getIdiomas().clear();
            filtro.getIdiomas().addAll(this.idiomas);
        }
        if (this.actoresIds != null) {
            filtro.getActoresIds().clear();
            filtro.getActoresIds().addAll(this.actoresIds);
        }
        if (this.directoresIds != null) {
            filtro.getDirectoresIds().clear();
            filtro.getDirectoresIds().addAll(this.directoresIds);
        }
        if (this.epocasIds != null) {
            filtro.getEpocasIds().clear();
            filtro.getEpocasIds().addAll(this.epocasIds);
        }
        boolean usarFechas = this.epocasIds == null || this.epocasIds.isEmpty();
        if (usarFechas) {
            if (this.fechaGte != null) {
                filtro.setFechaGte(this.fechaGte);
            }
            if (this.fechaLte != null) {
                filtro.setFechaLte(this.fechaLte);
            }
        } else {
            filtro.setFechaGte(null);
            filtro.setFechaLte(null);
        }
        if (this.certificacion != null) {
            filtro.setCertificacion(this.certificacion);
        }
        if (this.edadMinima != null) {
            filtro.setEdadMinima(this.edadMinima);
        }
        if (this.plataformasIds != null) {
            filtro.getPlataformasIds().clear();
            filtro.getPlataformasIds().addAll(this.plataformasIds);
        }
        if (this.desnudos != null) {
            filtro.setDesnudos(this.desnudos);
        }
    }

    public static FiltroSalaDTO fromEntity(FiltroSala filtro) {
        if (filtro == null) return null;
        return FiltroSalaDTO.builder()
                .generos(filtro.getGeneros())
                .idiomas(filtro.getIdiomas())
                .actoresIds(filtro.getActoresIds())
                .directoresIds(filtro.getDirectoresIds())
                .epocasIds(filtro.getEpocasIds())
                .fechaGte(filtro.getFechaGte())
                .fechaLte(filtro.getFechaLte())
                .certificacion(filtro.getCertificacion())
                .edadMinima(filtro.getEdadMinima())
                .plataformasIds(filtro.getPlataformasIds())
                .desnudos(filtro.isDesnudos())
                .build();
    }
}
