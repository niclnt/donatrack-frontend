package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo.CategoriaCatalogoResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.MapaDonacionesApiService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String inicio(Authentication authentication, Model model) {
        // Con sesion, cada rol va a su panel (/panel decide segun el rol)
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/panel";
        }

        // Ejemplos destacados: lo entregado en el ultimo mes
        try {
            model.addAttribute("donacionesRecientes", mapaDonacionesApiService.obtenerEntregadasRecientes(30, 6));
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            model.addAttribute("donacionesRecientes", List.of());
            model.addAttribute("errorRecientes", "No pudimos cargar las donaciones recientes. Intentá más tarde.");
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

        boolean esDonanteJuridico = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + SesionUsuario.ROL_DONANTE_JURIDICO));
        model.addAttribute("esDonanteJuridico", esDonanteJuridico);

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

        List<Map<String, Object>> donaciones = new ArrayList<>();
        try {
            List<Map> respuesta = webApiCallerService.getList(backendUrl.toString(), Map[].class);
            for (Map donacion : respuesta) {
                Map<String, Object> donacionVista = new LinkedHashMap<>();
                donacion.forEach((clave, valor) -> {
                    if (clave instanceof String nombreClave) {
                        donacionVista.put(nombreClave, valor);
                    }
                });
                donaciones.add(donacionVista);
            }
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            model.addAttribute("error", "No se pudieron cargar tus donaciones en este momento.");
        }

        if (esDonanteJuridico && !donaciones.isEmpty()) {
            try {
                List<RepresentanteResponse> representantes = webApiCallerService.getList(
                        donacionesServiceUrl + "/donantes/personaJuridica/mia/representantes",
                        RepresentanteResponse[].class
                );
                Map<String, String> nombresPorId = representantes.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                representante -> representante.id().toString(),
                                RepresentanteResponse::nombre,
                                (primero, ignorado) -> primero
                        ));
                donaciones.forEach(donacion ->
                        donacion.put("representanteNombreVista", resolverNombreRepresentante(donacion, nombresPorId)));
            } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
                donaciones.forEach(donacion -> donacion.put("representanteNombreVista", "No disponible"));
                model.addAttribute("errorRepresentantes", "No se pudieron cargar los datos de representantes.");
            }
        }
        model.addAttribute("donaciones", donaciones);

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

    private String resolverNombreRepresentante(Map<String, Object> donacion, Map<String, String> nombresPorId) {
        Object nombreDirecto = donacion.get("representanteNombre");
        if (nombreDirecto == null) nombreDirecto = donacion.get("nombreRepresentante");
        if (nombreDirecto != null && !nombreDirecto.toString().isBlank()) {
            return nombreDirecto.toString();
        }

        Object representante = donacion.get("representante");
        if (representante instanceof Map<?, ?> datosRepresentante) {
            Object nombre = datosRepresentante.get("nombre");
            if (nombre != null && !nombre.toString().isBlank()) {
                return nombre.toString();
            }
            representante = datosRepresentante.get("id");
        } else if (representante != null && !(representante instanceof Number)) {
            return representante.toString();
        }

        Object representanteId = donacion.get("representanteId");
        if (representanteId == null) representanteId = donacion.get("idRepresentante");
        if (representanteId == null) representanteId = representante;
        return representanteId == null
                ? "—"
                : nombresPorId.getOrDefault(representanteId.toString(), "—");
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