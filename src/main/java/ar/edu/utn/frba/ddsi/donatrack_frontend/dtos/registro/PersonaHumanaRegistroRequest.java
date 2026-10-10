package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro;

import java.util.List;

// Body de POST /donantes/personaHumana (donaciones-service)
public record PersonaHumanaRegistroRequest(
        String nombre,
        Integer edad,
        String tipoDoc,
        String documento,
        String genero,
        String direccion,
        List<MedioDeContactoDTO> mediosDeContacto,
        MedioDeContactoDTO medioDeContactoPreferido,
        String mail,
        String password
) {
}
