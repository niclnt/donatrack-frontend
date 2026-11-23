package ar.edu.utn.frba.ddsi.donatrack_frontend.config.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Rate limiting del front, que es la puerta de entrada publica del sistema. Aplica dos limites:
 * <ul>
 *   <li><b>Login</b> (POST /login): limite estricto por IP, contra ataques de fuerza bruta a las contraseñas.</li>
 *   <li><b>General</b>: por usuario logueado (o por IP si es anonimo) para el resto de las paginas.</li>
 * </ul>
 * Los recursos estaticos y el actuator no se limitan. Al pasarse del limite se responde 429 Too Many Requests
 * (pagina HTML o JSON segun lo que pida el navegador) sin llegar a los controllers ni a los servicios del backend.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final LimitadorDeSolicitudes limitadorGeneral;
    private final LimitadorDeSolicitudes limitadorLogin;
    private final Consumer<String> alRechazar;

    /**
     * @param alRechazar se llama con el tipo de limite superado ("login", "usuario" o "anonimo"), para la metrica
     */
    public RateLimitFilter(LimitadorDeSolicitudes limitadorGeneral,
                           LimitadorDeSolicitudes limitadorLogin,
                           Consumer<String> alRechazar) {
        this.limitadorGeneral = limitadorGeneral;
        this.limitadorLogin = limitadorLogin;
        this.alRechazar = alRechazar;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/assets/") || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/img/") || uri.startsWith("/actuator") || uri.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();

        if ("POST".equals(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            LimitadorDeSolicitudes.Resultado resultado = limitadorLogin.intentarConsumir("login:" + ip);
            if (!resultado.permitida()) {
                registrarRechazo("login", "ip:" + ip, request, resultado);
                response.setHeader("Retry-After", String.valueOf(resultado.segundosParaReintentar()));
                response.sendRedirect("/login?bloqueado=" + resultado.segundosParaReintentar());
                return;
            }
        }

        String usuario = usuarioLogueado();
        String clave = usuario != null ? "usuario:" + usuario : "ip:" + ip;
        LimitadorDeSolicitudes.Resultado resultado = limitadorGeneral.intentarConsumir(clave);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limitadorGeneral.getCapacidad()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(resultado.restantes()));

        if (resultado.permitida()) {
            filterChain.doFilter(request, response);
            return;
        }

        registrarRechazo(usuario != null ? "usuario" : "anonimo", clave, request, resultado);
        responder429(request, response, resultado.segundosParaReintentar());
    }

    private void registrarRechazo(String tipo, String clave, HttpServletRequest request,
                                  LimitadorDeSolicitudes.Resultado resultado) {
        log.warn("[RATE-LIMIT] Solicitud rechazada ({}): cliente={} {} {} (reintentar en {} s)",
                tipo, clave, request.getMethod(), request.getRequestURI(), resultado.segundosParaReintentar());
        alRechazar.accept(tipo);
    }

    private static String usuarioLogueado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    private static void responder429(HttpServletRequest request, HttpServletResponse response, long segundos)
            throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(segundos));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json") && !accept.contains("text/html")) {
            // Pedidos hechos por JavaScript (por ejemplo el mapa de seguimiento que se actualiza solo)
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"mensaje\":"
                    + "\"Demasiadas solicitudes. Reintenta en " + segundos + " segundos.\"}");
            return;
        }

        response.setContentType("text/html");
        response.getWriter().write("""
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Demasiadas solicitudes · DonaTrack</title>
                  <link rel="stylesheet" href="/assets/css/styles.css">
                </head>
                <body>
                <main id="main" class="page-body">
                  <div class="container empty-state" style="max-width: 560px;" role="alert">
                    <div class="entity__icon" aria-hidden="true" style="margin-inline:auto; font-size:2rem;">⏳</div>
                    <h1 class="mt-4">Demasiadas solicitudes</h1>
                    <p class="muted mt-4">Hiciste muchas solicitudes en poco tiempo. Esperá %d segundos y volvé a intentar.</p>
                    <a class="btn btn--primary mt-4" href="/">Volver al inicio</a>
                  </div>
                </main>
                </body>
                </html>
                """.formatted(segundos));
    }
}
