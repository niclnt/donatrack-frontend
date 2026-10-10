package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.NotificacionResponse;
import java.util.List;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Controller
public class NotificacionesController {

  private final RestTemplate restTemplate;
  private final String notificacionesServiceBaseUrl;

  public NotificacionesController(
      RestTemplate restTemplate,
      @Value("${notificaciones.service.url}") String notificacionesServiceBaseUrl) {
    this.restTemplate = restTemplate;
    this.notificacionesServiceBaseUrl = notificacionesServiceBaseUrl;
  }

  @GetMapping("/donante/notificaciones")
  public String notificaciones(Model model) {
    String backendUrl = notificacionesServiceBaseUrl + "/api/notificaciones/destinatarios/21"; //TODO: sacar hardcodeo de la id
    try {
      ResponseEntity<List<NotificacionResponse>> response = restTemplate.exchange(
          backendUrl,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<List<NotificacionResponse>>() {}
      );
      model.addAttribute("notificaciones", response.getBody());
    } catch (RestClientException e) {
      model.addAttribute("error", "No se pudo conectar con el Servicio de Notificaciones. Intenta de nuevo mas tarde.");
    }
    return "donante/notificaciones";
  }
}
