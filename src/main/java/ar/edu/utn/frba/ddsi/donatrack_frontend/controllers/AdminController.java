package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.BienDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private final WebApiCallerService webApiCallerService;

    public AdminController(WebApiCallerService webApiCallerService) {
        this.webApiCallerService = webApiCallerService;
    }

    // --- REQ 15: REGISTRAR DONACIÓN ---

    @GetMapping("/admin/registrar-donacion")
    public String mostrarRegistroDonacion(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            // CORRECCIÓN: URL correcta del CatalogoController de tus compañeros
            String categoriasUrl = "http://localhost:8080/donaciones/catalogo/categorias";
            model.addAttribute("categorias", webApiCallerService.getList(categoriasUrl, Map[].class));
        } catch (Exception e) {
            model.addAttribute("categorias", List.of());
            System.out.println("Error cargando categorías: " + e.getMessage());
        }

        return "admin/registrar-donacion";
    }

    @PostMapping("/admin/registrar-donacion")
    public String procesarRegistroDonacion(
            @RequestParam Long donanteId,
            @RequestParam String categoria,
            @RequestParam String subcategoria,
            @RequestParam Integer cantidad,
            @RequestParam String unidad,
            @RequestParam String estado,
            @RequestParam(required = false) String vencimiento,
            @RequestParam String descripcion,
            @RequestParam String responsable,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        BienDTO.CategoriaRef catRef = new BienDTO.CategoriaRef(null, categoria, false, false);
        BienDTO.SubcategoriaRef subRef = new BienDTO.SubcategoriaRef(null, subcategoria);
        BienDTO.UnidadRef uniRef = new BienDTO.UnidadRef(null, unidad, "UN");
        LocalDate fechaVenc = (vencimiento != null && !vencimiento.isBlank()) ? LocalDate.parse(vencimiento) : null;
        boolean usado = "Usado en buen estado".equals(estado);

        BienDTO bien = new BienDTO(descripcion, catRef, subRef, uniRef, cantidad, fechaVenc, usado, false, 0.0, 0.0);
        DonacionCreateRequest request = new DonacionCreateRequest(descripcion + " (Recepción por: " + responsable + ")", LocalDate.now(), List.of(bien));

        String backendUrl = "http://localhost:8080/donaciones-service/donaciones/donantes/" + donanteId;

        try {
            webApiCallerService.post(backendUrl, request, String.class);
            model.addAttribute("exito", "Donación registrada correctamente en depósito.");
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar la donación: " + e.getMessage());
        }

        try {
            model.addAttribute("categorias", webApiCallerService.getList("http://localhost:8080/donaciones/catalogo/categorias", Map[].class));
        } catch (Exception ignored) {}

        return "admin/registrar-donacion";
    }

    // --- REQ 16: DEPÓSITO Y VENCIDAS ---

    @GetMapping("/admin/deposito-vencidas")
    public String mostrarDeposito(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            String url = "http://localhost:8080/donaciones-service/donaciones?estado=EN_DEPOSITO";
            model.addAttribute("donaciones", webApiCallerService.getList(url, Map[].class));
        } catch (Exception e) {
            model.addAttribute("donaciones", List.of());
        }

        return "admin/deposito-vencidas";
    }
    @PostMapping("/admin/marcar-vencidas")
    public String procesarVencidas(
            @RequestParam(required = false) List<String> segmentosIds,
            @RequestParam String motivo,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        if (segmentosIds == null || segmentosIds.isEmpty()) {
            model.addAttribute("error", "Debes seleccionar al menos un bien para marcarlo como vencido.");
            return mostrarDeposito(authentication, model);
        }

        // Recorremos los seleccionados y enviamos la orden autenticada
        for (String parIds : segmentosIds) {
            String[] ids = parIds.split("-");
            String donacionId = ids[0];
            String segmentadaId = ids[1];

            String url = "http://localhost:8080/donaciones-service/donaciones/" + donacionId + "/segmentadas/" + segmentadaId + "/estado";

            Map<String, String> requestBody = Map.of(
                    "nuevoEstado", "VENCIDA",
                    "motivo", motivo,
                    "responsable",authentication.getName()
            );

            try {
                // Usamos el caller autenticado en lugar del RestTemplate crudo
                webApiCallerService.put(url, requestBody, String.class);
            } catch (Exception e) {
                System.out.println("Error al marcar vencida: " + e.getMessage());
            }
        }

        return "redirect:/admin/deposito-vencidas?exito=true";
    }

    // --- REQ 17: ASIGNACIÓN POR ALGORITMO ---

    @GetMapping("/admin/asignar-algoritmo")
    public String mostrarAsignacion(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            // Buscamos el Top 10 de sugerencias del motor de matchmaking
            String url = "http://localhost:8080/api/asignaciones/rankings/top";
            model.addAttribute("propuestas", webApiCallerService.getList(url, Map[].class));
        } catch (Exception e) {
            model.addAttribute("propuestas", List.of());
            model.addAttribute("error", "No se pudieron cargar las sugerencias del algoritmo: " + e.getMessage());
        }

        return "admin/asignar-algoritmo";
    }

    @PostMapping("/admin/forzar-algoritmo")
    public String forzarAlgoritmo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            webApiCallerService.post("http://localhost:8080/api/asignaciones/forzar-ejecucion", null, String.class);
        } catch (Exception ignored) {}

        return "redirect:/admin/asignar-algoritmo?actualizado=true";
    }

    @PostMapping("/admin/confirmar-asignacion")
    public String confirmarAsignacion(
            @RequestParam Long idDonacionSegmentada,
            @RequestParam Long idNecesidad,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        String url = "http://localhost:8080/api/asignaciones/" + idDonacionSegmentada + "/confirmar/" + idNecesidad + "?responsable=" + authentication.getName();

        try {
            webApiCallerService.post(url, null, String.class);
        } catch (Exception e) {
            System.out.println("Error al confirmar asignación: " + e.getMessage());
        }

        return "redirect:/admin/asignar-algoritmo?exito=true";
    }

    // --- REQ 18: ADMINISTRAR CAMIONES ---

    @GetMapping("/admin/camiones")
    public String mostrarCamiones(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            // Se usa el puerto 8083 para logistica-service
            String url = "http://localhost:8083/logistica/camiones";
            model.addAttribute("camiones", webApiCallerService.getList(url, Map[].class));
        } catch (Exception e) {
            model.addAttribute("camiones", List.of());
            System.out.println("Error cargando camiones: " + e.getMessage());
        }

        return "admin/camiones";
    }

    @PostMapping("/admin/camiones")
    public String registrarCamion(
            @RequestParam String patente,
            @RequestParam String estado,
            @RequestParam Double volumen,
            @RequestParam Double altura,
            @RequestParam Double carga,
            @RequestParam(required = false) String chofer,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        // Mapeo adaptado al CamionCreateRequest del backend
        Map<String, Object> requestBody = Map.of(
                "patente", patente,
                "estado", estado,
                "volumenM3", volumen,
                "alturaMetros", altura,
                "capacidadPesoKg", carga,
                "choferAsignado", chofer != null ? chofer : ""
        );

        try {
            String url = "http://localhost:8083/logistica/camiones";
            webApiCallerService.post(url, requestBody, String.class);
        } catch (Exception e) {
            System.out.println("Error al registrar camión: " + e.getMessage());
        }

        return "redirect:/admin/camiones?exito=true";
    }

}