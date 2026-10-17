package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes;

// Con el documento alcanza si la persona ya esta registrada; el resto se usa para darla de alta
public record RepresentanteRequest(
        String documento,
        String tipoDoc,
        String nombre,
        Integer edad,
        String genero,
        String direccion,
        String mail,
        String telefono
) {
}
