package com.whavie.service;

import com.whavie.dto.Pelicula;
import com.whavie.integration.*;
import com.whavie.model.FiltroSala;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TMDbServiceImpl implements TMDbService {
    private final RestClient restClient;
    private final Random rnd = new Random();

    public TMDbServiceImpl(
            @Value("${tmdb.api.url}") String baseUrl,
            @Value("${tmdb.api.token}") String token) {
        // Configuramos TIMEOUTS para evitar que la app se quede colgada esperando a TMDb.
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(5))
                .build();
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        // Construimos el cliente base
        // Le precargamos la URL y el Token de autorización para no tener que
        // pasarlos en cada método individualmente.
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/json")
                .requestFactory(factory)
                .build();
    }

    // Este método actualmente no se utiliza
    // Planeo agregar algo más adelante para su función
    @Override
    public Pelicula obtenerPeliculaAleatoriaSinFiltro() {
        int paginaRandom = rnd.nextInt(500) + 1;
        TmdbDiscoverResponse discover = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("include_adult", "false")
                        .queryParam("with_runtime.gte", "60")
                        .queryParam("page", paginaRandom)
                        .build())
                .retrieve()
                .body(TmdbDiscoverResponse.class);
        if (discover == null || discover.results() == null) {
            log.error("TMDb API respondió con null o results null");
            throw new IllegalStateException("TMDb API no respondió correctamente");
        }
        if (discover.results().isEmpty()) {
            log.warn("No hay películas disponibles sin filtro");
            throw new IllegalArgumentException("No hay películas disponibles en TMDb");
        }
        Long randomMovieId = discover.results().get(rnd.nextInt(discover.results().size())).id();
        return obtenerDetallesPelicula(randomMovieId, Set.of());
    }

    @Override
    public Pelicula obtenerPeliculaAleatoriaFiltro(FiltroSala filtroSala) {
        if (filtroSala != null && filtroSala.getEpocasIds() != null && !filtroSala.getEpocasIds().isEmpty()) {
            String epocaId = elegirEpocaAleatoria(filtroSala.getEpocasIds());
            FiltroSala filtroEpoca = crearFiltroConEpoca(filtroSala, epocaId);
            return obtenerPeliculaAleatoriaFiltroRango(filtroEpoca);
        }
        return obtenerPeliculaAleatoriaFiltroRango(filtroSala);
    }

    private Pelicula obtenerPeliculaAleatoriaFiltroRango(FiltroSala filtroSala) {
        // PASO 1: Descubrimiento Inicial
        // Hacemos una consulta "en blanco" a la página 1 solo para averiguar
        // cuántas páginas totales hay que cumplan con estos filtros.
        TmdbDiscoverResponse discover = restClient.get()
                .uri(uriBuilder -> aplicarFiltros(uriBuilder.path("/discover/movie"), filtroSala).build())
                .retrieve()
                .body(TmdbDiscoverResponse.class);

        if (discover == null || discover.results() == null) {
            log.error("TMDb API respondió con null o results null para filtro: {}", filtroSala);
            throw new IllegalStateException("TMDb API no respondió correctamente");
        }

        if (discover.results().isEmpty()) {
            log.info("No hay películas disponibles con los filtros aplicados");
            throw new IllegalArgumentException("No hay películas disponibles con los filtros seleccionados");
        }

        // PASO 2: Elegimos una página al azar dentro del rango permitido por TMDb (máx 500)
        int totalPaginas = Math.min(discover.total_pages(), 500);
        int paginaRandom = rnd.nextInt(totalPaginas) + 1;
        // PASO 3: Traemos la página aleatoria con los mismos filtros para obtener una
        // lista de películas que cumplan con los criterios.
        TmdbDiscoverResponse busqueda = restClient.get()
                .uri(uriBuilder -> aplicarFiltros(uriBuilder.path("/discover/movie"), filtroSala)
                        .queryParam("page", paginaRandom)
                        .build())
                .retrieve()
                .body(TmdbDiscoverResponse.class);

        if (busqueda == null || busqueda.results() == null || busqueda.results().isEmpty()) {
            log.error("TMDb devolvió resultados vacíos en página aleatoria: {}", paginaRandom);
            throw new IllegalStateException("No se obtuvieron películas en la página aleatoria");
        }

        Long randomMovieId = busqueda.results().get(rnd.nextInt(busqueda.results().size())).id();

        // Control de plataformas: Si el usuario filtró por Netflix, le pasamos esa restricción
        // a 'obtenerDetallesPelicula' para que no nos muestre enlaces a Amazon o HBO.
        Set<Integer> permitidas = filtroSala != null && filtroSala.getPlataformasIds() != null
                ? new HashSet<>(filtroSala.getPlataformasIds())
                : Set.of();
        return obtenerDetallesPelicula(randomMovieId, permitidas);
    }

    @Override
    public List<Pelicula> obtenerListaPeliculasFiltro(FiltroSala filtroSala) {
        // Lógica similar al método anterior, pero devuelve una lista completa de 20 películas
        // de una página aleatoria, en lugar de una sola.
        TmdbDiscoverResponse initialResponse = restClient.get()
                .uri(uriBuilder -> aplicarFiltros(uriBuilder.path("/discover/movie"), filtroSala).build())
                .retrieve()
                .body(TmdbDiscoverResponse.class);

        if (initialResponse == null || initialResponse.results().isEmpty()) {
            return Collections.emptyList();
        }

        int maxPages = Math.min(initialResponse.total_pages(), 500);
        int randomPage = rnd.nextInt(maxPages) + 1;

        TmdbDiscoverResponse finalResponse = restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = aplicarFiltros(uriBuilder.path("/discover/movie"), filtroSala)
                            .queryParam("language", "es-ES")
                            .queryParam("page", randomPage)
                            .queryParam("sort_by", "vote_count.desc");
                    return builder.build();
                })
                .retrieve()
                .body(TmdbDiscoverResponse.class);

        Set<Integer> permitidas = filtroSala != null && filtroSala.getPlataformasIds() != null
                ? new HashSet<>(filtroSala.getPlataformasIds())
                : Set.of();

        List<Pelicula> resultado = finalResponse.results().stream()
                .map(result -> obtenerDetallesPelicula(result.id(), permitidas))
                .collect(Collectors.toList());

        // Desordenamos la lista para que no siempre aparezcan las mismas películas
        // en el mismo orden cuando se aplican los mismos filtros.
        Collections.shuffle(resultado);
        return resultado;
    }

    @Override
    public List<Pelicula> obtenerPeliculasPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
                .map(id -> obtenerDetallesPelicula(id, Set.of()))
                .toList();
    }

    @Override
    public Pelicula obtenerPeliculaPorTmdbId(Long tmdbId) {
        return obtenerDetallesPelicula(tmdbId, Set.of());
    }

    @Override
    public List<Map<String, Object>> buscarPersonas(String query, String tipo) {
        try {
            Map response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/person")
                            .queryParam("query", query)
                            .queryParam("language", "es-ES")
                            .build())
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            // Filtramos la respuesta masiva de TMDb y nos quedamos solo con lo necesario
            return results.stream()
                    // Nos aseguramos de que si buscaron un director, no devuelva un actor con ese nombre
                    .filter(p -> {
                        String dept = (String) p.getOrDefault("known_for_department", "");
                        if (tipo.equals("actor")) return "Acting".equals(dept);
                        if (tipo.equals("director")) return "Directing".equals(dept);
                        return true;
                    })
                    // Extraemos solo ID y Nombre
                    .map(p -> Map.of(
                            "id", p.get("id"),
                            "name", p.get("name"),
                            "knownFor", p.getOrDefault("known_for_department", "")
                    ))
                    // Eliminamos duplicados
                    .collect(java.util.stream.Collectors.toMap(
                            m -> (String) m.get("name"),
                            m -> m,
                            (existing, replacement) -> existing,
                            java.util.LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    .limit(5)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // Estos métodos devuelven datos fijos que usamos para armar los selects
    // en el frontend de creación de sala.
    @Override
    public List<TmdbGenre> obtenerGeneros() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/genre/movie/list").queryParam("language", "es-ES").build())
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> lista = (List<Map<String, Object>>) response.get("genres");

            return lista.stream()
                    .map(g -> new TmdbGenre((Integer) g.get("id"), (String) g.get("name")))
                    .sorted(Comparator.comparing(TmdbGenre::name))
                    .toList();

        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Map<String, String>> obtenerIdiomasPrincipales() {
        return List.of(
                Map.of("id", "es", "name", "Español"),
                Map.of("id", "en", "name", "Inglés"),
                Map.of("id", "fr", "name", "Francés"),
                Map.of("id", "it", "name", "Italiano"),
                Map.of("id", "ja", "name", "Japonés"),
                Map.of("id", "ko", "name", "Coreano")
        );
    }

    @Override
    public List<Map<String, Object>> obtenerPlataformasPrincipales() {
        return List.of(
                Map.of("id", 8, "name", "Netflix"),
                Map.of("id", 9, "name", "Prime Video"),
                Map.of("id", 337, "name", "Disney+"),
                Map.of("id", 1899, "name", "HBO Max"),
                Map.of("id", 350, "name", "Apple TV"),
                Map.of("id", 531, "name", "Paramount+")
        );
    }

    @Override
    public List<Map<String, Object>> obtenerPeliculasEpocas() {
        return List.of(
                Map.of("id", "estrenos", "name", "2023-2026", "gte", "2023-01-01", "lte", "2026-12-31"),
                Map.of("id", "modernas", "name", "2010-2022", "gte", "2010-01-01", "lte", "2022-12-31"),
                Map.of("id", "nostalgia", "name", "90s y 00s", "gte", "1990-01-01", "lte", "2009-12-31"),
                Map.of("id", "clasicos", "name", "70s y 80s", "gte", "1970-01-01", "lte", "1989-12-31")
        );
    }

    @Override
    public List<Map<String, Object>> obtenerClasificacionEdad() {
        return List.of(
                Map.of("id", "G", "name", "Familiar", "desc", "Para todos"),
                Map.of("id", "PG-13", "name", "Adolescentes", "desc", "Mayores de 13"),
                Map.of("id", "R", "name", "Adultos", "desc", "Contenido fuerte")
        );
    }

    /**
     * Transforma nuestra entidad "FiltroSala" a los
     * parámetros específicos que espera recibir la API de TMDb.
     */
    private UriBuilder aplicarFiltros(UriBuilder uriBuilder, FiltroSala filtroSala) {
        uriBuilder.queryParam("include_adult", "false"); // Siempre bloqueamos contenido explícito
        uriBuilder.queryParam("certification_country", "US"); // Usamos el estándar de edades americano (PG-13, R)
        uriBuilder.queryParam("with_runtime.gte", "60");
        if (filtroSala == null) return uriBuilder;
        // TMDb usa el símbolo "|" para decir "OR" entre varios filtros del mismo tipo,
        // por eso transformamos nuestras listas a ese formato.
        if (filtroSala.getGeneros() != null && !filtroSala.getGeneros().isEmpty()) {
            uriBuilder.queryParam("with_genres", filtroSala.getGeneros().stream().
                    map(String::valueOf).collect(Collectors.joining("|")));
        }
        if (filtroSala.getIdiomas() != null && !filtroSala.getIdiomas().isEmpty()) {
            uriBuilder.queryParam("with_original_language", String.join("|", filtroSala.getIdiomas()));
        } else {
            uriBuilder.queryParam("with_original_language", "es|en|fr|it|ja|ko");
        }
        if (filtroSala.getActoresIds() != null && !filtroSala.getActoresIds().isEmpty()) {
            uriBuilder.queryParam("with_cast", filtroSala.getActoresIds().stream().
                    map(String::valueOf).collect(Collectors.joining("|")));
        }
        if (filtroSala.getDirectoresIds() != null && !filtroSala.getDirectoresIds().isEmpty()) {
            uriBuilder.queryParam("with_crew", filtroSala.getDirectoresIds().stream().
                    map(String::valueOf).collect(Collectors.joining("|")));
        }
        // Filtro de Plataformas (JustWatch)
        if (filtroSala.getPlataformasIds() != null && !filtroSala.getPlataformasIds().isEmpty()) {
            uriBuilder.queryParam("with_watch_providers", filtroSala.getPlataformasIds().stream()
                    .map(String::valueOf).collect(Collectors.joining("|")));
            uriBuilder.queryParam("watch_region", "AR"); // Configuramos región Argentina
            uriBuilder.queryParam("watch_monetization_types", "flatrate"); // Solo servicios de suscripción (no alquiler)
        }
        if (filtroSala.getFechaGte() != null && !filtroSala.getFechaGte().isBlank()) {
            uriBuilder.queryParam("primary_release_date.gte", filtroSala.getFechaGte());
        }
        if (filtroSala.getFechaLte() != null && !filtroSala.getFechaLte().isBlank()) {
            uriBuilder.queryParam("primary_release_date.lte", filtroSala.getFechaLte());
        }
        if (filtroSala.getCertificacion() != null && !filtroSala.getCertificacion().isBlank()) {
            uriBuilder.queryParam("certification", filtroSala.getCertificacion());
        } else if (filtroSala.getDesnudos() == null || !filtroSala.getDesnudos()) {
            // Restricción por defecto si no quieren ver películas fuertes
            uriBuilder.queryParam("certification.lte", "PG-13");
        }

        return uriBuilder;
    }

    /**
     * Llama a la API pidiendo toda la info de una sola película,
     * y la convierte a nuestro DTO (Pelicula) limpiando y organizando los datos.
     */
    private Pelicula obtenerDetallesPelicula(Long tmdbId, Set<Integer> plataformasPermitidas) {
        TmdbMovieDetails detalles = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("language", "es-ES") // Pedimos sinopsis en español
                        // 'append_to_response' nos ahorra hacer 3 peticiones distintas,
                        // trayendo el reparto (credits) y las plataformas (watch/providers) en la misma llamada
                        .queryParam("append_to_response", "credits,watch/providers")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbMovieDetails.class);

        if (detalles == null) {
            throw new RuntimeException("No se pudieron obtener los detalles de la pelicula con id: " + tmdbId);
        }

        return Pelicula.builder()
                .tmdbId(detalles.id())
                .titulo(detalles.title())
                .sinopsis(detalles.overview())
                // Si la película no tiene póster oficial, le ponemos el GIF de Travolta :P
                .posterPath(detalles.posterPath() != null
                        ? "https://image.tmdb.org/t/p/w500" + detalles.posterPath()
                        : "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdm" +
                          "xiMGo0anN4Y3EwcmplcXoyem5kZmRydmtwNXJsOXl1cW0zN2M5YiZl" +
                          "cD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/6uGhT1O4sxpi8/giphy.gif")
                .fechaEstreno(detalles.releaseDate())
                .rating(detalles.voteAverage())
                .generos(detalles.genres() != null ? detalles.genres().stream().map(TmdbGenre::name).toList() : List.of())
                .idiomaOriginal(traducirCodigoIdioma(detalles.originalLanguage()))
                // Extraemos los primeros 5 actores principales
                .cast(detalles.credits() != null && detalles.credits().cast() != null
                        ? detalles.credits().cast().stream()
                          .filter(c -> "Acting".equals(c.knownForDepartment()))
                          .map(TmdbCast::name)
                          .limit(5)
                          .toList() : List.of())
                // Extraemos a los directores
                .directores(detalles.credits() != null && detalles.credits().crew() != null
                        ? detalles.credits().crew().stream()
                          .filter(c -> "Director".equals(c.job()))
                          .map(TmdbCrew::name)
                          .toList() : List.of())
                // Calculamos en qué plataformas se puede ver (respetando los filtros)
                .plataformas(obtenerNombresPlataformas(detalles, plataformasPermitidas))
                .plataformasLogos(obtenerLogosPlataformas(detalles, plataformasPermitidas))
                .build();
    }

    private String traducirCodigoIdioma(String codigo) {
        if (codigo == null || codigo.isEmpty()) return "Desconocido";
        Locale loc = Locale.forLanguageTag(codigo);
        String nombre = loc.getDisplayLanguage(Locale.forLanguageTag("es-ES"));
        if (nombre == null || nombre.isEmpty()) return "Desconocido";
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }

    private List<String> obtenerNombresPlataformas(TmdbMovieDetails detalles, Set<Integer> permitidas) {
        try {
            if (detalles.watchProviders() != null
                    && detalles.watchProviders().results() != null
                    && detalles.watchProviders().results().containsKey("AR")) {
                TmdbProviderRegion region = detalles.watchProviders().results().get("AR");
                if (region.flatrate() != null) {
                    return region.flatrate().stream()
                            .filter(p -> permitidas.isEmpty() || permitidas.contains(p.providerId()))
                            .map(p -> normalizarNombrePlataforma(p.providerName()))
                            .toList();
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo plataformas: " + e.getMessage());
        }
        return List.of();
    }

    private List<String> obtenerLogosPlataformas(TmdbMovieDetails detalles, Set<Integer> permitidas) {
        try {
            if (detalles.watchProviders() != null
                    && detalles.watchProviders().results() != null
                    && detalles.watchProviders().results().containsKey("AR")) {
                TmdbProviderRegion region = detalles.watchProviders().results().get("AR");
                if (region.flatrate() != null) {
                    return region.flatrate().stream()
                            .filter(p -> permitidas.isEmpty() || permitidas.contains(p.providerId()))
                            .map(TmdbProvider::logoPath)
                            .toList();
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo plataformas: " + e.getMessage());
        }
        return List.of();
    }

    private String normalizarNombrePlataforma(String nombre) {
        if (nombre == null) return "";
        return switch (nombre.toLowerCase()) {
            case "disney plus", "disney+" -> "Disney+";
            case "amazon prime video", "amazon prime" -> "Prime Video";
            case "hbo max", "max", "hbo max amazon channel" -> "HBO Max";
            case "apple tv plus", "apple tv+" -> "Apple TV+";
            case "paramount plus", "paramount+ amazon channel", "paramount+ roku premium channel" -> "Paramount+";
            default -> nombre;
        };
    }

    @Override
    public List<Pelicula> obtenerListaPeliculasFiltroMultiplesEpocas(FiltroSala filtroSala, List<String> epocasIds, int maxResultados) {
        if (epocasIds == null || epocasIds.isEmpty()) {
            return obtenerListaPeliculasFiltro(filtroSala);
        }
        Map<String, Map<String, Object>> epocasMap = indexarEpocasPorId();
        List<List<Pelicula>> porEpoca = new ArrayList<>();
        for (String epocaId : epocasIds) {
            Map<String, Object> epoca = epocasMap.get(epocaId);
            if (epoca == null) {
                continue;
            }
            String gte = (String) epoca.get("gte");
            String lte = (String) epoca.get("lte");
            FiltroSala filtroEpoca = crearFiltroConRango(filtroSala, gte, lte);
            porEpoca.add(obtenerListaPeliculasFiltro(filtroEpoca));
        }
        return intercalarResultados(porEpoca, maxResultados);
    }

    private Map<String, Map<String, Object>> indexarEpocasPorId() {
        return obtenerPeliculasEpocas().stream()
                .filter(e -> e.get("id") != null)
                .collect(Collectors.toMap(
                        e -> e.get("id").toString(),
                        e -> e,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private List<Pelicula> intercalarResultados(List<List<Pelicula>> listas, int maxResultados) {
        LinkedHashMap<Long, Pelicula> resultado = new LinkedHashMap<>();
        List<Pelicula> pool = new ArrayList<>();
        for (List<Pelicula> lista : listas) {
            if (lista != null && !lista.isEmpty()) {
                pool.addAll(lista);
            }
        }
        Collections.shuffle(pool, rnd);
        for (Pelicula peli : pool) {
            if (peli == null) {
                continue;
            }
            resultado.putIfAbsent(peli.getTmdbId(), peli);
            if (resultado.size() >= maxResultados) {
                break;
            }
        }
        return new ArrayList<>(resultado.values());
    }

    private String elegirEpocaAleatoria(List<String> epocasIds) {
        if (epocasIds == null || epocasIds.isEmpty()) {
            return null;
        }
        return epocasIds.get(rnd.nextInt(epocasIds.size()));
    }

    private FiltroSala crearFiltroConEpoca(FiltroSala base, String epocaId) {
        Map<String, Map<String, Object>> epocasMap = indexarEpocasPorId();
        Map<String, Object> epoca = epocasMap.get(epocaId);
        if (epoca == null) {
            return crearFiltroConRango(base, null, null);
        }
        String gte = (String) epoca.get("gte");
        String lte = (String) epoca.get("lte");
        return crearFiltroConRango(base, gte, lte);
    }

    private FiltroSala crearFiltroConRango(FiltroSala base, String gte, String lte) {
        FiltroSala filtro = new FiltroSala();
        if (base != null) {
            filtro.setGeneros(new ArrayList<>(base.getGeneros()));
            filtro.setIdiomas(new ArrayList<>(base.getIdiomas()));
            filtro.setActoresIds(new ArrayList<>(base.getActoresIds()));
            filtro.setDirectoresIds(new ArrayList<>(base.getDirectoresIds()));
            filtro.setPlataformasIds(new ArrayList<>(base.getPlataformasIds()));
            filtro.setCertificacion(base.getCertificacion());
            filtro.setEdadMinima(base.getEdadMinima());
            filtro.setDesnudos(base.getDesnudos());
        }
        filtro.setFechaGte(gte);
        filtro.setFechaLte(lte);
        return filtro;
    }
}
