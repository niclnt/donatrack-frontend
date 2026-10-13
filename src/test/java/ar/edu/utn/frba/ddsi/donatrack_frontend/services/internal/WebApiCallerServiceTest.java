package ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.SesionExpiradaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WebApiCallerServiceTest {

    private static final String AUTH = "http://auth/api/auth";
    private static final String API = "http://donaciones/recurso";

    private MockRestServiceServer server;
    private WebApiCallerService caller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        caller = new WebApiCallerService(builder.build(), AUTH);

        // Simula la HttpSession del usuario logueado
        session = new MockHttpSession();
        session.setAttribute(SesionUsuario.ACCESS_TOKEN, "access-viejo");
        session.setAttribute(SesionUsuario.REFRESH_TOKEN, "refresh-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void mandaElAccessTokenDeLaSesion() {
        server.expect(requestTo(API))
                .andExpect(header("Authorization", "Bearer access-viejo"))
                .andRespond(withSuccess("{\"valor\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertEquals("ok", caller.get(API, Map.class).get("valor"));
        server.verify();
    }

    @Test
    void anteUn401RefrescaElTokenYReintentaUnaVez() {
        server.expect(requestTo(API)).andExpect(header("Authorization", "Bearer access-viejo"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(AUTH + "/refresh")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.refreshToken").value("refresh-1"))
                .andRespond(withSuccess("{\"accessToken\":\"access-nuevo\",\"refreshToken\":\"refresh-1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(API)).andExpect(header("Authorization", "Bearer access-nuevo"))
                .andRespond(withSuccess("{\"valor\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertEquals("ok", caller.get(API, Map.class).get("valor"));
        assertEquals("access-nuevo", session.getAttribute(SesionUsuario.ACCESS_TOKEN));
        server.verify();
    }

    @Test
    void un403NoDisparaRefresh() {
        server.expect(requestTo(API)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Sin permiso\"}"));

        ApiException e = assertThrows(ApiException.class, () -> caller.get(API, String.class));
        assertEquals(403, e.getStatus());
        assertEquals("Sin permiso", e.getMessage());
        server.verify(); // si hubiera intentado refrescar, habria una llamada no esperada
    }

    @Test
    void siElRefreshFallaLaSesionExpiraYSeInvalida() {
        server.expect(requestTo(API)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(AUTH + "/refresh")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(SesionExpiradaException.class, () -> caller.get(API, String.class));
        assertTrue(session.isInvalid());
    }

    @Test
    void sinTokenEnLaSesionEsSesionExpirada() {
        session.removeAttribute(SesionUsuario.ACCESS_TOKEN);

        assertThrows(SesionExpiradaException.class, () -> caller.get(API, String.class));
    }

    @Test
    void servicioCaidoEsServicioNoDisponible() {
        server.expect(requestTo(API)).andRespond(withException(new IOException("Connection refused")));

        assertThrows(ServicioNoDisponibleException.class, () -> caller.get(API, String.class));
    }

    @Test
    void elMensajeDeErrorDelServicioLlegaAlFront() {
        server.expect(requestTo(API)).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"La patente ya existe\"}"));

        ApiException e = assertThrows(ApiException.class, () -> caller.post(API, "{}", String.class));
        assertEquals("La patente ya existe", e.getMessage());
    }

    @Test
    void unPostSinCuerpoSeMandaIgual() {
        server.expect(requestTo(API)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-viejo"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertEquals("ok", caller.post(API, null, String.class));
        server.verify();
    }

    @Test
    void elMultipartMandaElArchivoConElToken() {
        server.expect(requestTo(API)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-viejo"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("filename=\"donantes.csv\"")))
                .andRespond(withSuccess("{\"filasLeidas\":2}", MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("archivo", new ByteArrayResource("a,b\n1,2".getBytes()) {
            @Override
            public String getFilename() {
                return "donantes.csv";
            }
        });

        assertEquals(2, caller.postMultipart(API, partes, Map.class).get("filasLeidas"));
        server.verify();
    }
}
