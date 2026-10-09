package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.time.LocalDate;
import java.util.List;

public record DonacionCreateRequest(
        String descripcion,
        LocalDate fechaDonacion,
        List<BienDTO> bienes
) {}