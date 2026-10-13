package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PuestoDTO(
        int puesto,
        String username,
        int cantidadMisionesCompletadas
) {}