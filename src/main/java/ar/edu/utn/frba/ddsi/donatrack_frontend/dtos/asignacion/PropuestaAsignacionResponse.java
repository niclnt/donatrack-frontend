package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.asignacion;

import java.time.LocalDateTime;
import java.util.Map;

// Respuesta de GET /api/asignaciones/rankings/top (donaciones-service): una necesidad candidata para una donacion
public record PropuestaAsignacionResponse(
        Long idDonacionSegmentada,
        String nombreDonacion,
        Long idNecesidad,
        String descripcionNecesidad,
        Map<String, Double> puntajesPorAlgoritmo,
        LocalDateTime fechaCalculo
) {
}
