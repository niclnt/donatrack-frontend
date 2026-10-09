package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.BienDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.DonacionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.DonacionResponse;
import lombok.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.List;
@Data
@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio(Model model) {
        // Inyectamos un dato dinámico en el "diccionario" de la vista
        model.addAttribute("mensaje", "¡El Cliente Liviano de DonaTrack está vivo!");

        // Retornamos el nombre exacto del archivo HTML (sin la extensión .html)
        return "index";
    }
    @GetMapping("/donante/mis-donaciones")
    public String misDonaciones(Model model) {
        //  Instancio el cliente HTTP de Spring
        RestTemplate restTemplate = new RestTemplate();

        // Defino la URL del backend real (ajustar dependiendo la ruta de la API)
        String backendUrl = "http://localhost:8080/donaciones-service/donaciones";

        try {
            //  Hacemos la peticion GET a la API y le decimos que nos devuelva una Lista de DonacionDTO
            ResponseEntity<List<DonacionResponse>> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DonacionResponse>>() {}
            );
            model.addAttribute("donaciones", response.getBody());

        } catch (Exception e) {
            // Si el backend esta apagado, mandamos un error a la vista para que no explote
            model.addAttribute("error", "No se pudo conectar con el Servicio de Donaciones.");
        }

        return "donante/mis-donaciones";
    }

    @GetMapping("/donante/nueva-donacion")
    public String mostrarFormularioDonacion(Model model) {
        // Más adelante acá le pasaremos un DTO vacío para que Thymeleaf lo llene
        return "donante/nueva-donacion";
    }
    @PostMapping("/donante/nueva-donacion")
    public String procesarNuevaDonacion(
            @RequestParam String descripcionDonacion,
            @RequestParam String descripcionBien,
            @RequestParam Integer cantidad,
            @RequestParam String unidad,
            @RequestParam String categoria,
            @RequestParam String subcategoria) {

        // 1. Usamos los nuevos moldes (pasando false en los booleanos de Categoría para evitar el null)
        BienDTO.CategoriaRef catRef = new BienDTO.CategoriaRef(null, categoria, false, false);
        BienDTO.SubcategoriaRef subRef = new BienDTO.SubcategoriaRef(null, subcategoria);
        BienDTO.UnidadRef uniRef = new BienDTO.UnidadRef(null, unidad, "UN");

        // 2. Armamos el BienDTO
        BienDTO bien = new BienDTO(
                descripcionBien, catRef, subRef, uniRef, cantidad,
                null, false, false, 0.0, 0.0
        );

        // 3. Ensamblamos el Request
        DonacionCreateRequest request = new DonacionCreateRequest(
                descripcionDonacion,
                java.time.LocalDate.now(),
                java.util.List.of(bien)
        );

        // 4. Enviamos al backend
        RestTemplate restTemplate = new RestTemplate();
        String backendUrl = "http://localhost:8080/donaciones-service/donaciones/donantes/1";

        try {
            restTemplate.postForEntity(backendUrl, request, String.class);
        } catch (Exception e) {
            System.out.println("Error al guardar en el backend: " + e.getMessage());
            return "redirect:/donante/nueva-donacion";
        }

        return "redirect:/donante/mis-donaciones";
    }
}