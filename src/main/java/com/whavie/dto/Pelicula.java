package com.whavie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pelicula {
    private Long tmdbId;
    private String titulo;
    private String sinopsis;
    private String posterPath;
    private String fechaEstreno;
    private Double rating;
    private List<String> generos;
    private String idiomaOriginal;
    private List<String> cast;
    private List<String> directores;
    private List<String> plataformas;
    private List<String> plataformasLogos;
}

