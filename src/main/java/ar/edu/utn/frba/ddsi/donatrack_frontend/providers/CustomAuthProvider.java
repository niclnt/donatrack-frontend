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

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthProvider.class);

    private final AuthApiService authApiService;

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

        // Roles y permisos vienen en la respuesta del login: el front no necesita la clave del JWT
        List<String> roles = respuesta.roles() == null ? List.of() : respuesta.roles();
        List<String> permisos = respuesta.permisos() == null ? List.of() : respuesta.permisos();

        HttpSession session = SesionUsuario.actual();
        session.setAttribute(SesionUsuario.ACCESS_TOKEN, respuesta.accessToken());
        session.setAttribute(SesionUsuario.REFRESH_TOKEN, respuesta.refreshToken());
        session.setAttribute(SesionUsuario.USUARIO_ID, respuesta.usuarioId());
        session.setAttribute(SesionUsuario.ROLES, roles);
        log.info("Usuario {} logueado con roles {}", respuesta.mail(), roles);

        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(rol -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rol)));
        permisos.forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso)));

        // La contraseña no se guarda en la sesion
        return new UsernamePasswordAuthenticationToken(respuesta.mail(), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
