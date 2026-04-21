package com.whavie.service;

import com.whavie.dto.FiltroSalaDTO;
import java.util.List;
import java.util.Map;

public interface FiltroSalaService {
    void guardarOActualizarFiltro(String codigoSala, FiltroSalaDTO filtro, Long usuarioId);
    FiltroSalaDTO convertirFiltrosDelWebSocket(List<Map<String, String>> filtrosRecibidos);
}
