package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos;

public record MisionResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Double progreso,            // Porcentaje de avance (0.0 a 100.0)
        String detalleProgreso,     // Ej: "2 de 3 donaciones realizadas"
        String estado,              // "COMPLETADA", "EN_PROGRESO", "BLOQUEADA"
        InsigniaDTO insigniaRecompensa
) {
    // Constructor de compatibilidad rápida
    public MisionResponseDTO(String nombre, String descripcion, Double progreso, String detalleProgreso) {
        this(null, nombre, descripcion, progreso, detalleProgreso, "EN_PROGRESO", null);
    }
}