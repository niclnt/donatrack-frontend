package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// La persona juridica logueada gestiona las personas habilitadas a donar en su nombre
@Controller
@RequestMapping("/donante/representantes")
public class RepresentantesController {

    private final WebApiCallerService webApiCallerService;
    private final String representantesUrl;

    public RepresentantesController(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.representantesUrl = donacionesServiceUrl + "/donantes/personaJuridica/mia/representantes";
    }

    @GetMapping
    public String verRepresentantes(Model model) {
        try {
            model.addAttribute("representantes", webApiCallerService.getList(representantesUrl, RepresentanteResponse[].class));
        } catch (ApiException | ServicioNoDisponibleException e) {
            model.addAttribute("representantes", List.of());
            model.addAttribute("error", "No se pudieron cargar los representantes: " + e.getMessage());
        }
        return "donante/representantes";
    }

    @PostMapping
    public String agregarRepresentante(@ModelAttribute RepresentanteRequest request, RedirectAttributes redirect) {
        try {
            webApiCallerService.post(representantesUrl, request, RepresentanteResponse[].class);
            redirect.addFlashAttribute("exito", "Representante agregado correctamente.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/donante/representantes";
    }

    @PostMapping("/{id}/quitar")
    public String quitarRepresentante(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            webApiCallerService.delete(representantesUrl + "/" + id);
            redirect.addFlashAttribute("exito", "Representante quitado.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/donante/representantes";
    }
}
