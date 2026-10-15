package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.time.LocalDate;
import java.util.List;

public record DonacionEntregadaMapaResponse(
        Long id,
        String entidadBeneficiaria,
        Double latitud,
        Double longitud,
        LocalDate fechaEntrega,
        List<String> bienes
) {
}