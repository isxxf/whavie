package com.whavie.controller;

import com.whavie.dto.Pelicula;
import com.whavie.dto.PreferenciaUsuarioDTO;
import com.whavie.model.FiltroSala;
import com.whavie.service.PreferenciaUsuarioService;
import com.whavie.service.TMDbService;
import com.whavie.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preferencias")
public class PreferenciaUsuarioController {
    private final PreferenciaUsuarioService preferenciaUsuarioService;
    private final UsuarioService usuarioService;
    private final TMDbService tmDbService;

    public PreferenciaUsuarioController(PreferenciaUsuarioService preferenciaUsuarioService,
                                        UsuarioService usuarioService, TMDbService tmDbService) {
        this.preferenciaUsuarioService = preferenciaUsuarioService;
        this.usuarioService = usuarioService;
        this.tmDbService = tmDbService;
    }

    // Obtenemos las preferencias del usuario
    @GetMapping
    public ResponseEntity<PreferenciaUsuarioDTO> obtenerPreferenciasUsuario(Principal principal) {
        Long usuarioId = usuarioService.obtenerUsuarioPorUsername(principal.getName()).getId();
        return ResponseEntity.ok(preferenciaUsuarioService.obtenerPreferenciaUsuarioDTO(usuarioId));
    }

    // Guardamos las preferencias del usuario
    @PostMapping
    public ResponseEntity<PreferenciaUsuarioDTO> guardarPreferenciasUsuario(Principal principal,
                                                                            @Valid @RequestBody PreferenciaUsuarioDTO dto) {
        Long usuarioId = usuarioService.obtenerUsuarioPorUsername(principal.getName()).getId();
        return ResponseEntity.ok(preferenciaUsuarioService.guardarPreferenciaUsuario(usuarioId, dto));
    }

    // Actualizamos las preferencias del usuario
    @PutMapping
    public ResponseEntity<PreferenciaUsuarioDTO> actualizarPreferenciasUsuario(Principal principal,
                                                                               @Valid @RequestBody PreferenciaUsuarioDTO dto) {
        Long usuarioId = usuarioService.obtenerUsuarioPorUsername(principal.getName()).getId();
        if (!preferenciaUsuarioService.existePreferenciaUsuario(usuarioId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(preferenciaUsuarioService.guardarPreferenciaUsuario(usuarioId, dto));
    }

    // Buscamos las peliculas de acuerdo a los filtros / preferencias seleccionadas por el usuario
    @PostMapping("/peliculas")
    public ResponseEntity<List<Pelicula>> buscarPeliculas(@RequestBody PreferenciaUsuarioDTO dto) {
        FiltroSala filtro = new FiltroSala();

        if (dto.getGenerosFavoritos() != null)
            filtro.setGeneros(dto.getGenerosFavoritos());
        if (dto.getIdiomasPreferidos() != null)
            filtro.setIdiomas(dto.getIdiomasPreferidos());
        if (dto.getPlataformasIds() != null)
            filtro.setPlataformasIds(dto.getPlataformasIds());
        if (dto.getClasificacionEdad() != null)
            filtro.setCertificacion(dto.getClasificacionEdad());
        if (dto.getActoresFavoritos() != null)
            filtro.setActoresIds(dto.getActoresFavoritos());
        if (dto.getDirectoresFavoritos() != null)
            filtro.setDirectoresIds(dto.getDirectoresFavoritos());
        if (dto.getEpocasIds() != null)
            filtro.setEpocasIds(dto.getEpocasIds());
        if (dto.getFechaGte() != null)
            filtro.setFechaGte(dto.getFechaGte());
        if (dto.getFechaLte() != null)
            filtro.setFechaLte(dto.getFechaLte());

        if (dto.getEpocasIds() != null && !dto.getEpocasIds().isEmpty()) {
            return ResponseEntity.ok(tmDbService.obtenerListaPeliculasFiltroMultiplesEpocas(filtro, dto.getEpocasIds(), 20));
        }
        return ResponseEntity.ok(tmDbService.obtenerListaPeliculasFiltro(filtro));
    }

    // Buscamos las peliculas de acuerdo a los actores o directores que eligió el usuario
    @GetMapping("/buscar-persona")
    public ResponseEntity<List<Map<String, Object>>> buscarPersona(
            @RequestParam String query,
            @RequestParam(defaultValue = "") String tipo) {
        return ResponseEntity.ok(tmDbService.buscarPersonas(query, tipo));
    }
}