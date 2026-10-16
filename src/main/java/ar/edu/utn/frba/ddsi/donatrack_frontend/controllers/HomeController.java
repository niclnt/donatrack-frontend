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
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.MapaDonacionesApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Data
@Controller
public class HomeController {

    // Manda el token de la sesion en cada llamada (y lo refresca si vencio)
    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;
    private final MapaDonacionesApiService mapaDonacionesApiService;

    public HomeController(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl,
            MapaDonacionesApiService mapaDonacionesApiService) {
        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
        this.mapaDonacionesApiService = mapaDonacionesApiService;
    }

    @GetMapping("/")
    public String inicio(Authentication authentication) {
        // Con sesion, cada rol va a su panel (/panel decide segun el rol)
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/panel";
        }
        return "index";
    }

    // REQ. 6 — Lista de entidades beneficiarias visible para el donante.
    @GetMapping("/donante/entidades")
    public String entidades(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        List<EntidadBeneficiariaResponse> entidades;
        try {
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
            @RequestParam(required = false) String subcategoria,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String orden,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Long donanteId;
        try {
            donanteId = extraerDonanteId();
        } catch (IllegalStateException e) {
            model.addAttribute("donaciones", java.util.List.of());
            model.addAttribute("error", "No se encontró un perfil de donante asociado a tu cuenta. Volvé a iniciar sesión.");
            return "donante/mis-donaciones";
        }

        StringBuilder backendUrl = new StringBuilder(donacionesServiceUrl + "/donaciones-service/donaciones/donantes/")
                .append(donanteId).append("?");

        if (estado != null && !estado.isEmpty()) backendUrl.append("estado=").append(estado).append("&");
        if (categoria != null && !categoria.isEmpty()) backendUrl.append("categoria=").append(categoria).append("&");
        if (subcategoria != null && !subcategoria.isEmpty()) backendUrl.append("subcategoria=").append(subcategoria).append("&");
        if (busqueda != null && !busqueda.isEmpty()) backendUrl.append("busqueda=").append(busqueda).append("&");
        if (orden != null && !orden.isEmpty()) backendUrl.append("orden=").append(orden).append("&");

        try {
            model.addAttribute("donaciones", webApiCallerService.getList(backendUrl.toString(), Map[].class));
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            model.addAttribute("donaciones", java.util.List.of());
            model.addAttribute("error", "No se pudieron cargar tus donaciones en este momento.");
        }

        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("categoriaFiltro", categoria);
        model.addAttribute("subcategoriaFiltro", subcategoria);
        model.addAttribute("busquedaFiltro", busqueda);
        model.addAttribute("ordenFiltro", orden);

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

    @GetMapping("/legales")
    public String mostrarLegales() {
        return "legales";
    }

    @GetMapping("/privacidad")
    public String mostrarPrivacidad() {
        return "privacidad";
    }

    @GetMapping("/mapa-donaciones")
    public String mostrarMapaDonaciones(Model model) {
        try {
            model.addAttribute("donacionesEntregadas",
                    mapaDonacionesApiService.obtenerDonacionesEntregadas());

        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            model.addAttribute("donacionesEntregadas", List.of());
            model.addAttribute("errorMapa",
                    "No se pudieron cargar las donaciones entregadas.");
        }

        return "mapa-donaciones";
    }

    @PostMapping("/donante/nueva-donacion")
    public String procesarNuevaDonacion(
            @RequestParam String descripcionDonacion,
            @RequestParam List<String> descripcionBien,
            @RequestParam List<Integer> cantidad,
            @RequestParam List<String> unidad,
            @RequestParam List<String> categoria,
            @RequestParam List<String> subcategoria,
            @RequestParam List<Double> pesoUnitarioKg,
            @RequestParam List<Double> volumenUnitarioLitros,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Long donanteId;
        try {
            donanteId = extraerDonanteId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo realizar la donación: No hay un perfil de donante activo.");
            return "redirect:/donante/nueva-donacion";
        }

        // Peso y volumen los exige el motor de ruteo: sin ellos la donacion nunca se puede planificar
        boolean faltanMedidas = pesoUnitarioKg.size() != descripcionBien.size()
                || volumenUnitarioLitros.size() != descripcionBien.size()
                || pesoUnitarioKg.stream().anyMatch(p -> p == null || p <= 0)
                || volumenUnitarioLitros.stream().anyMatch(v -> v == null || v <= 0);
        if (faltanMedidas) {
            redirectAttributes.addFlashAttribute("errorCatalogo", "Completá el peso y el volumen por unidad de cada bien (mayores a 0).");
            return "redirect:/donante/nueva-donacion";
        }

        // Construimos la lista de todos los bienes cargados en el formulario
        List<BienDTO> bienes = new ArrayList<>();
        for (int i = 0; i < descripcionBien.size(); i++) {
            BienDTO.CategoriaRef catRef = new BienDTO.CategoriaRef(null, categoria.get(i), false, false);
            BienDTO.SubcategoriaRef subRef = new BienDTO.SubcategoriaRef(null, subcategoria.get(i));
            BienDTO.UnidadRef uniRef = new BienDTO.UnidadRef(null, unidad.get(i), "UN");

            bienes.add(new BienDTO(
                    descripcionBien.get(i), catRef, subRef, uniRef, cantidad.get(i),
                    null, false, false, pesoUnitarioKg.get(i), volumenUnitarioLitros.get(i) / 1000 // litros -> m3
            ));
        }

        DonacionCreateRequest request = new DonacionCreateRequest(
                descripcionDonacion,
                java.time.LocalDate.now(),
                bienes
        );

        String backendUrl = donacionesServiceUrl + "/donaciones-service/donaciones/donantes/" + donanteId;

        try {
            webApiCallerService.post(backendUrl, request, DonacionResponse.class);
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            System.out.println("Error al guardar en el backend: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorCatalogo", "Ocurrió un error al registrar la donación.");
            return "redirect:/donante/nueva-donacion";
        }

        return "redirect:/donante/mis-donaciones";
    }

    // Sin fallback al usuarioId: es un id del auth-service y apuntaria a otro donante
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