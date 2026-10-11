package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionSegmentadaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EntidadBeneficiariaController {
  private final WebApiCallerService webApiCallerService;
  private final String donacionesServiceUrl;

  public EntidadBeneficiariaController(
      WebApiCallerService webApiCallerService,
      @Value("${donaciones.service.url}") String donacionesServiceUrl) {
    this.webApiCallerService = webApiCallerService;
    this.donacionesServiceUrl = donacionesServiceUrl;
  }

  @GetMapping("/ong/asignadas")
  public String asignadas(Model model) {
    Long usuarioId = SesionUsuario.usuarioId();
    if (usuarioId == null) {
      return "redirect:/login?expired";
    }

    EntidadBeneficiariaResponse entidad =
        webApiCallerService.get(
            donacionesServiceUrl
                + "/donaciones/entidades-beneficiarias/usuario/"
                + usuarioId,
            EntidadBeneficiariaResponse.class
        );

    List<DonacionSegmentadaResponse> donaciones =
        webApiCallerService.getList(
            donacionesServiceUrl
                + "/donaciones-service/donaciones/segmentadas/entidad/"
                + entidad.id(),
            DonacionSegmentadaResponse[].class
        );

    model.addAttribute("donaciones", donaciones);
    return "ong/asignadas";
  }
}
