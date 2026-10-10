package ar.edu.utn.frba.ddsi.donatrack_frontend.services;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.AuthResponseDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class AuthApiService {

    private static final Logger log = LoggerFactory.getLogger(AuthApiService.class);

    private final RestClient restClient = RestClient.create();
    private final String authServiceUrl;

    public AuthApiService(@Value("${auth.service.url}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }

    // null si las credenciales son incorrectas
    public AuthResponseDTO login(String username, String password) {
        try {
            return restClient.post().uri(authServiceUrl)
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .body(AuthResponseDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED) || e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
                return null;
            }
            log.error("Error del servicio de autenticación: {}", e.getMessage());
            throw new ServicioNoDisponibleException("Error en el servicio de autenticación", e);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo contactar al servicio de autenticación", e);
        }
    }
}
