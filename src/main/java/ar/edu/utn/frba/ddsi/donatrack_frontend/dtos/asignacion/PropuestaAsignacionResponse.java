package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.asignacion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Respuesta de GET /api/asignaciones/rankings/top (donaciones-service): una necesidad candidata para una donacion
@JsonIgnoreProperties(ignoreUnknown = true)
public record PropuestaAsignacionResponse(
        Long idDonacionSegmentada,
        String nombreDonacion,
        Long idNecesidad,
        String descripcionNecesidad,
        Map<String, Double> puntajesPorAlgoritmo,
        LocalDateTime fechaCalculo,
        String donante,
        LocalDate fechaDonacion,
        List<BienPropuesto> bienes,
        int cantidadDonada,
        String entidad,
        String tipoNecesidad,
        String detalleNecesidad,
        int cantidadSolicitada,
        int cantidadRecibida,
        int cantidadFaltante
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BienPropuesto(String descripcion, int cantidad, String unidad, LocalDate fechaVencimiento, boolean usado) {
    }

    // Cuanto de lo que le falta a la necesidad cubre esta donacion
    public boolean cubreTotalmente() {
        return cantidadFaltante > 0 && cantidadDonada >= cantidadFaltante;
    }
}
