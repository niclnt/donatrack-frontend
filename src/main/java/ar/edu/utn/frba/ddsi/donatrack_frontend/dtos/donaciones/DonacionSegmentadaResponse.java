package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionSegmentadaResponse(
        Long id,
        Long donacionPadreId,
        Long donanteId,
        String estadoActual,
        List<BienDTO> bienes,
        LocalDate fechaDonacion,
        String entidadAsignada
) {
}
