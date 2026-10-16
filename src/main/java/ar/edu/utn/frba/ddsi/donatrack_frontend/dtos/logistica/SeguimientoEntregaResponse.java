package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Respuesta de GET /logistica/monitoreo/{donantes|beneficiarios}/{id}/seguimiento: una entrega en una ruta en curso
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeguimientoEntregaResponse(
        String rutaId,
        String entregaId,
        String patente,
        String estadoEntrega,
        String direccionDestino
) {
}
