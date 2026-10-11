package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

import java.time.LocalDate;

public record InsigniaDTO(
        Long id,
        String nombre,
        String descripcion,
        String urlImagen,
        LocalDate fechaObtencion,
        Boolean esVisible
) {
    // Constructor secundario conveniente si algunos campos no vienen en el JSON inicial
    public InsigniaDTO(String nombre, String descripcion) {
        this(null, nombre, descripcion, null, null, true);
    }
}