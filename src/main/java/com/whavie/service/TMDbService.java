package com.whavie.service;

import com.whavie.dto.Pelicula;
import com.whavie.integration.TmdbGenre;
import com.whavie.model.FiltroSala;

import java.util.List;
import java.util.Map;

public interface TMDbService {
    Pelicula obtenerPeliculaAleatoriaFiltro(FiltroSala filtroSala);
    Pelicula obtenerPeliculaAleatoriaSinFiltro();
    Pelicula obtenerPeliculaPorTmdbId(Long tmdbId);
    List<TmdbGenre> obtenerGeneros();
    List<Map<String, String>> obtenerIdiomasPrincipales();
    List<Map<String, Object>> obtenerPlataformasPrincipales();
    List<Map<String, Object>> obtenerPeliculasEpocas();
    List<Map<String, Object>> obtenerClasificacionEdad();
    List<Pelicula> obtenerListaPeliculasFiltro(FiltroSala filtroSala);
    List<Pelicula> obtenerListaPeliculasFiltroMultiplesEpocas(FiltroSala filtroSala, List<String> epocasIds, int maxResultados);
    List<Pelicula> obtenerPeliculasPorIds(List<Long> ids);
    List<Map<String, Object>> buscarPersonas(String query,  String tipo);
}
