package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

// Esta anotación evita que la app explote si el backend manda datos que no definimos acá
@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionSegmentadaDTO(
        Long id,
        Long donanteId,
        String subcategoria,   // Antes era Subcategoria
        String estadoActual,   // Antes era PosibleEstado
        List<BienDTO> bienes,
        List<Object> historial, // Lo dejamos genérico para no tener que crear EstadoResponse
        LocalDate fechaDonacion
) {}