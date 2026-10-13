package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica;

// Cuerpo de POST /logistica/camiones (logistica-service)
public record CamionCreateRequest(
        String patente,
        Double capacidadVolumen,
        Double altura,
        Double capacidadCarga,
        String nombreChofer
) {
}
