package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

public record MisionResponseDTO(
        String nombre,
        String descripcion,
        int puntosOtorgados,
        boolean completada
) {}