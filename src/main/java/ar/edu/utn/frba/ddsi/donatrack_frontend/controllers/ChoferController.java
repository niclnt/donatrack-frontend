package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.RutaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Panel del chofer: ve las rutas de su camion, da inicio a las planificadas, reporta su posicion
// (GPS del celular o simulador del recorrido) y registra las entregas que no se pudieron realizar.
@Controller
public class ChoferController {

    private static final int MAX_MOTIVO = 500;

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
            redirect.addFlashAttribute("exito", "Ruta iniciada. Las entregas pasaron a \"En traslado\" y se avisó a "
                    + "donantes y entidades con el enlace al mapa en vivo.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo iniciar la ruta: " + e.getMessage());
        }
        return "redirect:/chofer";
    }

    // La entrega no se pudo realizar en la parada (sede cerrada, nadie responde...)
    @PostMapping("/chofer/rutas/{rutaId}/entregas/{entregaId}/no-entregada")
    public String registrarEntregaFallida(@PathVariable String rutaId,
                                          @PathVariable String entregaId,
                                          @RequestParam(required = false) String motivo,
                                          RedirectAttributes redirect) {
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) {
            redirect.addFlashAttribute("error", "Indicá el motivo por el que no se pudo realizar la entrega.");
            redirect.addFlashAttribute("entregaConError", entregaId);
            return "redirect:/chofer";
        }
        if (motivoLimpio.length() > MAX_MOTIVO) {
            motivoLimpio = motivoLimpio.substring(0, MAX_MOTIVO);
        }
        try {
            webApiCallerService.post(
                    logisticaServiceUrl + "/logistica/rutas/" + rutaId + "/entregas/" + entregaId + "/no-entregada",
                    Map.of("motivo", motivoLimpio), Map.class);
            redirect.addFlashAttribute("exito", "Registramos que la entrega no se pudo realizar. Se avisó a la "
                    + "entidad, a la persona donante y a administración; la donación vuelve al depósito.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo registrar la entrega fallida: " + e.getMessage());
        }
        return "redirect:/chofer";
    }

    // Posicion del camion, enviada por el navegador del chofer (GPS real o simulador).
    // El navegador no tiene el token: el front la reenvia a logistica con el token de la sesion.
    @PostMapping("/chofer/rutas/{rutaId}/posiciones")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reportarPosicion(@PathVariable String rutaId,
                                                                @RequestBody Map<String, Object> posicion) {
        Map<String, Object> body = new HashMap<>();
        body.put("latitud", posicion.get("latitud"));
        body.put("longitud", posicion.get("longitud"));
        body.put("velocidad", posicion.get("velocidad"));
        try {
            webApiCallerService.post(logisticaServiceUrl + "/logistica/monitoreo/rutas/" + rutaId + "/posiciones",
                    body, Map.class);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ok", true));
        } catch (ApiException e) {
            // 409: la ruta ya no esta en curso (por ejemplo, se confirmaron todas las entregas)
            return ResponseEntity.status(e.getStatus()).body(Map.of("ok", false, "error", e.getMessage()));
        } catch (ServicioNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("ok", false, "error", "Logística no está disponible en este momento."));
        }
    }
}
