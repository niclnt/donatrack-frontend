package ar.edu.utn.frba.ddsi.donatrack_frontend.providers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.AuthResponseDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.AuthApiService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CustomAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthProvider.class);

    private final AuthApiService authApiService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String INCENTIVOS_SERVICE_URL = "http://localhost:8082";

    public CustomAuthProvider(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        AuthResponseDTO respuesta;
        try {
            respuesta = authApiService.login(username, password);
        } catch (ServicioNoDisponibleException e) {
            throw new AuthenticationServiceException(e.getMessage(), e);
        }
        if (respuesta == null) {
            throw new BadCredentialsException("Usuario o contraseña inválidos");
        }

        List<String> roles = respuesta.roles() == null ? List.of() : respuesta.roles();
        List<String> permisos = respuesta.permisos() == null ? List.of() : respuesta.permisos();

        HttpSession session = SesionUsuario.actual();
        session.setAttribute(SesionUsuario.ACCESS_TOKEN, respuesta.accessToken());
        session.setAttribute(SesionUsuario.REFRESH_TOKEN, respuesta.refreshToken());
        session.setAttribute(SesionUsuario.USUARIO_ID, respuesta.usuarioId());
        session.setAttribute(SesionUsuario.ROLES, roles);

        // --- OBTENER DONANTE_ID DESDE DONACIONES-SERVICE ---
        if (roles.contains("DONANTE") || roles.contains("ROLE_DONANTE")) {
            try {
                String DONACIONES_SERVICE_URL = "http://localhost:8080"; // Puerto donde corre donaciones-service

                // Llamada al nuevo endpoint de DonanteController
                Map personaHumana = restTemplate.getForObject(
                        DONACIONES_SERVICE_URL + "/donantes/personaHumana/usuario/" + respuesta.usuarioId(),
                        Map.class
                );

                if (personaHumana != null && personaHumana.get("id") != null) {
                    Long donanteId = Long.valueOf(personaHumana.get("id").toString());
                    session.setAttribute(SesionUsuario.DONANTE_ID, donanteId);
                    log.info("Donante ID {} asignado a la sesión para usuarioId {}", donanteId, respuesta.usuarioId());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener la PersonaHumana para el usuarioId {}: {}", respuesta.usuarioId(), e.getMessage());
            }
        }

        log.info("Usuario {} logueado con roles {}", respuesta.mail(), roles);

        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(rol -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rol)));
        permisos.forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso)));

        return new UsernamePasswordAuthenticationToken(respuesta.mail(), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}