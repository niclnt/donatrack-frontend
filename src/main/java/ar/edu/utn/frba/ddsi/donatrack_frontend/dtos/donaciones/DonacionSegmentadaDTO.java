package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

// Esta anotación evita que la app explote si el backend manda datos que no definimos acá
@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionSegmentadaDTO(
    Long id,
    Long donanteId,
    SubcategoriaRef subcategoria,
    String estadoActual,
    List<BienDTO> bienes,
    List<Object> historial,
    LocalDate fechaDonacion,
    String entidadAsignada
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubcategoriaRef(Long id, String nombre) {
    }
}