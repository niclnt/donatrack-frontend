package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

import java.util.List;

public record DashboardIncentivosDTO(
        int puntosTotales,
        int puntosSiguienteNivel,
        double porcentajeAvance,
        List<MisionResponseDTO> misionesActivas
) {}