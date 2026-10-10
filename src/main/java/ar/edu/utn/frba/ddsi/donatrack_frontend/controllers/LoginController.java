package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        // Si ya tiene sesion, no tiene sentido mostrarle el login
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/panel";
        }
        return "login";
    }

    // Lleva a cada usuario a la pantalla de inicio de su rol
    @GetMapping("/panel")
    public String panel(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        return "redirect:" + SesionUsuario.panelPara(roles);
    }
}
