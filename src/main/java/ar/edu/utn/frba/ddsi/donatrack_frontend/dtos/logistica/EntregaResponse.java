package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

// Respuesta de GET /logistica/trazabilidad/entregas/{id} (logistica-service)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EntregaResponse(
        String id,
        Long entidadBeneficiariaId,
        String direccion,
        LocalDate fecha,
        String estadoActual,
        List<DonacionEntregada> donacionesSegmentadas,
        List<String> fotosEvidencia
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DonacionEntregada(Long id, String subcategoria, List<BienEntregado> bienes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BienEntregado(String descripcion, String categoria, int cantidad) {
    }

    public boolean enTraslado() {
        return "EN_TRASLADO".equals(estadoActual);
    }

    public boolean entregada() {
        return "ENTREGADA".equals(estadoActual);
    }
}
