package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.util.List;

// Fotos de la recepcion de una donacion entregada (idDonacion = id de la donacion segmentada)
public record FotosDonacionResponse(
        Long idDonacion,
        List<String> fotos
) {
}
