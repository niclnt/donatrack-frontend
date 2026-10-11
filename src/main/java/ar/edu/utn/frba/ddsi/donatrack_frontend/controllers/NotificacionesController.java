package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.NotificacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;

@Controller
public class NotificacionesController {

  private final WebApiCallerService webApiCallerService;
  private final String notificacionesServiceBaseUrl;

  public NotificacionesController(
      WebApiCallerService webApiCallerService,
      @Value("${notificaciones.service.url}") String notificacionesServiceBaseUrl) {
    this.webApiCallerService = webApiCallerService;
    this.notificacionesServiceBaseUrl = notificacionesServiceBaseUrl;
  }

  @GetMapping("/donante/notificaciones")
  public String notificaciones(Model model) {
    return cargarNotificaciones(model, "donante/notificaciones");
  }

  @GetMapping("/ong/notificaciones")
  public String notificacionesEntidad(Model model) {
    return cargarNotificaciones(model, "ong/notificaciones");
  }

  private String cargarNotificaciones(Model model, String vista) {
    Long usuarioId = (Long) SesionUsuario.actual().getAttribute(SesionUsuario.USUARIO_ID);

    if(usuarioId == null){
      return "redirect:/login?expired";
    }

    String backendUrl = notificacionesServiceBaseUrl + "/api/notificaciones/destinatarios/" + usuarioId;
    try {
      List<NotificacionResponse> notificaciones = webApiCallerService.getList(backendUrl, NotificacionResponse[].class);
      model.addAttribute("notificaciones", notificaciones);
    } catch (RestClientException e) {
      model.addAttribute("error", "No se pudo conectar con el Servicio de Notificaciones. Intenta de nuevo mas tarde.");
    }

    return vista;
  }
}
