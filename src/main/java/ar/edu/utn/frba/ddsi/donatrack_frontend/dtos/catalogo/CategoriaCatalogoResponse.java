package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo;

import java.util.List;

public record CategoriaCatalogoResponse (
    Long id,
    String nombre,
    List<SubcategoriaCatalogoResponse> subcategorias
){
}
