package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.time.LocalDate;
import java.util.List;

public record DonacionEntregadaMapaResponse(
        Long id,
        String entidadBeneficiaria,
        Double latitud,
        Double longitud,
        LocalDate fechaEntrega,
        List<String> bienes,
        List<String> fotos // ids de las fotos de la recepcion (vienen de logistica, no de donaciones)
) {
    public DonacionEntregadaMapaResponse conFotos(List<String> fotos) {
        return new DonacionEntregadaMapaResponse(id, entidadBeneficiaria, latitud, longitud, fechaEntrega, bienes,
                fotos != null ? fotos : List.of());
    }
}
