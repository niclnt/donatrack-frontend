package ar.edu.utn.frba.ddsi.donatrack_frontend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Con contraseña temporal (CSV, chofer creado por el admin) solo se puede entrar a la pantalla de cambio:
// cualquier otra pagina redirige ahi hasta que el usuario elija una propia
@Configuration
public class CambioContraseniaObligatorioInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(SesionUsuario.DEBE_CAMBIAR_CONTRASENIA))) {
            response.sendRedirect(request.getContextPath() + SesionUsuario.URL_CAMBIO_CONTRASENIA);
            return false;
        }
        return true;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .excludePathPatterns(SesionUsuario.URL_CAMBIO_CONTRASENIA, "/logout", "/login", "/error",
                        "/css/**", "/js/**", "/img/**", "/assets/**", "/actuator/**", "/favicon.ico");
    }
}
