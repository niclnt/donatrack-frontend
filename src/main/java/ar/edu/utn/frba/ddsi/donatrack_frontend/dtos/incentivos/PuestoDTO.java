package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PuestoDTO(
        String username,
        Integer puesto,
        Integer cantidadMisionesCompletadas,
        String periodoLog
) {}