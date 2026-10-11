package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo.CategoriaCatalogoResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionSegmentadaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades.NecesidadCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    EntidadBeneficiariaResponse entidad = obtenerEntidadDelUsuario(usuarioId);

    List<DonacionSegmentadaResponse> donaciones =
        webApiCallerService.getList(
            donacionesServiceUrl+"/donaciones-service/donaciones/segmentadas/entidad/"+entidad.id(),
            DonacionSegmentadaResponse[].class
        );

    model.addAttribute("donaciones", donaciones);
    return "ong/asignadas";
  }

  @GetMapping("ong/necesidades")
  public String necesidades(Model model){
    Long usuarioId = SesionUsuario.usuarioId();

    if( usuarioId == null){
      return "redirect:/login?expired";
    }

    EntidadBeneficiariaResponse entidad = obtenerEntidadDelUsuario(usuarioId);

    List<NecesidadResponse> necesidades = webApiCallerService.getList(
        donacionesServiceUrl+ "/donaciones/necesidades/entidad/"+entidad.id(),
        NecesidadResponse[].class
    );

    List<CategoriaCatalogoResponse> categorias =
        webApiCallerService.getList(
            donacionesServiceUrl
                + "/donaciones/catalogo/categorias",
            CategoriaCatalogoResponse[].class
        );

    model.addAttribute("necesidades", necesidades);
    model.addAttribute("categorias", categorias);
    model.addAttribute("entidadId", entidad.id());

    return "ong/necesidades";
  }

  @PostMapping("ong/necesidades")
  public String crearNecesidad(
      @RequestParam String categoriaNombre,
      @RequestParam String subcategoriaNombre,
      @RequestParam int cantidadSolicitada,
      @RequestParam String tipoNecesidad,
      @RequestParam(required = false) Integer cantidadDias,
      @RequestParam(required = false) String situacion,
      @RequestParam String descripcion,
      RedirectAttributes redirectAttributes
  ){
    Long usuarioId = SesionUsuario.usuarioId();

    if (usuarioId == null) {
      return "redirect:/login?expired";
    }

    EntidadBeneficiariaResponse entidad =
        obtenerEntidadDelUsuario(usuarioId);

    NecesidadCreateRequest request =
        new NecesidadCreateRequest(
            categoriaNombre,
            subcategoriaNombre,
            entidad.id(),
            descripcion,
            cantidadSolicitada,
            tipoNecesidad,
            cantidadDias,
            situacion
        );

    webApiCallerService.post(
        donacionesServiceUrl + "/donaciones/necesidades",
        request,
        NecesidadResponse.class
        );

    redirectAttributes.addFlashAttribute(
        "mensajeExito",
        "La necesidad fue registrada correctamente"
    );

    return "redirect:/ong/necesidades";
  }

  private EntidadBeneficiariaResponse obtenerEntidadDelUsuario(Long usuarioId) {
    return webApiCallerService.get(
        donacionesServiceUrl + "/donaciones/entidades-beneficiarias/usuario/" + usuarioId,
        EntidadBeneficiariaResponse.class
    );
  }
}

