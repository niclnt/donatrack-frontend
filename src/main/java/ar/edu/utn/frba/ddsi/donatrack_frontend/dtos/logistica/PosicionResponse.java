package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

// Ultima posicion reportada por el camion (logistica marca si la senal quedo vieja)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PosicionResponse(
        Double latitud,
        Double longitud,
        Double velocidad,
        Instant timestamp,
        boolean desactualizado,
        long segundosDesdeActualizacion
) {
}
