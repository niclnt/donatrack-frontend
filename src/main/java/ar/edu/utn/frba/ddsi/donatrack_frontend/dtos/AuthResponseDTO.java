package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.util.List;

// Respuesta del auth-service: los tokens y los datos del usuario, para armar la sesion sin abrir el JWT
// debeCambiarContrasenia: la contraseña es temporal (CSV, chofer creado por el admin); null si auth no lo manda
public record AuthResponseDTO(String accessToken, String refreshToken,
                              Long usuarioId, String mail, List<String> roles, List<String> permisos,
                              Boolean debeCambiarContrasenia) {
}
