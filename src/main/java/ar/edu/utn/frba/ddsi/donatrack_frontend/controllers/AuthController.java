package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email, @RequestParam String pass, HttpSession session, Model model) {

        // temporal: si ponemos el correo del mock, se loguea como el donante ID 1
        //aca vamos a poner un RestTemplate.postForEntity() que apunte al puerto del auth service
        // y de ahi guardamos con seesion.setAttribute("token",token)
        if ("ana@mail.com".equals(email) && "12345678".equals(pass)) {
            session.setAttribute("donanteId", 1L); // Guardamos el ID en sesión
            return "redirect:/donante/mis-donaciones";
        }

        // Si falla, volvemos a la vista de login con error
        model.addAttribute("error", true);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Borra la sesión
        return "redirect:/";
    }
}