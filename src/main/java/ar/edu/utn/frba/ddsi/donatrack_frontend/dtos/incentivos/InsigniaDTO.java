package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InsigniaDTO(
        Long id,
        String nombre,
        String descripcion,
        @JsonAlias({"logoUrl", "urlImagen"})
        String urlImagen,
        LocalDate fechaObtencion,
        Boolean esVisible
) {
    // Constructor secundario conveniente si algunos campos no vienen en el JSON inicial
    public InsigniaDTO(String nombre, String descripcion) {
        this(null, nombre, descripcion, null, null, true);
    }
}