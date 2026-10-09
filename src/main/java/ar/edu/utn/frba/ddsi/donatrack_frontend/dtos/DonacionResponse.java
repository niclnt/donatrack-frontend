package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.util.List;

public record DonacionResponse(
        Long id,
        Long donanteId,
        String descripcion,
        List<BienDTO> bienes,
        List<DonacionSegmentadaDTO> donacionesSegmentadas
){}