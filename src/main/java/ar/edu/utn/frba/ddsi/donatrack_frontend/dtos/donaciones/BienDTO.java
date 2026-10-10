package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import java.time.LocalDate;

public record BienDTO(
        String descripcion,
        CategoriaRef categoria,
        SubcategoriaRef subcategoria,
        UnidadRef unidad,
        int cantidad,
        LocalDate fechaVencimiento,
        boolean usado,
        boolean vencido,
        Double pesoUnitarioKg,
        Double volumenUnitarioM3
) {
    // Modelos específicos para cubrir los booleanos y evitar el error "null to boolean"
    public record CategoriaRef(Long id, String nombre, boolean esPerecedero, boolean requiereEstadoUso) {}
    public record SubcategoriaRef(Long id, String nombre) {}
    public record UnidadRef(Long id, String nombre, String abreviatura) {}
}