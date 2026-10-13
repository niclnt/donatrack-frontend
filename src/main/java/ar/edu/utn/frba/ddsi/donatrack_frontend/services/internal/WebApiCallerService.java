package ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.AuthResponseDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.SesionExpiradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WebApiCallerService {

    private static final Logger log = LoggerFactory.getLogger(WebApiCallerService.class);

    private final RestClient restClient;
    private final String authServiceUrl;

    @Autowired
    public WebApiCallerService(@Value("${auth.service.url}") String authServiceUrl) {
        this(RestClient.create(), authServiceUrl);
    }

    WebApiCallerService(RestClient restClient, String authServiceUrl) {
        this.restClient = restClient;
        this.authServiceUrl = authServiceUrl;
    }

    public <T> T get(String url, Class<T> tipo) {
        return conToken(token -> restClient.get().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().body(tipo));
    }

    public <T> List<T> getList(String url, Class<T[]> tipoArray) {
        return comoLista(conToken(token -> restClient.get().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().body(tipoArray)));
    }

    public <T> T post(String url, Object body, Class<T> tipo) {
        return conToken(token -> conCuerpo(restClient.post().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token), body)
                .retrieve().body(tipo));
    }

    public <T> T put(String url, Object body, Class<T> tipo) {
        return conToken(token -> conCuerpo(restClient.put().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token), body)
                .retrieve().body(tipo));
    }

    // RestClient falla con NullPointerException si se le pasa body(null): sin cuerpo no se setea
    private static RestClient.RequestBodySpec conCuerpo(RestClient.RequestBodySpec request, Object body) {
        return body == null ? request : request.body(body);
    }

    // Para subir archivos (ej. importacion CSV)
    public <T> T postMultipart(String url, MultiValueMap<String, ?> partes, Class<T> tipo) {
        return conToken(token -> restClient.post().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(partes)
                .retrieve().body(tipo));
    }

    public void delete(String url) {
        conToken(token -> restClient.delete().uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().toBodilessEntity());
    }

    // Para paginas publicas (landing, mapa): sin token
    public <T> List<T> getListPublica(String url, Class<T[]> tipoArray) {
        try {
            return comoLista(traducirErrores(token -> restClient.get().uri(url).retrieve().body(tipoArray)));
        } catch (TokenRechazadoException e) {
            throw new ApiException(401, "El recurso requiere iniciar sesión");
        }
    }

    // Para formularios publicos (registro): sin token
    public <T> T postPublico(String url, Object body, Class<T> tipo) {
        try {
            return traducirErrores(token -> restClient.post().uri(url).body(body).retrieve().body(tipo));
        } catch (TokenRechazadoException e) {
            throw new ApiException(401, "El recurso requiere iniciar sesión");
        }
    }

    private <T> T conToken(ApiCall<T> apiCall) {
        String accessToken = SesionUsuario.accessToken();
        if (accessToken == null) {
            throw new SesionExpiradaException();
        }
        try {
            return traducirErrores(ignorado -> apiCall.execute(accessToken));
        } catch (TokenRechazadoException e) {
            // Token vencido: refrescamos y reintentamos una sola vez
            String nuevoAccessToken = refrescarTokens();
            try {
                return traducirErrores(ignorado -> apiCall.execute(nuevoAccessToken));
            } catch (TokenRechazadoException otraVez) {
                cerrarSesion();
                throw new SesionExpiradaException();
            }
        }
    }

    private String refrescarTokens() {
        String refreshToken = SesionUsuario.refreshToken();
        if (refreshToken == null) {
            cerrarSesion();
            throw new SesionExpiradaException();
        }
        try {
            AuthResponseDTO tokens = restClient.post().uri(authServiceUrl + "/refresh")
                    .body(Map.of("refreshToken", refreshToken))
                    .retrieve().body(AuthResponseDTO.class);
            SesionUsuario.actual().setAttribute(SesionUsuario.ACCESS_TOKEN, tokens.accessToken());
            SesionUsuario.actual().setAttribute(SesionUsuario.REFRESH_TOKEN, tokens.refreshToken());
            return tokens.accessToken();
        } catch (RuntimeException e) {
            log.info("No se pudo refrescar el token: {}", e.getMessage());
            cerrarSesion();
            throw new SesionExpiradaException();
        }
    }

    private void cerrarSesion() {
        SecurityContextHolder.clearContext();
        SesionUsuario.actual().invalidate();
    }

    private <T> T traducirErrores(ApiCall<T> llamada) {
        try {
            return llamada.execute(null);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                throw new TokenRechazadoException();
            }
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new NotFoundException(mensajeDeError(e));
            }
            throw new ApiException(e.getStatusCode().value(), mensajeDeError(e));
        } catch (RestClientResponseException e) {
            throw new ApiException(e.getStatusCode().value(), mensajeDeError(e));
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException("El servicio no está disponible en este momento", e);
        }
    }

    // Los servicios devuelven {"error": ...} o {"message": ...}
    private String mensajeDeError(RestClientResponseException e) {
        try {
            Map<?, ?> cuerpo = e.getResponseBodyAs(Map.class);
            if (cuerpo != null) {
                Object mensaje = cuerpo.get("message") != null ? cuerpo.get("message") : cuerpo.get("error");
                if (mensaje != null) return mensaje.toString();
            }
        } catch (RuntimeException ignorada) {
            // cuerpo vacio o no JSON
        }
        return "Error " + e.getStatusCode().value() + " al comunicarse con el servicio";
    }

    private static <T> List<T> comoLista(T[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    @FunctionalInterface
    public interface ApiCall<T> {
        T execute(String accessToken);
    }

    private static class TokenRechazadoException extends RuntimeException {
    }
}
