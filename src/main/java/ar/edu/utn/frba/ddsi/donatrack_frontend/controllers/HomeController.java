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
import java.util.Map;

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
    public String misDonaciones(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String orden,
            HttpSession session,
            Model model) {

        Long donanteId = (Long) session.getAttribute("donanteId");
        if (donanteId == null) {
            return "redirect:/login"; // Si no hay sesión, lo echamos al login
        }

        RestTemplate restTemplate = new RestTemplate();
        StringBuilder backendUrl = new StringBuilder("http://localhost:8080/donaciones-service/donaciones/donantes/")
                .append(donanteId).append("?");

        if (estado != null && !estado.isEmpty()) backendUrl.append("estado=").append(estado).append("&");
        if (categoria != null && !categoria.isEmpty()) backendUrl.append("categoria=").append(categoria).append("&");
        if (busqueda != null && !busqueda.isEmpty()) backendUrl.append("busqueda=").append(busqueda).append("&");
        if (orden != null && !orden.isEmpty()) backendUrl.append("orden=").append(orden).append("&");

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    backendUrl.toString(), HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            model.addAttribute("donaciones", response.getBody());

        } catch (Exception e) {
            model.addAttribute("donaciones", java.util.List.of());
        }

        // Mantener el estado en la vista
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("categoriaFiltro", categoria);
        model.addAttribute("busquedaFiltro", busqueda);
        model.addAttribute("ordenFiltro", orden);

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
            @RequestParam String subcategoria,
            HttpSession session){

        Long donanteId = (Long) session.getAttribute("donanteId");
        if (donanteId == null) {
            return "redirect:/login";
        }

        //  Usamos los nuevos moldes (pasando false en los booleanos de Categoría para evitar el null)
        BienDTO.CategoriaRef catRef = new BienDTO.CategoriaRef(null, categoria, false, false);
        BienDTO.SubcategoriaRef subRef = new BienDTO.SubcategoriaRef(null, subcategoria);
        BienDTO.UnidadRef uniRef = new BienDTO.UnidadRef(null, unidad, "UN");

        //  Armamos el BienDTO
        BienDTO bien = new BienDTO(
                descripcionBien, catRef, subRef, uniRef, cantidad,
                null, false, false, 0.0, 0.0
        );

        //  Ensamblamos el Request
        DonacionCreateRequest request = new DonacionCreateRequest(
                descripcionDonacion,
                java.time.LocalDate.now(),
                java.util.List.of(bien)
        );

        //
        // Enviamos al backend
        RestTemplate restTemplate = new RestTemplate();
        String backendUrl = "http://localhost:8080/donaciones-service/donaciones/donantes/" + donanteId;

        try {
            restTemplate.postForEntity(backendUrl, request, String.class);
        } catch (Exception e) {
            System.out.println("Error al guardar en el backend: " + e.getMessage());
            return "redirect:/donante/nueva-donacion";
        }

        return "redirect:/donante/mis-donaciones";
    }
}