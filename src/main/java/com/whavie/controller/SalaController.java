package com.whavie.controller;

import com.whavie.dto.*;
import com.whavie.exception.BadRequestException;
import com.whavie.model.Sala;
import com.whavie.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/sala")
public class SalaController {
    private final SalaService salaService;
    private final ParticipanteSalaService participanteSalaService;
    private final FiltroSalaService filtroSalaService;
    private final VotoPeliculaService votoPeliculaService;
    private final TMDbService tmDbService;
    private final UsuarioService usuarioService;

    public SalaController(SalaService salaService, ParticipanteSalaService participanteSalaService,
                          FiltroSalaService filtroSalaService, VotoPeliculaService votoPeliculaService,
                          TMDbService tmDbService, UsuarioService usuarioService) {
        this.salaService = salaService;
        this.participanteSalaService = participanteSalaService;
        this.filtroSalaService = filtroSalaService;
        this.votoPeliculaService = votoPeliculaService;
        this.tmDbService = tmDbService;
        this.usuarioService = usuarioService;
    }

    // Obtenemos la sala
    @GetMapping("/obtener-sala/{codigo}")
    public ResponseEntity<SalaDTO> obtenerSala(@PathVariable String codigo) {
        Sala sala = salaService.obtenerSala(codigo);
        Pelicula pelicula = null;
        if (sala.getPeliculaActualTmdbId() != null) {
            pelicula = tmDbService.obtenerPeliculaPorTmdbId(sala.getPeliculaActualTmdbId());
        }
        return ResponseEntity.ok(SalaDTO.fromEntity(sala, pelicula));
    }

    // Obtenemos los participantes de la sala
    @GetMapping("/{codigo}/participantes")
    public ResponseEntity<?> obtenerParticipantes(@PathVariable String codigo) {
        return ResponseEntity.ok(participanteSalaService.listarParticipantes(codigo));
    }

    // Creamos la sala, exclusivo solo para usuarios registrados
    @PostMapping
    public ResponseEntity<SalaDTO> crearSala(Principal principal, @RequestParam(defaultValue = "2") int maxParticipantes) {
        if (principal == null) {
            throw new BadRequestException("Se requiere autenticación para crear una sala");
        }
        Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
        Sala sala = salaService.crearSala(usuarioId, maxParticipantes);
        return ResponseEntity.ok(SalaDTO.fromEntity(sala));
    }

    // Endpoint para poder unirse a una sala, tanto como usuario registrado o invitado
    @PostMapping("/{codigo}")
    public ResponseEntity<?> unirseSala(@PathVariable String codigo,
                                        @RequestParam(required = false) String nombreInvitado,
                                        Principal principal, HttpSession session) {
        if (principal != null) {
            // El usuario ya tiene cuenta. Buscamos su ID y lo anotamos.
            Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
            salaService.unirseSalaRegistrado(codigo, usuarioId);
            return ResponseEntity.ok("OK");
        } else {
            // Es un invitado. Validamos que se haya puesto un nombre.
            if (nombreInvitado == null || nombreInvitado.isBlank()) {
                throw new BadRequestException("Se requiere un nombre para poder unirse a la sala");
            }
            // Limpiamos el nombre de espacios y lo normalizamos a minúsculas para evitar
            // problemas de duplicados por mayúsculas o espacios.
            String nombreNormalizado = nombreInvitado.trim().toLowerCase();
            // Creamos un token para este invitado y lo guardamos en su sesión
            String tokenInvitado = salaService.unirseSalaInvitado(codigo, nombreNormalizado);
            session.setAttribute("nombreInvitado", nombreNormalizado);
            session.setAttribute("tokenInvitado", tokenInvitado);
            return ResponseEntity.ok(tokenInvitado);
        }
    }

