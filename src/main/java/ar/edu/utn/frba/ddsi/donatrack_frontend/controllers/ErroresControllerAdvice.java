package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.SesionExpiradaException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    // Se lanza antes de llegar al controller; el unico upload es la importacion CSV
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoDemasiadoGrande() {
        return "redirect:/admin/importar-csv?archivoGrande";
    }

    @ExceptionHandler({ServicioNoDisponibleException.class, ApiException.class})
    public String errorDeServicio(RuntimeException e, Model model) {
        model.addAttribute("titulo", "Algo salió mal");
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }
}
