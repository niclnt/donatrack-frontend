package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo;

import java.util.List;

public record CategoriaCatalogoResponse (
    Long id,
    String nombre,
    // Para mostrar el vencimiento y el estado del bien solo cuando corresponden (null si el backend no los manda)
    Boolean esPerecedero,
    Boolean requiereEstadoUso,
    List<SubcategoriaCatalogoResponse> subcategorias
){
}
