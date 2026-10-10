package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.SesionExpiradaException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Errores de los servicios que ningun controller manejo
@ControllerAdvice
public class ErroresControllerAdvice {

    @ExceptionHandler(SesionExpiradaException.class)
    public String sesionExpirada() {
        return "redirect:/login?expired";
    }

    @ExceptionHandler(NotFoundException.class)
    public String noEncontrado() {
        return "redirect:/404";
    }

    @ExceptionHandler({ServicioNoDisponibleException.class, ApiException.class})
    public String errorDeServicio(RuntimeException e, Model model) {
        model.addAttribute("titulo", "Algo salió mal");
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }
}
