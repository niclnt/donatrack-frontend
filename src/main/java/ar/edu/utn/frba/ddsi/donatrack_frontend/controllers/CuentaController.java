package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

// Cambio de contraseña del usuario logueado (cualquier rol). Obligatorio en el primer ingreso con contraseña temporal
@Controller
public class CuentaController {

    private static final int LARGO_MINIMO = 8;

    private final WebApiCallerService webApiCallerService;
    private final String authServiceUrl;

    public CuentaController(WebApiCallerService webApiCallerService,
                            @Value("${auth.service.url}") String authServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.authServiceUrl = authServiceUrl;
    }

    @GetMapping(SesionUsuario.URL_CAMBIO_CONTRASENIA)
    public String mostrarCambio(HttpSession session, Model model) {
        model.addAttribute("obligatorio", Boolean.TRUE.equals(session.getAttribute(SesionUsuario.DEBE_CAMBIAR_CONTRASENIA)));
        return "cuenta/contrasenia";
    }

    @PostMapping(SesionUsuario.URL_CAMBIO_CONTRASENIA)
    public String cambiar(@RequestParam(required = false) String actual,
                          @RequestParam(required = false) String nueva,
                          @RequestParam(required = false) String confirmacion,
                          HttpSession session, Model model, RedirectAttributes redirect) {
        // Lo que se puede chequear sin ir a auth (la contraseña actual la valida auth)
        String error = null;
        if (actual == null || actual.isBlank() || nueva == null || nueva.isBlank()) {
            error = "Completá la contraseña actual y la nueva.";
        } else if (nueva.length() < LARGO_MINIMO) {
            error = "La contraseña nueva tiene que tener al menos " + LARGO_MINIMO + " caracteres.";
        } else if (!nueva.equals(confirmacion)) {
            error = "La confirmación no coincide con la contraseña nueva.";
        }

        if (error == null) {
            try {
                webApiCallerService.put(authServiceUrl + "/usuarios/me/contrasenia",
                        Map.of("actual", actual, "nueva", nueva), Void.class);
            } catch (ApiException e) {
                error = e.getMessage();
            } catch (ServicioNoDisponibleException e) {
                error = "No pudimos cambiar la contraseña en este momento. Probá de nuevo en unos minutos.";
            }
        }

        if (error != null) {
            model.addAttribute("error", error);
            return mostrarCambio(session, model);
        }

        session.setAttribute(SesionUsuario.DEBE_CAMBIAR_CONTRASENIA, false);
        redirect.addFlashAttribute("exito", "Tu contraseña se cambió correctamente.");
        return "redirect:" + SesionUsuario.URL_CAMBIO_CONTRASENIA;
    }

    // Boton "Ir a mi panel" de la pantalla
    @org.springframework.web.bind.annotation.ModelAttribute("urlPanel")
    public String urlPanel(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) session.getAttribute(SesionUsuario.ROLES);
        return SesionUsuario.panelPara(roles != null ? roles : List.of());
    }
}
