package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

// Respuesta de GET /logistica/rutas/mias (logistica-service)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaResponse(
        String id,
        LocalDate fecha,
        String estadoActual,
        CamionResponse camion,
        String nombreChofer,
        List<EntregaResponse> entregas
) {
    public boolean planificada() {
        return "PLANIFICADA".equals(estadoActual);
    }

    public boolean enCurso() {
        return "EN_CURSO".equals(estadoActual);
    }
}
