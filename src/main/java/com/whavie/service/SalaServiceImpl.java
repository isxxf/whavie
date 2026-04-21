package com.whavie.service;

import com.whavie.dto.ResultadoRondaDTO;
import com.whavie.dto.SalaDTO;
import com.whavie.model.EstadoSala;
import com.whavie.model.FiltroSala;
import com.whavie.model.Sala;
import com.whavie.model.Usuario;
import com.whavie.dto.Pelicula;
import com.whavie.repository.SalaRepository;
import com.whavie.repository.UsuarioRepository;
import com.whavie.repository.VotoPeliculaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class SalaServiceImpl implements SalaService {
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VotoPeliculaRepository votoPeliculaRepository;

    private final ParticipanteSalaService participanteSalaService;
    private final TMDbService tmDbService;

    private final Random random = new Random();

    public SalaServiceImpl(SalaRepository salaRepository, ParticipanteSalaService participanteSalaService,
                           UsuarioRepository usuarioRepository, VotoPeliculaRepository votoPeliculaRepository,
                           TMDbService tmDbService) {
        this.salaRepository = salaRepository;
        this.participanteSalaService = participanteSalaService;
        this.usuarioRepository = usuarioRepository;
        this.votoPeliculaRepository = votoPeliculaRepository;
        this.tmDbService = tmDbService;
    }

    // Genera un string de 7 caracteres alfanuméricos.
    // Hace un bucle do-while para verificar que el código generado no exista ya en la BD.
    @Override
    public String generarCodigoSala() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String codigo;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
            }
            codigo = sb.toString();
        } while (salaRepository.existsByCodigo(codigo));
        return codigo;
    }

    @Override
    @Transactional
    public Sala crearSala(Long creadorId, int maxParticipantes) {
        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));
        String codigoSala = generarCodigoSala();
        Sala sala = new Sala();
        sala.setCodigo(codigoSala);
        sala.setCreador(creador);
        sala.setEstado(EstadoSala.ESPERANDO);
        sala.setMaxParticipantes(maxParticipantes);

        Sala salaGuardada = salaRepository.save(sala);
        // Automáticamente, el creador se añade como el primer participante de la sala
        participanteSalaService.agregarParticipanteRegistrado(codigoSala, creador.getId());

        return salaGuardada;
    }

    @Override
    public void unirseSalaRegistrado(String codigo, Long usuarioId) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        validarIngresoSala(sala);
        participanteSalaService.agregarParticipanteRegistrado(codigo, usuarioId);
    }

    @Override
    public String unirseSalaInvitado(String codigo, String nombreInvitado) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        validarIngresoSala(sala);
        return participanteSalaService.agregarParticipanteInvitado(codigo, nombreInvitado);
    }

    @Override
    @Transactional
    public void salirSalaRegistrado(String codigo, Long usuarioId) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        // Si el creador sale, se elimina toda la sala. Si es un usuario normal, solo se remueve él.
        if (sala.getCreador().getId().equals(usuarioId)) {
            eliminarSala(codigo, usuarioId);
        } else {
            participanteSalaService.eliminarParticipanteRegistrado(codigo, usuarioId);
        }
    }

    @Override
    public void salirSalaInvitado(String tokenInvitado) {
        participanteSalaService.eliminarParticipanteInvitado(tokenInvitado);
    }

    @Transactional
    @Override
    public void eliminarSala(String codigo, Long usuarioId) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        if (!sala.getCreador().getId().equals(usuarioId)) {
            throw new IllegalStateException("Solo el creador puede eliminar la sala");
        }
        salaRepository.delete(sala);
    }

    @Transactional
    @Override
    public void eliminarSalaAbandono(String codigo) {
        salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        salaRepository.deleteByCodigo(codigo);
    }

    @Override
    public Sala obtenerSala(String codigo) {
        return salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
    }

    @Override
    @Transactional
    public SalaDTO iniciarVotacion(String codigo, Long usuarioId) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        if (sala.getEstado() != EstadoSala.ESPERANDO) {
            throw new IllegalStateException("La votación ya ha sido iniciada o la sala ya cerró.");
        }
        if (!sala.getCreador().getId().equals(usuarioId)) {
            throw new IllegalStateException("Solo el creador de la sala puede iniciar la votación");
        }

        long participantes = participanteSalaService.contarParticipantes(codigo);
        if (participantes < 2) {
            throw new IllegalStateException("Se necesitan al menos 2 participantes para iniciar la votación");
        }
        // Cambiamos el estado para bloquear el ingreso de nuevos participantes
        sala.setEstado(EstadoSala.EN_VOTACION);
        sala.setPeliculasMostradas(1);
        salaRepository.saveAndFlush(sala);
        return SalaDTO.fromEntity(sala);
    }

    // Este es el motor de decisión después de cada voto.
    // Usamos SERIALIZABLE para evitar que dos votos simultáneos
    // lean el estado de la sala antes de que el otro se guarde, causando resultados inconsistentes.
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResultadoRondaDTO resolverVotacion(Sala sala) {
        long participantes = participanteSalaService.contarParticipantes(sala.getCodigo());
        long votosTotales = votoPeliculaRepository.countBySalaIdAndTmdbId(sala.getId(), sala.getPeliculaActualTmdbId());
        // Aún no han votado todos, no hacemos nada todavía
        if (votosTotales < participantes) {
            return new ResultadoRondaDTO("ESPERANDO", null);
        }
        // Ya votaron todos, revisamos si hubo match (todos likes)
        boolean votacion = matchPelicula(sala);
        if (votacion) {
            sala.setEstado(EstadoSala.FINALIZADA);
            salaRepository.save(sala);
            return new ResultadoRondaDTO("HAY_MATCH", sala.getPeliculaActualTmdbId());
        }
        // Lógica de "desempate": Si ya vieron más de 10 películas y nadie se pone de acuerdo,
        // aceptamos la película actual si al menos la mitad + 1 le dio "Me gusta".
        // Esto para evitar que se haga un "bucle infinito" sin que se elija una película.
        if (sala.getPeliculasMostradas() >= 11) {
            long likes = votoPeliculaRepository.countBySalaIdAndTmdbIdAndVotoTrue(sala.getId(), sala.getPeliculaActualTmdbId());
            if (likes > (participantes / 2)) {
                sala.setEstado(EstadoSala.FINALIZADA);
                salaRepository.save(sala);
                return new ResultadoRondaDTO("GANADORA_MAYORIA", sala.getPeliculaActualTmdbId());
            }
        }
        // Si no hay match y no aplica el desempate, pasamos a la siguiente película
        Long nuevaPeliId = siguientePelicula(sala);
        return new ResultadoRondaDTO("NUEVA_RONDA", nuevaPeliId);
    }

    @Override
    public boolean matchPelicula(Sala sala) {
        if (sala.getEstado() != EstadoSala.EN_VOTACION) {
            throw new IllegalStateException("La sala no está en estado de votación");
        }
        long participantes = participanteSalaService.contarParticipantes(sala.getCodigo());
        if (participantes < 2) {
            throw new IllegalStateException("No hay suficientes participantes para realizar la votación");
        }
        Long tmdbId = sala.getPeliculaActualTmdbId();
        if (tmdbId == null) {
            throw new IllegalStateException("No hay película en votación actualmente");
        }
        // Cuenta cuántos "Likes" tiene esta película en esta sala
        long likes = votoPeliculaRepository.countBySalaIdAndTmdbIdAndVotoTrue(sala.getId(), tmdbId);
        // Si los likes son iguales a la cantidad de personas, es Match.
        return likes == participantes;
    }

    @Override
    @Transactional
    public Long siguientePelicula(Sala sala) {
        List<Long> lista = sala.getPeliculasParaVotarIds();
        int siguiente = sala.getPeliculasMostradas();

        Long nuevaId;
        // Si todavía quedan películas en la lista inicial que no se han mostrado, las usamos en orden.
        // Esto para garantizar que se muestren todas las opciones que el anfitrión eligió al inicio.
        if (siguiente < lista.size()) {
            nuevaId = lista.get(siguiente);
        } else {
            // Si ya se acabó la lista, traemos una nueva pelicula random de TMDb aplicando
            // los filtros originales, asegurándonos de no repetir una que ya mostramos.
            Pelicula pelicula;
            int intentos = 0;
            do {
                pelicula = tmDbService.obtenerPeliculaAleatoriaFiltro(sala.getFiltroSala());
                intentos++;
            } while (sala.getHistorialPeliculasIds().contains(pelicula.getTmdbId()) && intentos < 15);
            nuevaId = pelicula.getTmdbId();
        }

        sala.getHistorialPeliculasIds().add(nuevaId);
        sala.setPeliculaActualTmdbId(nuevaId);
        sala.setPeliculasMostradas(sala.getPeliculasMostradas() + 1);
        salaRepository.save(sala);
        // Borramos los votos de la ronda anterior para no ocupar espacio innecesario en la BD,
        // ya que no los necesitamos para nada después de resolver la ronda.
        votoPeliculaRepository.deleteBySalaId(sala.getId());
        return nuevaId;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esAnfitrion(String codigoSala, String username) {
        return salaRepository.findByCodigo(codigoSala)
                .map(sala -> sala.getCreador().getUsername().equals(username))
                .orElse(false);
    }

    @Override
    @Transactional
    public void guardarPeliculasVotacion(String codigo, List<Long> tmdbIds, FiltroSala filtroEntidad) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        sala.getPeliculasParaVotarIds().clear();
        sala.getPeliculasParaVotarIds().addAll(tmdbIds);
        // Si tenemos películas en la lista, preparamos la sala para mostrar la primera de inmediato.
        if (!tmdbIds.isEmpty()) {
            sala.setPeliculaActualTmdbId(tmdbIds.get(0));
            sala.getHistorialPeliculasIds().clear();
            sala.getHistorialPeliculasIds().add(tmdbIds.get(0));
            sala.setPeliculasMostradas(1);
        }

        filtroEntidad.setSala(sala);
        sala.setFiltroSala(filtroEntidad);

        salaRepository.save(sala);
    }

    /**
     * @Scheduled es un "cron job" de Spring.
     * fixedRate = 3600000 significa que este método se ejecuta automáticamente
     * cada 1 hora. Busca salas que tengan más de 8 horas de creadas y las borra.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void eliminarSalasExpiradas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(8);
        List<Sala> salasExpiradas = salaRepository.findByFechaCreacionBefore(limite);
        if (!salasExpiradas.isEmpty()) {
            salaRepository.deleteAll(salasExpiradas);
        }
    }

    @Transactional
    public void guardarVotacionEnBD(String codigo, Long usuarioId,
                                       List<Long> peliculasIds, FiltroSala filtroEntidad){
        iniciarVotacion(codigo,usuarioId);
        guardarPeliculasVotacion(codigo,peliculasIds,filtroEntidad);
    }

    private void validarIngresoSala(Sala sala) {
        if (sala.getEstado() != EstadoSala.ESPERANDO) {
            throw new IllegalStateException("La sala no permite nuevos participantes");
        }
        if (sala.getParticipantes().size() >= sala.getMaxParticipantes()) {
            throw new IllegalStateException("La sala ha alcanzado el número máximo de participantes");
        }
    }
}
