package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.RegistroForm;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.TipoRegistro;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.RegistroApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Registro en dos pasos: 1) elegir tipo de cuenta  2) completar el formulario de ese tipo
@Controller
@RequestMapping("/registro")
public class RegistroController {

    private static final Logger log = LoggerFactory.getLogger(RegistroController.class);

    private final RegistroApiService registroApiService;

    public RegistroController(RegistroApiService registroApiService) {
        this.registroApiService = registroApiService;
    }

    @GetMapping
    public String elegirTipo(Authentication authentication) {
        if (estaLogueado(authentication)) {
            return "redirect:/panel";
        }
        return "registro/elegir";
    }

    @GetMapping("/{slug}")
    public String formulario(@PathVariable String slug, Authentication authentication, Model model) {
        if (estaLogueado(authentication)) {
            return "redirect:/panel";
        }
        model.addAttribute("tipo", tipoDesde(slug));
        model.addAttribute("registro", new RegistroForm());
        return "registro/formulario";
    }

    @PostMapping("/{slug}")
    public String registrar(@PathVariable String slug,
                            @ModelAttribute("registro") RegistroForm form,
                            BindingResult errores,
                            Model model,
                            RedirectAttributes redirect) {
        TipoRegistro tipo = tipoDesde(slug);
        model.addAttribute("tipo", tipo);

        registroApiService.validar(tipo, form, errores);
        if (errores.hasErrors()) {
            return "registro/formulario";
        }

        try {
            registroApiService.registrar(tipo, form);
        } catch (ApiException e) {
            log.info("Registro rechazado ({}): {}", e.getStatus(), e.getMessage());
            // Los 4xx traen un mensaje pensado para el usuario (ej: mail repetido); los 5xx no
            model.addAttribute("errorGeneral", e.getStatus() < 500 ? e.getMessage()
                    : "No pudimos crear la cuenta en este momento. Intentá de nuevo en unos minutos.");
            return "registro/formulario";
        } catch (ServicioNoDisponibleException e) {
            model.addAttribute("errorGeneral", "El servicio no está disponible en este momento. Intentá de nuevo en unos minutos.");
            return "registro/formulario";
        }

        // Flash: el mail no viaja en la URL
        redirect.addFlashAttribute("mailRegistrado", form.getMail().trim());
        return "redirect:/login";
    }

    private static TipoRegistro tipoDesde(String slug) {
        return TipoRegistro.desdeSlug(slug).orElseThrow(() -> new NotFoundException("Tipo de cuenta inexistente: " + slug));
    }

    private static boolean estaLogueado(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
