package ar.edu.utn.frba.ddsi.donatrack_frontend.config.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private final List<String> rechazos = new ArrayList<>();
    // General: 3 de rafaga. Login: 2 de rafaga. Recarga lenta (1 por minuto) para que no se recarguen durante el test
    private final RateLimitFilter filtro = new RateLimitFilter(
            new LimitadorDeSolicitudes(3, 1), new LimitadorDeSolicitudes(2, 1), rechazos::add);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void demasiadosIntentosDeLoginRedirigenAlLoginBloqueado() throws Exception {
        ejecutar(request("POST", "/login", "1.1.1.1"));
        ejecutar(request("POST", "/login", "1.1.1.1"));

        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(request("POST", "/login", "1.1.1.1"), respuesta, cadena);

        assertTrue(respuesta.getRedirectedUrl().startsWith("/login?bloqueado="));
        // No llega a validar la contraseña contra el auth-service
        assertNull(cadena.getRequest());
        assertEquals(List.of("login"), rechazos);
    }

    @Test
    void paginaHtmlRecibe429() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, ejecutar(request("GET", "/", "2.2.2.2")).getStatus());
        }
        MockHttpServletResponse respuesta = ejecutar(request("GET", "/", "2.2.2.2"));
        assertEquals(429, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("Demasiadas solicitudes"));
        assertTrue(respuesta.getHeader("Retry-After") != null);
    }

    @Test
    void pedidoJsonRecibe429EnJson() throws Exception {
        for (int i = 0; i < 3; i++) {
            ejecutar(request("GET", "/donante/mapa/datos", "3.3.3.3"));
        }
        MockHttpServletRequest pedido = request("GET", "/donante/mapa/datos", "3.3.3.3");
        pedido.addHeader("Accept", "application/json");
        MockHttpServletResponse respuesta = ejecutar(pedido);
        assertEquals(429, respuesta.getStatus());
        assertTrue(respuesta.getContentType().startsWith("application/json"));
    }

    @Test
    void usuarioLogueadoSeLimitaPorUsuario() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ana@mail.com", null, List.of(new SimpleGrantedAuthority("ROLE_DONANTE_HUMANO"))));
        ejecutar(request("GET", "/donante/mis-donaciones", "4.4.4.1"));
        ejecutar(request("GET", "/donante/mis-donaciones", "4.4.4.2"));
        ejecutar(request("GET", "/donante/mis-donaciones", "4.4.4.3"));
        assertEquals(429, ejecutar(request("GET", "/donante/mis-donaciones", "4.4.4.4")).getStatus());
        assertEquals(List.of("usuario"), rechazos);
    }

    @Test
    void estaticosNoSeLimitan() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, ejecutar(request("GET", "/assets/css/styles.css", "5.5.5.5")).getStatus());
        }
    }

    private MockHttpServletResponse ejecutar(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(request, respuesta, new MockFilterChain());
        return respuesta;
    }

    private static MockHttpServletRequest request(String metodo, String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRemoteAddr(ip);
        return request;
    }
}
