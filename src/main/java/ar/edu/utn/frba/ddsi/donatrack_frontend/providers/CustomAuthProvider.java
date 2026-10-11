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

    public CustomAuthProvider(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.info("[AUTH-PROVIDER] ===== INICIANTE AUTENTICACION =====");
        log.info("[AUTH-PROVIDER] Intentando login para usuario: {}", username);

        AuthResponseDTO respuesta;
        try {
            respuesta = authApiService.login(username, password);
        } catch (ServicioNoDisponibleException e) {
            log.error("[AUTH-PROVIDER] Servicio de Auth NO disponible: {}", e.getMessage());
            throw new AuthenticationServiceException(e.getMessage(), e);
        }

        if (respuesta == null) {
            log.warn("[AUTH-PROVIDER] Credenciales invalidas para usuario: {}", username);
            throw new BadCredentialsException("Usuario o contraseña inválidos");
        }

        List<String> roles = respuesta.roles() == null ? List.of() : respuesta.roles();
        List<String> permisos = respuesta.permisos() == null ? List.of() : respuesta.permisos();

        log.info("[AUTH-PROVIDER] Login exitoso en Auth-Service.");
        log.info("[AUTH-PROVIDER] USUARIO_ID recibido: {}", respuesta.usuarioId());
        log.info("[AUTH-PROVIDER] ROLES recibidos: {}", roles);

        HttpSession session = SesionUsuario.actual();
        session.setAttribute(SesionUsuario.ACCESS_TOKEN, respuesta.accessToken());
        session.setAttribute(SesionUsuario.REFRESH_TOKEN, respuesta.refreshToken());
        session.setAttribute(SesionUsuario.USUARIO_ID, respuesta.usuarioId());
        session.setAttribute(SesionUsuario.ROLES, roles);

        // --- OBTENER DONANTE_ID DESDE DONACIONES-SERVICE ---
        // Verificamos ROL_DONANTE_HUMANO / DONANTE_JURIDICO para coincidir con el log real
        boolean esDonante = roles.stream().anyMatch(r ->
                r.contains("DONANTE") || r.equals(SesionUsuario.ROL_DONANTE_HUMANO) || r.equals(SesionUsuario.ROL_DONANTE_JURIDICO)
        );

        if (esDonante) {
            log.info("[AUTH-PROVIDER] El usuario tiene rol de DONANTE. Consultando a donaciones-service...");
            try {
                String DONACIONES_SERVICE_URL = "http://localhost:8080";
                String urlConsulta = DONACIONES_SERVICE_URL + "/donantes/personaHumana/usuario/" + respuesta.usuarioId();

                log.info("[AUTH-PROVIDER] Realizando GET a: {}", urlConsulta);

                Map personaHumana = restTemplate.getForObject(urlConsulta, Map.class);

                log.info("[AUTH-PROVIDER] Respuesta recibida de Donaciones: {}", personaHumana);

                if (personaHumana != null && personaHumana.get("id") != null) {
                    Long donanteId = Long.valueOf(personaHumana.get("id").toString());
                    session.setAttribute(SesionUsuario.DONANTE_ID, donanteId);
                    log.info("[AUTH-PROVIDER] ¡EXITO! DONANTE_ID {} guardado en la sesion.", donanteId);
                } else {
                    log.warn("[AUTH-PROVIDER] La respuesta de donaciones fue nula o no contiene el campo 'id'.");
                }
            } catch (Exception e) {
                log.error("[AUTH-PROVIDER] Error al consultar donaciones-service para usuarioId {}: {}", respuesta.usuarioId(), e.getMessage(), e);
            }
        } else {
            log.warn("[AUTH-PROVIDER] El usuario NO coincide con los roles de donante evaluados. Roles actual: {}", roles);
        }

        log.info("[AUTH-PROVIDER] Usuario {} logueado correctamente con roles {}", respuesta.mail(), roles);

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