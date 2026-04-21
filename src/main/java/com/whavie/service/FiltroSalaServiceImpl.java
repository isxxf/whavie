package com.whavie.service;

import com.whavie.dto.FiltroSalaDTO;
import com.whavie.model.FiltroSala;
import com.whavie.model.Sala;
import com.whavie.repository.SalaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FiltroSalaServiceImpl implements FiltroSalaService {
    private final SalaRepository salaRepository;
    private final TMDbService tmDbService;

    public FiltroSalaServiceImpl(SalaRepository salaRepository, TMDbService tmDbService) {
        this.salaRepository = salaRepository;
        this.tmDbService = tmDbService;
    }

    /**
     * Metodo para guardar o actualizar los filtros de una sala
     * Usamos @Transactional debido a que si ocurre algún error a la mitad de este
     * método, Spring deshace todos los cambios automáticamente para que no queden datos a medias.
     */
    @Override
    @Transactional
    public void guardarOActualizarFiltro(String codigoSala, FiltroSalaDTO dto, Long usuarioId) {
        // Buscamos la sala. Si no existe, cortamos todo lanzando un error.
        Sala sala = salaRepository.findByCodigo(codigoSala)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        // La persona que puede cambiar los filtros de la sala es el anfitrión de la misma
        if (!sala.getCreador().getId().equals(usuarioId)) {
            throw new RuntimeException("Solo el creador de la sala puede cambiar los filtros");
        }
        // Si la sala ya tenía un filtro guardado antes, lo editamos.
        // Si es la primera vez que se aplican filtros, creamos uno nuevo vacío.
        FiltroSala filtro = (sala.getFiltroSala() != null) ? sala.getFiltroSala() : new FiltroSala();
        // Copiamos los datos que llegaron del frontend (DTO) hacia nuestra entidad de base de datos.
        filtro.setGeneros(dto.getGeneros());
        filtro.setIdiomas(dto.getIdiomas());
        filtro.setActoresIds(dto.getActoresIds());
        filtro.setDirectoresIds(dto.getDirectoresIds());
        filtro.setEpocasIds(dto.getEpocasIds());
        filtro.setFechaLte(dto.getFechaLte());
        filtro.setFechaGte(dto.getFechaGte());
        filtro.setCertificacion(dto.getCertificacion());
        filtro.setEdadMinima(dto.getEdadMinima() != null ? dto.getEdadMinima().toString() : null);
        filtro.setDesnudos(dto.getDesnudos());
        // Establecemos la relación bidireccional (El filtro le pertenece a la sala y la sala contiene a este filtro).
        filtro.setSala(sala);
        sala.setFiltroSala(filtro);

        salaRepository.save(sala);
    }

    /**
     * Este método actúa como un "traductor".
     * El WebSocket envía un arreglo de objetos muy básicos (tipo y valor).
     * Aquí lo convertimos en un objeto estructurado (FiltroSalaDTO) que Java pueda entender y usar.
     */
    @Override
    public FiltroSalaDTO convertirFiltrosDelWebSocket(List<Map<String, String>> filtrosRecibidos) {
        FiltroSalaDTO filtroDTO = new FiltroSalaDTO();
        // Si el usuario no mandó ningún filtro, devolvemos el objeto vacío
        if (filtrosRecibidos == null || filtrosRecibidos.isEmpty()) {
            return filtroDTO;
        }
        // Recorremos cada filtro que llegó desde el frontend y
        // lo asignamos al campo correspondiente de nuestro DTO según su tipo.
        for (Map<String, String> f : filtrosRecibidos) {
            String tipo = f.get("tipo");
            String id = f.get("id");
            // Si algún dato viene corrupto, lo saltamos y seguimos con el próximo
            if (tipo == null || id == null) {
                continue;
            }
            // Según el "tipo" de filtro, lo guardamos en la lista correspondiente del DTO
            switch (tipo) {
                case "genero" -> filtroDTO.getGeneros().add(Integer.parseInt(id));
                case "idioma" -> filtroDTO.getIdiomas().add(id);
                case "actor" -> filtroDTO.getActoresIds().add(Integer.parseInt(id));
                case "director" -> filtroDTO.getDirectoresIds().add(Integer.parseInt(id));
                case "epoca" -> filtroDTO.getEpocasIds().add(id);
                case "clasificacion" -> filtroDTO.setCertificacion(id);
                case "plataforma" -> filtroDTO.getPlataformasIds().add(Integer.parseInt(id));
                default -> {
                }
            }
        }
        if (filtroDTO.getEpocasIds() != null && filtroDTO.getEpocasIds().size() == 1) {
            String epocaId = filtroDTO.getEpocasIds().get(0);
            tmDbService.obtenerPeliculasEpocas().stream()
                    .filter(e -> e.get("id").equals(epocaId))
                    .findFirst()
                    .ifPresent(epoca -> {
                        filtroDTO.setFechaGte((String) epoca.get("gte"));
                        filtroDTO.setFechaLte((String) epoca.get("lte"));
                    });
        }
        return filtroDTO;
    }
}
