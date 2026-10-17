package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes;

public record RepresentanteResponse(
        Long id,
        String nombre,
        String tipoDoc,
        String documento,
        String mail
) {
}
