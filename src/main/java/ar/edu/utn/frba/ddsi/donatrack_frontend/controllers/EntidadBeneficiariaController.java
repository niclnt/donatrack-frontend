package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo.CategoriaCatalogoResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionSegmentadaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades.NecesidadCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.EntregaARecibir;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.EntregaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.SeguimientoEntregaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EntidadBeneficiariaController {
  private final WebApiCallerService webApiCallerService;
  private final String donacionesServiceUrl;
  private final String logisticaServiceUrl;

  public EntidadBeneficiariaController(
      WebApiCallerService webApiCallerService,
      @Value("${donaciones.service.url}") String donacionesServiceUrl,
      @Value("${logistica.service.url}") String logisticaServiceUrl) {
    this.webApiCallerService = webApiCallerService;
    this.donacionesServiceUrl = donacionesServiceUrl;
    this.logisticaServiceUrl = logisticaServiceUrl;
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

    List<CategoriaCatalogoResponse> categorias;
    try {
      categorias = webApiCallerService.getList(
          donacionesServiceUrl + "/donaciones/catalogo/categorias",
          CategoriaCatalogoResponse[].class
      );
    } catch (ApiException | ServicioNoDisponibleException e) {
      categorias = List.of();
    }

    model.addAttribute("donaciones", donaciones);
    model.addAttribute("categorias", categorias);
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

  // REQ. 12 — Confirmar recepcion: entregas en camino (EN_TRASLADO) y ya recibidas (ENTREGADA) de la entidad
  @GetMapping("/ong/confirmar-recepcion")
  public String confirmarRecepcion(Model model) {
    Long usuarioId = SesionUsuario.usuarioId();
    if (usuarioId == null) {
      return "redirect:/login?expired";
    }

    List<EntregaARecibir> enCamino = new ArrayList<>();
    List<EntregaARecibir> recibidas = new ArrayList<>();
    try {
      EntidadBeneficiariaResponse entidad = obtenerEntidadDelUsuario(usuarioId);
      List<SeguimientoEntregaResponse> seguimiento = webApiCallerService.getList(
          logisticaServiceUrl + "/logistica/monitoreo/beneficiarios/" + entidad.id() + "/seguimiento",
          SeguimientoEntregaResponse[].class);

      for (SeguimientoEntregaResponse s : seguimiento) {
        EntregaResponse entrega = webApiCallerService.get(
            logisticaServiceUrl + "/logistica/trazabilidad/entregas/" + s.entregaId(), EntregaResponse.class);
        if (entrega.enTraslado()) {
          enCamino.add(new EntregaARecibir(s.patente(), entrega));
        } else if (entrega.entregada()) {
          recibidas.add(new EntregaARecibir(s.patente(), entrega));
        }
      }
    } catch (ApiException | ServicioNoDisponibleException e) {
      model.addAttribute("error", "No se pudieron cargar tus entregas: " + e.getMessage());
    }

    model.addAttribute("enCamino", enCamino);
    model.addAttribute("recibidas", recibidas);
    return "ong/confirmar-recepcion";
  }

  // Confirma la recepcion (si todavia estaba en camino) y sube las fotos. Si ya estaba confirmada, solo agrega fotos.
  @PostMapping("/ong/confirmar-recepcion/{entregaId}")
  public String procesarRecepcion(
      @PathVariable String entregaId,
      @RequestParam(name = "fotos", required = false) List<MultipartFile> fotos,
      RedirectAttributes redirect) {
    Long usuarioId = SesionUsuario.usuarioId();
    if (usuarioId == null) {
      return "redirect:/login?expired";
    }

    List<MultipartFile> imagenes = fotos == null ? List.of() : fotos.stream().filter(f -> !f.isEmpty()).toList();
    if (imagenes.isEmpty()) {
      redirect.addFlashAttribute("error", "Adjuntá al menos una foto de la donación recibida.");
      return "redirect:/ong/confirmar-recepcion";
    }
    if (imagenes.stream().anyMatch(f -> f.getContentType() == null || !f.getContentType().startsWith("image/"))) {
      redirect.addFlashAttribute("error", "Solo se pueden adjuntar imágenes.");
      return "redirect:/ong/confirmar-recepcion";
    }

    String urlEntrega = logisticaServiceUrl + "/logistica/trazabilidad/entregas/" + entregaId;
    boolean confirmadaAhora = false;
    try {
      EntidadBeneficiariaResponse entidad = obtenerEntidadDelUsuario(usuarioId);
      Map<String, String> encabezados = Map.of("X-Entidad-Id", entidad.id().toString());

      EntregaResponse entrega = webApiCallerService.get(urlEntrega, EntregaResponse.class);
      if (entrega.enTraslado()) {
        webApiCallerService.post(urlEntrega + "/confirmar", null, EntregaResponse.class, encabezados);
        confirmadaAhora = true;
      }

      webApiCallerService.postMultipart(urlEntrega + "/fotos", comoPartes(imagenes), EntregaResponse.class, encabezados);
      redirect.addFlashAttribute("exito", confirmadaAhora
          ? "Recepción confirmada. Le avisamos al donante que su donación llegó."
          : "Fotos agregadas a la entrega.");
    } catch (ApiException | ServicioNoDisponibleException e) {
      redirect.addFlashAttribute("error", confirmadaAhora
          ? "La recepción quedó confirmada, pero no se pudieron subir las fotos: " + e.getMessage() + ". Volvé a intentarlo desde \"Entregas recibidas\"."
          : "No se pudo confirmar la recepción: " + e.getMessage());
    }
    return "redirect:/ong/confirmar-recepcion";
  }

  // Cada foto viaja con su nombre y Content-Type (logistica rechaza lo que no sea image/*)
  private static MultiValueMap<String, Object> comoPartes(List<MultipartFile> imagenes) {
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    for (MultipartFile imagen : imagenes) {
      try {
        HttpHeaders encabezado = new HttpHeaders();
        encabezado.setContentType(MediaType.parseMediaType(imagen.getContentType()));
        partes.add("fotos", new HttpEntity<>(new ByteArrayResource(imagen.getBytes()) {
          @Override
          public String getFilename() {
            return imagen.getOriginalFilename();
          }
        }, encabezado));
      } catch (IOException e) {
        throw new UncheckedIOException("No se pudo leer la foto " + imagen.getOriginalFilename(), e);
      }
    }
    return partes;
  }

  private EntidadBeneficiariaResponse obtenerEntidadDelUsuario(Long usuarioId) {
    return webApiCallerService.get(
        donacionesServiceUrl + "/donaciones/entidades-beneficiarias/usuario/" + usuarioId,
        EntidadBeneficiariaResponse.class
    );
  }
}
