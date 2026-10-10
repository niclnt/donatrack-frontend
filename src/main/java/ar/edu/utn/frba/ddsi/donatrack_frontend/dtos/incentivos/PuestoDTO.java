package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

public record PuestoDTO(
        int puesto,
        String username,
        int cantidadMisionesCompletadas
) {}