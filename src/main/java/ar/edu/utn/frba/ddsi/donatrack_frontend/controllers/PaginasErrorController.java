package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginasErrorController {

    @GetMapping("/403")
    public String accesoDenegado(Model model) {
        model.addAttribute("titulo", "Acceso denegado");
        model.addAttribute("mensaje", "No tenés permiso para acceder a esta sección.");
        return "403";
    }

    @GetMapping("/404")
    public String noEncontrado(Model model) {
        model.addAttribute("titulo", "No encontrado");
        return "404";
    }
}
