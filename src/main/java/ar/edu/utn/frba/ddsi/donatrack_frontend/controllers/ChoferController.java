package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.RutaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

// Panel del chofer: ve las rutas de su camion y da inicio a las planificadas
@Controller
public class ChoferController {

    private final WebApiCallerService webApiCallerService;
    private final String logisticaServiceUrl;

    public ChoferController(WebApiCallerService webApiCallerService,
                            @Value("${logistica.service.url}") String logisticaServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.logisticaServiceUrl = logisticaServiceUrl;
    }

    @GetMapping("/chofer")
    public String misRutas(Model model) {
        List<RutaResponse> rutas = List.of();
        try {
            rutas = webApiCallerService.getList(logisticaServiceUrl + "/logistica/rutas/mias", RutaResponse[].class);
        } catch (ApiException | ServicioNoDisponibleException e) {
            model.addAttribute("error", "No pudimos cargar tus rutas: " + e.getMessage());
        }
        model.addAttribute("rutas", rutas);
        return "chofer/rutas";
    }

    @PostMapping("/chofer/rutas/{id}/iniciar")
    public String iniciarRuta(@PathVariable String id, RedirectAttributes redirect) {
        try {
            webApiCallerService.post(logisticaServiceUrl + "/logistica/rutas/" + id + "/iniciar", Map.of(), Map.class);
            redirect.addFlashAttribute("exito", "Ruta iniciada. Las entregas pasaron a \"En traslado\".");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo iniciar la ruta: " + e.getMessage());
        }
        return "redirect:/chofer";
    }
}
