package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica;

// Respuesta de GET/POST /logistica/camiones (logistica-service)
public record CamionResponse(
        String patente,
        Double capacidadVolumen,
        Double altura,
        Double capacidadCarga,
        Boolean disponible
) {
}
