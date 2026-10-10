package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro;

import java.util.List;

// Body de POST /donaciones/entidades-beneficiarias (donaciones-service)
public record EntidadBeneficiariaRegistroRequest(
        String razonSocial,
        String direccion,
        String localidad,
        String ciudad,
        String provincia,
        int telefono,
        List<MedioDeContactoDTO> mediosDeContacto,
        String mail,
        String password
) {
}
