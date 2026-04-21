package com.whavie.controller;

import com.whavie.dto.*;
import com.whavie.model.EstadoSala;
import com.whavie.model.Sala;
import com.whavie.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class HomeController {
    private final SalaService salaService;
    private final ParticipanteSalaService participanteSalaService;
    private final TMDbService tmDbService;
    private final UsuarioService usuarioService;
    private final PreferenciaUsuarioService preferenciaUsuarioService;


    public HomeController(SalaService salaService, ParticipanteSalaService participanteSalaService,
                          TMDbService tmDbService, UsuarioService usuarioService, PreferenciaUsuarioService preferenciaUsuarioService) {
        this.salaService = salaService;
        this.participanteSalaService = participanteSalaService;
        this.tmDbService = tmDbService;
        this.usuarioService = usuarioService;
        this.preferenciaUsuarioService = preferenciaUsuarioService;
    }

    /**
     * RUTA PRINCIPAL
     * Si el usuario está logueado (Principal no es null), buscamos sus datos y los mandamos a la vista
     * para poder mostrar su nombre o avatar en el inicio.
     */
    @GetMapping("/")
    public String inicio(Model model, Principal principal) {
        if (principal != null) {
            try {
                UsuarioDTO usuario = usuarioService.obtenerUsuarioPorUsername(principal.getName());
                model.addAttribute("usuario", usuario);
            } catch (Exception e) {
                model.addAttribute("usuario", null);
            }
        }
        return "inicio";
    }

    /**
     * RUTA DE LOGIN
     * Muestra la pantalla de inicio de sesión.
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pagina", "login");
        return "login";
    }

    /**
     * RUTA DE REGISTRO
     * Muestra la pantalla para registrarse.
     */
    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("pagina", "register");
        return "register";
    }

    /**
     * RUTA DE RECUPERACION DE CONTRASENA
     * Muestra la pantalla para establecer una nueva contraseña.
     */
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    /**
     * RUTA PARA CREAR SALA
     * Muestra el formulario donde el anfitrión configura los filtros de la sala.
     */
    @GetMapping("/crear-sala")
    public String crearSala() {
        return "crear-sala";
    }

    /**
     * RUTA PARA UNIRSE A UNA SALA
     * Muestra la pantalla donde un usuario pone el código de la sala para entrar.
     */
    @GetMapping("/unirse")
    public String unirse() {
        return "unirse-sala";
    }

    /**
     * RUTA DE LA SALA
     * Muestra la pantalla principal de la sala,
     * donde se ven los participantes, el anfitrión y las opciones para configurar la votación.
     */
    @GetMapping("/sala/{codigo}")
    public String sala(@PathVariable String codigo, Model model, Principal principal, HttpSession session) {
        try {
            // 1. Traemos la info de la sala
            SalaDTO sala = SalaDTO.fromEntity(salaService.obtenerSala(codigo));
            boolean esAnfitrion = false;
            String nombreActual;
            String tokenInvitado = null;
            // 2. Verificamos quién está intentando entrar:
            if (principal != null) {
                // Es un usuario registrado
                UsuarioDTO usuarioDTO = usuarioService.obtenerUsuarioPorUsername(principal.getName());
                nombreActual = usuarioDTO.getUsername();
                session.setAttribute("usuarioId", usuarioDTO.getId());
                // Comprobamos si este usuario fue el que creó esta sala
                if (sala.getCreador() != null) {
                    esAnfitrion = usuarioDTO.getUsername().equals(sala.getCreador().getUsername());
                }
                if (!esAnfitrion) {
                    try {
                        participanteSalaService.agregarParticipanteRegistrado(codigo, usuarioDTO.getId());
                    } catch (IllegalStateException e) {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("ya es participante")) {
                        } else {
                            return "redirect:/unirse?codigo=" + codigo + "&error=sala_llena";
                        }
                    }
                }
            } else {
                // Es un usuario invitado
                nombreActual = (String) session.getAttribute("nombreInvitado");
                tokenInvitado = (String) session.getAttribute("tokenInvitado");

                // Si no tiene nombre ni token en sesión, lo mandamos a la pantalla de unirse
                if (nombreActual == null || tokenInvitado == null) {
                    return "redirect:/unirse?codigo=" + codigo;
                }
                // Verificamos que realmente exista en la base de datos
                boolean estaEnBD = participanteSalaService.listarParticipantes(codigo)
                        .stream()
                        .anyMatch(p -> p.getDisplayName().equals(nombreActual));
                if (!estaEnBD) {
                    session.invalidate();
                    return "redirect:/unirse?codigo=" + codigo;
                }
            }
            // 3. Separamos al creador de los invitados para mostrarlos
            String nombreCreador = (sala.getCreador() != null) ? sala.getCreador().getUsername() : "";
            var listaParticipantes = participanteSalaService.listarParticipantes(codigo)
                    .stream()
                    .filter(p -> !p.getDisplayName().equals(nombreCreador))
                    .toList();
            // 4. Asignamos los avatares
            String finalNombreActual = nombreActual;
            String avatarActual;

            if (esAnfitrion && sala.getCreador() != null) {
                avatarActual = sala.getCreador().getAvatar();
            } else {
                // Si es invitado, buscamos su avatar. Si no tiene, le generamos uno aleatorio con DiceBear
                avatarActual = listaParticipantes.stream()
                        .filter(p -> p.getDisplayName().equals(finalNombreActual))
                        .map(ParticipanteSalaDTO::getAvatarUrl)
                        .findFirst()
                        .orElse("https://api.dicebear.com/7.x/bottts/svg?seed=" + finalNombreActual);
            }
            // 5. Metemos todos los datos en el model para que Thymeleaf los pueda usar en la vista
            model.addAttribute("sala", sala);
            model.addAttribute("creador", sala.getCreador());
            model.addAttribute("participantes", listaParticipantes);
            model.addAttribute("esAnfitrion", esAnfitrion);
            model.addAttribute("nombreActual", nombreActual);
            model.addAttribute("avatarActual", avatarActual);
            // Cargamos los filtros para las peliculas
            model.addAttribute("generos", tmDbService.obtenerGeneros());
            model.addAttribute("plataformas", tmDbService.obtenerPlataformasPrincipales());
            model.addAttribute("idiomas", tmDbService.obtenerIdiomasPrincipales());
            model.addAttribute("epocas", tmDbService.obtenerPeliculasEpocas());
            model.addAttribute("clasificacionesEdad", tmDbService.obtenerClasificacionEdad());

            return "sala";
        } catch (Exception e) {
            return "redirect:/?error=sala_inexistente";
        }
    }

    /**
     * RUTA DE VOTACIÓN
     * Aca es donde los usuarios ven las películas para votar
     */
    @GetMapping("/votacion")
    public String votacion(@RequestParam String codigo, Model model, Principal principal, HttpSession session) {
        try {
            if (principal == null) {
                String nombreInvitado = (String) session.getAttribute("nombreInvitado");
                if (nombreInvitado == null) {
                    return "redirect:/?error=requiere_nombre";
                }
            }
            // Verificamos que el anfitrión ya haya apretado "Empezar" para mostrar las películas a votar
            Sala sala = salaService.obtenerSala(codigo);
            if (sala.getEstado() != EstadoSala.EN_VOTACION) {
                return "redirect:/?error=sala_finalizada";
            }
            // Traemos las películas reales de la API usando los ID guardados según los filtros
            List<Long> idsParaVotar = sala.getPeliculasParaVotarIds();
            List<Pelicula> listaPeliculas = tmDbService.obtenerPeliculasPorIds(idsParaVotar);
            // Identificamos quién está votando para guardar sus likes correctamente
            String nombreActual;
            Long participanteId;
            if (principal != null) {
                UsuarioDTO usuarioDTO = usuarioService.obtenerUsuarioPorUsername(principal.getName());
                nombreActual = usuarioDTO.getUsername();
                participanteId = participanteSalaService.buscarParticipanteRegistrado(codigo, usuarioDTO.getId());
            } else {
                nombreActual = (String) session.getAttribute("nombreInvitado");
                String tokenInvitado = (String) session.getAttribute("tokenInvitado");
                participanteId = participanteSalaService.buscarParticipanteInvitado(tokenInvitado);
            }

            session.setAttribute("participanteId", participanteId);
            // Mandamos las peliculas a la vista
            model.addAttribute("peliculas", listaPeliculas);
            model.addAttribute("codigo", codigo);
            model.addAttribute("nombreActual", nombreActual);
            return "votacion";
        } catch (Exception e) {
            return "redirect:/?error=sala_inexistente";
        }
    }

    /**
     * RESULTADO PELICULA MODO INDIVIDUAL
     * Pantalla final si el usuario estaba usando la app solo.
     */
    @GetMapping("/resultado/individual/{tmdbId}")
    public String resultadoIndividual(@PathVariable Long tmdbId,
                                      Model model,
                                      Principal principal) {
        // Solo usuarios registrados pueden usar el modo individual
        if (principal == null) {
            return "redirect:/login";
        }
        // Traemos los detalles de la pelicula ganadora para mostrar sus detalles
        Pelicula pelicula = tmDbService.obtenerPeliculaPorTmdbId(tmdbId);
        model.addAttribute("pelicula", pelicula);
        model.addAttribute("tipoResultado", "individual");
        return "resultado";
    }

    /**
     * RESULTADO DE SALA GRUPAL
     * Pantalla final que ven todos cuando se elige la pelicula
     */
    @GetMapping("/resultado/sala/{codigo}/{tmdbId}/{tipoResultado}")
    public String resultadoGrupal(@PathVariable String codigo,
                                  @PathVariable Long tmdbId,
                                  @PathVariable String tipoResultado,
                                  Model model) {
        Sala sala = salaService.obtenerSala(codigo);
        if (sala.getEstado() != EstadoSala.FINALIZADA) {
            return "redirect:/sala/" + codigo;
        }
        if (!sala.getPeliculasParaVotarIds().contains(tmdbId)) {
            return "redirect:/?error=pelicula_invalida";
        }

        Pelicula pelicula = tmDbService.obtenerPeliculaPorTmdbId(tmdbId);
        model.addAttribute("pelicula", pelicula);
        model.addAttribute("codigo", codigo);
        model.addAttribute("esMatch", "match".equals(tipoResultado));
        model.addAttribute("tipoResultado", tipoResultado);

        return "resultado";
    }

    /**
     * MODO INDIVIDUAL
     * Pantalla donde el usuario configura sus preferencias y ve una película recomendada solo para él.
     */
    @GetMapping("/modo-individual")
    public String modoIndividual(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            // Traemos al usuario y sus filtros pre-guardados (si tiene)
            Long usuarioId = usuarioService.obtenerUsuarioPorUsername(principal.getName()).getId();
            PreferenciaUsuarioDTO preferencias = preferenciaUsuarioService.obtenerPreferenciaUsuarioDTO(usuarioId);
            // Cargamos todos los datos necesarios para que pueda ajustar los filtros
            model.addAttribute("preferencias", preferencias);
            model.addAttribute("generos", tmDbService.obtenerGeneros());
            model.addAttribute("plataformas", tmDbService.obtenerPlataformasPrincipales());
            model.addAttribute("idiomas", tmDbService.obtenerIdiomasPrincipales());
            model.addAttribute("clasificacionesEdad", tmDbService.obtenerClasificacionEdad());
            model.addAttribute("epocas", tmDbService.obtenerPeliculasEpocas());

            return "modo-individual";
        } catch (Exception e) {
            return "redirect:/?error=error_modo_individual";
        }
    }
}