package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

import java.util.List;

public record DashboardIncentivosDTO(
        String categoriaActual,             // "COLABORADOR", "SOSTENEDOR", "TRANSFORMADOR"
        String siguienteCategoria,          // "SOSTENEDOR" (o null si es la máxima)
        int misionesCompletadasCategoria,   // ej: 1
        int totalMisionesCategoria,         // ej: 3 (misiones totales de la categoría actual)
        int misionesFaltantes,              // ej: 2
        List<MisionResponseDTO> misiones,   // Lista de misiones (completadas, en curso y bloqueadas)
        List<InsigniaDTO> insignias         // Insignias obtenidas
) {}