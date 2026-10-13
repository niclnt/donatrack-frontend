package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo.CategoriaCatalogoResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.BienDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import java.util.List;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Data
@Controller
public class HomeController {

    // Manda el token de la sesion en cada llamada (y lo refresca si vencio)
    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;

    public HomeController(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
    }

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    // REQ. 6 — Lista de entidades beneficiarias visible para el donante.
    // Llama a GET /donaciones/entidades-beneficiarias que devuelve todas las entidades
    // con sus necesidades ya embebidas en la respuesta.
    @GetMapping("/donante/entidades")
    public String entidades(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        List<EntidadBeneficiariaResponse> entidades;
        try {
            // El backend devuelve la lista completa; cada EntidadBeneficiariaResponse
            // trae 'necesidades' embebidas (List<NecesidadResponse>) con categoria,
            // cantidadSolicitada, cantidadRecibida, satisfecha, etc.
            entidades = webApiCallerService.getList(
                donacionesServiceUrl + "/donaciones/entidades-beneficiarias",
                EntidadBeneficiariaResponse[].class
            );
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            entidades = java.util.List.of();
            model.addAttribute("errorEntidades", "No se pudieron cargar las entidades. Intentá más tarde.");
        }

        model.addAttribute("entidades", entidades);
        return "donante/entidades";
    }


    @GetMapping("/donante/mis-donaciones")
    public String misDonaciones(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String orden,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Long donanteId = extraerDonanteId();

        StringBuilder backendUrl = new StringBuilder(donacionesServiceUrl + "/donaciones-service/donaciones/donantes/")
                .append(donanteId).append("?");

        if (estado != null && !estado.isEmpty()) backendUrl.append("estado=").append(estado).append("&");
        if (categoria != null && !categoria.isEmpty()) backendUrl.append("categoria=").append(categoria).append("&");
        if (busqueda != null && !busqueda.isEmpty()) backendUrl.append("busqueda=").append(busqueda).append("&");
        if (orden != null && !orden.isEmpty()) backendUrl.append("orden=").append(orden).append("&");

        // Si la sesion vencio, SesionExpiradaException no se atrapa aca: ErroresControllerAdvice manda al login
        try {
            model.addAttribute("donaciones", webApiCallerService.getList(backendUrl.toString(), Map[].class));
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            model.addAttribute("donaciones", java.util.List.of());
        }

        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("categoriaFiltro", categoria);
        model.addAttribute("busquedaFiltro", busqueda);
        model.addAttribute("ordenFiltro", orden);

        return "donante/mis-donaciones";
    }

    @GetMapping("/donante/nueva-donacion")
    public String mostrarFormularioDonacion(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        List<CategoriaCatalogoResponse> categorias;

        try {
            categorias = webApiCallerService.getList(
                donacionesServiceUrl + "/donaciones/catalogo/categorias",
                CategoriaCatalogoResponse[].class
            );
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            categorias = List.of();
            model.addAttribute("errorCatalogo", "No se pudieron cargar las categorías. Intentá nuevamente más tarde.");
        }

        model.addAttribute("categorias", categorias);

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
            Authentication authentication){

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Long donanteId = extraerDonanteId();

        BienDTO.CategoriaRef catRef = new BienDTO.CategoriaRef(null, categoria, false, false);
        BienDTO.SubcategoriaRef subRef = new BienDTO.SubcategoriaRef(null, subcategoria);
        BienDTO.UnidadRef uniRef = new BienDTO.UnidadRef(null, unidad, "UN");

        BienDTO bien = new BienDTO(
                descripcionBien, catRef, subRef, uniRef, cantidad,
                null, false, false, 0.0, 0.0
        );

        DonacionCreateRequest request = new DonacionCreateRequest(
                descripcionDonacion,
                java.time.LocalDate.now(),
                java.util.List.of(bien)
        );

        String backendUrl = donacionesServiceUrl + "/donaciones-service/donaciones/donantes/" + donanteId;

        try {
            webApiCallerService.post(backendUrl, request, DonacionResponse.class);
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            System.out.println("Error al guardar en el backend: " + e.getMessage());
            return "redirect:/donante/nueva-donacion";
        }

        return "redirect:/donante/mis-donaciones";
    }

    // Usamos la clase estática de tu compañero para leer el ID directamente de la sesión
    private Long extraerId() {
        try {
            Object idObjeto = SesionUsuario.actual().getAttribute(SesionUsuario.USUARIO_ID);
            if (idObjeto != null) {
                return Long.valueOf(idObjeto.toString());
            }
        } catch (Exception e) {
            System.out.println("Error al extraer ID de SesionUsuario: " + e.getMessage());
        }
        return 1L; // Fallback de seguridad
    }

    private Long extraerDonanteId() {
        Long donanteId = SesionUsuario.donanteId();

        if (donanteId == null) {
            throw new IllegalStateException(
                "No hay un donante autenticado en la sesión"
            );
        }

        return donanteId;
    }
}