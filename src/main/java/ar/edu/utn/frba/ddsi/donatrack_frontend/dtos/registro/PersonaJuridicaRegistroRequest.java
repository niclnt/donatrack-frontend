package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro;

// Body de POST /donantes/personaJuridica (donaciones-service)
public record PersonaJuridicaRegistroRequest(
        String razonSocial,
        String tipoJuridico,
        String rubro,
        String mail,
        String password
) {
}
