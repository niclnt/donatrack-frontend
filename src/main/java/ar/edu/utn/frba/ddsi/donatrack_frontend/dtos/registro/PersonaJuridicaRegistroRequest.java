package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro;

import java.util.List;

// Body de POST /donantes/personaJuridica (donaciones-service)
public record PersonaJuridicaRegistroRequest(
        String razonSocial,
        String tipoJuridico,
        List<MedioDeContactoDTO> mediosDeContacto,
        MedioDeContactoDTO medioDeContactoPreferido,
        String rubro,
        String mail,
        String password
) {
}