    // Endpoint para salir de la sala
    @PostMapping("/{codigo}/salir")
    public ResponseEntity<?> salirSala(@PathVariable String codigo,
                                       @RequestParam(required = false) String tokenInvitado,
                                       Principal principal) {
        if (principal != null) {
            // Si tiene cuenta, lo sacamos usando su ID
            Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
            salaService.salirSalaRegistrado(codigo, usuarioId);
        } else {
            // Si es invitado, verificamos su token y lo sacamos
            if (tokenInvitado == null || tokenInvitado.isBlank()) {
                throw new BadRequestException("Token de invitado requerido para salir de la sala");
            }
            salaService.salirSalaInvitado(tokenInvitado);
        }
        return ResponseEntity.ok().build();
    }

    // Eliminar sala, solo el anfitrión puede hacer esto
    @DeleteMapping("/{codigo}")
    public ResponseEntity<?> eliminarSala(@PathVariable String codigo, Principal principal) {
        Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
        salaService.eliminarSala(codigo, usuarioId);
        return ResponseEntity.ok().build();
    }

    // Guardamos los filtros seleccionados en la sala para buscar peliculas
    @PutMapping("/{codigo}/filtros")
    public ResponseEntity<?> aplicarFiltros(@PathVariable String codigo,
                                            @RequestBody FiltroSalaDTO filtroSala, Principal principal) {
        Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
        filtroSalaService.guardarOActualizarFiltro(codigo, filtroSala, usuarioId);
        return ResponseEntity.ok().body("Filtros aplicados correctamente a la sala " + codigo);
    }

    // Cambiamos el estado de la sala a "EN_VOTACION" y prepara las peliculas de acuerdo a los filtros seleccionados
    @PostMapping("/{codigo}/iniciar")
    public ResponseEntity<?> iniciarVotacion(@PathVariable String codigo, Principal principal) {
        Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
        Sala sala = salaService.obtenerSala(codigo);
        if (!sala.getCreador().getId().equals(usuarioId)) {
            throw new BadRequestException("Solo el creador puede iniciar la votación");
        }
        SalaDTO salaIniciada = salaService.iniciarVotacion(codigo, usuarioId);
        return ResponseEntity.ok(salaIniciada);
    }

    // Votacion de las peliculas
    @PostMapping("/{codigo}/votar")
    public ResponseEntity<?> votarPelicula(@PathVariable String codigo, @RequestParam Boolean voto,
                                           @RequestParam(required = false) String tokenInvitado,
                                           Principal principal) {
        if (voto == null) {
            throw new BadRequestException("El voto es requerido");
        }
        ResultadoRondaDTO resultado;
        // Identificamos quién votó y registramos su voto
        if (principal != null) {
            Long usuarioId = obtenerUsuarioIdDesdePrincipal(principal);
            Long participanteId = participanteSalaService.buscarParticipanteRegistrado(codigo, usuarioId);
            resultado = votoPeliculaService.votarPelicula(participanteId, voto);
        } else {
            if (tokenInvitado == null || tokenInvitado.isBlank()) {
                throw new BadRequestException("Token de invitado requerido para votar");
            }
            Long participanteId = participanteSalaService.buscarParticipanteInvitado(tokenInvitado);
            resultado = votoPeliculaService.votarPelicula(participanteId, voto);
        }
        return ResponseEntity.ok(resultado);
    }

    // Endpoint para obtener la película actual que se está votando en la sala, si es que ya se ha iniciado la votación
    @GetMapping("/{codigo}/pelicula-actual")
    public ResponseEntity<?> obtenerPeliculaActual(@PathVariable String codigo) {
        Sala sala = salaService.obtenerSala(codigo);
        if (sala.getPeliculaActualTmdbId() == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(sala.getPeliculaActualTmdbId());
    }

    private Long obtenerUsuarioIdDesdePrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            String email = oauthToken.getPrincipal().getAttribute("email");
            if (email == null) {
                throw new IllegalStateException("Email no disponible en OAuth2");
            }
            return usuarioService.obtenerUsuarioPorEmail(email).getId();
        }
        return usuarioService.obtenerUsuarioPorUsername(principal.getName()).getId();
    }
}
