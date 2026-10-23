package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo.CategoriaCatalogoResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.BienDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.asignacion.PropuestaAsignacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.importacion.ResultadoImportacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.PuestoDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.CamionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.CamionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Controller
public class AdminController {

    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;
    private final String logisticaServiceUrl;

    public AdminController(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl,
            @Value("${logistica.service.url}") String logisticaServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
        this.logisticaServiceUrl = logisticaServiceUrl;
    }

    // --- PANEL DE INICIO ---

    // Cada contador se carga por separado: si un servicio no responde, se muestra "—" y el resto sigue
    @GetMapping({"/admin", "/admin/inicio"})
    public String mostrarInicio(Model model) {
        try {
            List<Map> donaciones = webApiCallerService.getList(
                    donacionesServiceUrl + "/donaciones-service/donaciones?estado=EN_DEPOSITO", Map[].class);
            model.addAttribute("enDeposito", donaciones.stream()
                    .mapToInt(d -> d.get("donacionesSegmentadas") instanceof List<?> s ? s.size() : 0)
                    .sum());
        } catch (Exception e) {
            model.addAttribute("enDeposito", null);
        }

        try {
            List<PropuestaAsignacionResponse> propuestas = webApiCallerService.getList(
                    donacionesServiceUrl + "/api/asignaciones/rankings/top", PropuestaAsignacionResponse[].class);
            model.addAttribute("pendientesAsignacion", propuestas.stream()
                    .map(PropuestaAsignacionResponse::idDonacionSegmentada).distinct().count());
        } catch (Exception e) {
            model.addAttribute("pendientesAsignacion", null);
        }

        try {
            List<CamionResponse> camiones = webApiCallerService.getList(
                    logisticaServiceUrl + "/logistica/camiones", CamionResponse[].class);
            model.addAttribute("camionesTotal", camiones.size());
            model.addAttribute("camionesDisponibles", camiones.stream().filter(c -> Boolean.TRUE.equals(c.disponible())).count());
        } catch (Exception e) {
            model.addAttribute("camionesTotal", null);
        }

        return "admin/inicio";
    }

    // --- GESTIÓN SEPARADA DE DONANTES ---

    // pj: persona juridica elegida en la seccion de representantes
    @GetMapping("/admin/donantes")
    public String mostrarGestionDonantes(@RequestParam(required = false) Long pj, Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            List<Map> organizaciones = new ArrayList<>(webApiCallerService.getList(
                    donacionesServiceUrl + "/donantes/personaJuridica", Map[].class));
            organizaciones.sort(Comparator.comparing(o -> String.valueOf(o.get("razonSocial")), String.CASE_INSENSITIVE_ORDER));
            model.addAttribute("organizaciones", organizaciones);
        } catch (ApiException | ServicioNoDisponibleException e) {
            model.addAttribute("organizaciones", List.of());
            model.addAttribute("errorRep", "No se pudieron cargar las organizaciones: " + e.getMessage());
        }

        if (pj != null) {
            model.addAttribute("pjSeleccionada", pj);
            try {
                model.addAttribute("representantes", webApiCallerService.getList(
                        representantesUrl(pj), RepresentanteResponse[].class));
            } catch (ApiException | ServicioNoDisponibleException e) {
                model.addAttribute("representantes", List.of());
                model.addAttribute("errorRep", "No se pudieron cargar los representantes: " + e.getMessage());
            }
        }
        return "admin/donantes";
    }

    @PostMapping("/admin/donantes/{pj}/representantes")
    public String agregarRepresentante(@PathVariable Long pj, @ModelAttribute RepresentanteRequest request,
                                       RedirectAttributes redirect) {
        try {
            webApiCallerService.post(representantesUrl(pj), request, RepresentanteResponse[].class);
            redirect.addFlashAttribute("exitoRep", "Representante agregado correctamente.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("errorRep", e.getMessage());
        }
        return "redirect:/admin/donantes?pj=" + pj + "#representantes";
    }

    @PostMapping("/admin/donantes/{pj}/representantes/{id}/quitar")
    public String quitarRepresentante(@PathVariable Long pj, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            webApiCallerService.delete(representantesUrl(pj) + "/" + id);
            redirect.addFlashAttribute("exitoRep", "Representante quitado.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("errorRep", e.getMessage());
        }
        return "redirect:/admin/donantes?pj=" + pj + "#representantes";
    }

    private String representantesUrl(Long personaJuridicaId) {
        return donacionesServiceUrl + "/donantes/personaJuridica/" + personaJuridicaId + "/representantes";
    }

    @PostMapping("/admin/donantes/crear")
    public String crearDonanteForm(
            @RequestParam String tipoPersona,
            @RequestParam String mail,
            @RequestParam(required = false) String telefono,
            @RequestParam(defaultValue = "EMAIL") String medioPreferido,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String tipoDoc,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) Integer edad,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String razonSocial,
            @RequestParam(required = false) String tipoJuridico,
            @RequestParam(required = false) String rubro,
            Authentication authentication,
            RedirectAttributes redirect) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        Map<String, Object> medioEmail = Map.of("valor", mail, "tipoContacto", "EMAIL");
        Map<String, Object> medioPref = "EMAIL".equalsIgnoreCase(medioPreferido)
                ? medioEmail
                : Map.of("valor", telefono != null ? telefono : mail, "tipoContacto", "TELEFONO");

        List<Map<String, Object>> medios = new ArrayList<>();
        medios.add(medioEmail);
        if (telefono != null && !telefono.isBlank()) {
            medios.add(Map.of("valor", telefono, "tipoContacto", "TELEFONO"));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("mail", mail);
        requestBody.put("mediosDeContacto", medios);
        requestBody.put("medioDeContactoPreferido", medioPref);

        String endpoint;
        if ("JURIDICA".equalsIgnoreCase(tipoPersona)) {
            endpoint = "/donantes/personaJuridica";
            requestBody.put("razonSocial", razonSocial);
            requestBody.put("tipoJuridico", tipoJuridico);
            requestBody.put("rubro", rubro);
        } else {
            endpoint = "/donantes/personaHumana";
            requestBody.put("nombre", nombre);
            requestBody.put("tipoDoc", tipoDoc);
            requestBody.put("documento", documento);
            requestBody.put("edad", edad);
            requestBody.put("genero", genero);
            requestBody.put("direccion", direccion);
        }

        try {
            String url = donacionesServiceUrl + endpoint;
            webApiCallerService.post(url, requestBody, Map.class);
            redirect.addFlashAttribute("exito", "¡Donante registrado correctamente!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al registrar donante: " + e.getMessage());
        }

        return "redirect:/admin/donantes";
    }

    @GetMapping("/admin/donantes/buscar")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buscarDonantesProxy(@RequestParam(defaultValue = "HUMANA") String tipo) {
        String endpoint = tipo.equalsIgnoreCase("JURIDICA")
                ? "/donantes/personaJuridica"
                : "/donantes/personaHumana";

        try {
            List rawList = webApiCallerService.getList(donacionesServiceUrl + endpoint, Map[].class);
            return (List<Map<String, Object>>) rawList;
        } catch (Exception e) {
            System.out.println("Error buscando donantes en proxy: " + e.getMessage());
            return List.of();
        }
    }

    // --- REQ 15: REGISTRAR DONACIÓN ---

    @GetMapping("/admin/registrar-donacion")
    public String mostrarRegistroDonacion(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        cargarCategorias(model);
        return "admin/registrar-donacion";
    }

    // Los campos de cada bien llegan repetidos (uno por bloque del formulario). Se leen del
    // MultiValueMap y no como @RequestParam List<String>: con un solo bien Spring partiria
    // por comas un valor como "Fideos, arroz"
    @PostMapping("/admin/registrar-donacion")
    public String procesarRegistroDonacion(
            @RequestParam(required = false) Long donanteId,
            @RequestParam(required = false) Long representanteId,
            @RequestParam String descripcion,
            @RequestParam String responsable,
            @RequestParam MultiValueMap<String, String> params,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        if (donanteId == null) {
            model.addAttribute("error", "Seleccioná un donante antes de registrar la donación.");
            return mostrarRegistroDonacion(authentication, model);
        }

        List<BienDTO> bienes;
        try {
            bienes = armarBienes(params);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return mostrarRegistroDonacion(authentication, model);
        }

        DonacionCreateRequest request = new DonacionCreateRequest(descripcion + " (Recepción por: " + responsable + ")", LocalDate.now(), bienes, representanteId);

        String backendUrl = donacionesServiceUrl + "/donaciones-service/donaciones/donantes/" + donanteId;

        try {
            webApiCallerService.post(backendUrl, request, String.class);
            model.addAttribute("exito", "Donación registrada correctamente en depósito (" + bienes.size() + " bien(es)).");
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar la donación: " + e.getMessage());
        }

        cargarCategorias(model);
        return "admin/registrar-donacion";
    }

    private List<BienDTO> armarBienes(MultiValueMap<String, String> params) {
        List<String> descripciones = params.getOrDefault("descripcionBien", List.of());
        List<String> categorias = params.getOrDefault("categoria", List.of());
        List<String> subcategorias = params.getOrDefault("subcategoria", List.of());
        List<String> cantidades = params.getOrDefault("cantidad", List.of());
        List<String> unidades = params.getOrDefault("unidad", List.of());
        List<String> pesos = params.getOrDefault("pesoUnitarioKg", List.of());
        List<String> volumenes = params.getOrDefault("volumenUnitarioLitros", List.of());
        List<String> estados = params.getOrDefault("estado", List.of());
        List<String> vencimientos = params.getOrDefault("vencimiento", List.of());

        int total = descripciones.size();
        if (total == 0) {
            throw new IllegalArgumentException("Cargá al menos un bien.");
        }
        boolean incompletos = List.of(categorias, subcategorias, cantidades, unidades, pesos, volumenes, estados, vencimientos)
                .stream().anyMatch(lista -> lista.size() != total);
        if (incompletos) {
            throw new IllegalArgumentException("Completá categoría, subcategoría, cantidad, unidad, peso y volumen de cada bien.");
        }

        List<BienDTO> bienes = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int cantidad;
            double peso;
            double volumenLitros;
            LocalDate fechaVenc;
            try {
                cantidad = Integer.parseInt(cantidades.get(i));
                peso = Double.parseDouble(pesos.get(i));
                volumenLitros = Double.parseDouble(volumenes.get(i));
                String venc = vencimientos.get(i);
                fechaVenc = (venc != null && !venc.isBlank()) ? LocalDate.parse(venc) : null;
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Hay datos inválidos en el bien #" + (i + 1) + ".");
            }

            // Peso y volumen los exige el motor de ruteo: sin ellos la donacion nunca se puede planificar
            if (cantidad < 1 || peso <= 0 || volumenLitros <= 0) {
                throw new IllegalArgumentException("En el bien #" + (i + 1) + " la cantidad, el peso y el volumen por unidad tienen que ser mayores a 0.");
            }

            BienDTO.CategoriaRef catRef = new BienDTO.CategoriaRef(null, categorias.get(i), false, false);
            BienDTO.SubcategoriaRef subRef = new BienDTO.SubcategoriaRef(null, subcategorias.get(i));
            BienDTO.UnidadRef uniRef = new BienDTO.UnidadRef(null, unidades.get(i), "UN");
            boolean usado = "Usado en buen estado".equals(estados.get(i));

            // El back guarda el volumen en m3: 1 litro = 0.001 m3
            bienes.add(new BienDTO(descripciones.get(i), catRef, subRef, uniRef, cantidad, fechaVenc, usado, false,
                    peso, volumenLitros / 1000));
        }
        return bienes;
    }

    private void cargarCategorias(Model model) {
        try {
            model.addAttribute("categorias", webApiCallerService.getList(
                    donacionesServiceUrl + "/donaciones/catalogo/categorias", CategoriaCatalogoResponse[].class));
        } catch (Exception e) {
            model.addAttribute("categorias", List.of());
            model.addAttribute("errorCatalogo", "No se pudieron cargar las categorías. Intentá nuevamente más tarde.");
        }
    }

    // --- REQ 16: DEPÓSITO Y VENCIDAS ---

    @GetMapping("/admin/deposito-vencidas")
    public String mostrarDeposito(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            String url = donacionesServiceUrl + "/donaciones-service/donaciones?estado=EN_DEPOSITO";
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

        for (String parIds : segmentosIds) {
            String[] ids = parIds.split("-");
            String donacionId = ids[0];
            String segmentadaId = ids[1];

            String url = donacionesServiceUrl + "/donaciones-service/donaciones/" + donacionId + "/segmentadas/" + segmentadaId + "/estado";

            Map<String, String> requestBody = Map.of(
                    "nuevoEstado", "VENCIDA",
                    "motivo", motivo,
                    "responsable", authentication.getName()
            );

            try {
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

        Map<Long, List<PropuestaAsignacionResponse>> propuestasPorDonacion = new LinkedHashMap<>();
        try {
            List<PropuestaAsignacionResponse> propuestas = webApiCallerService.getList(
                    donacionesServiceUrl + "/api/asignaciones/rankings/top", PropuestaAsignacionResponse[].class);
            for (PropuestaAsignacionResponse propuesta : propuestas) {
                propuestasPorDonacion.computeIfAbsent(propuesta.idDonacionSegmentada(), id -> new ArrayList<>()).add(propuesta);
            }
        } catch (ApiException | ServicioNoDisponibleException e) {
            model.addAttribute("error", "No se pudieron cargar las sugerencias del algoritmo: " + e.getMessage());
        }
        model.addAttribute("propuestasPorDonacion", propuestasPorDonacion);

        return "admin/asignar-algoritmo";
    }

    @PostMapping("/admin/forzar-algoritmo")
    public String forzarAlgoritmo(Authentication authentication, RedirectAttributes redirect) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            webApiCallerService.post(donacionesServiceUrl + "/api/asignaciones/forzar-ejecucion", null, String.class);
            redirect.addFlashAttribute("exito", "El algoritmo se ejecutó y actualizó las sugerencias.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo ejecutar el algoritmo: " + e.getMessage());
        }

        return "redirect:/admin/asignar-algoritmo";
    }

    @PostMapping("/admin/confirmar-asignacion")
    public String confirmarAsignacion(
            @RequestParam Long idDonacionSegmentada,
            @RequestParam Long idNecesidad,
            Authentication authentication,
            RedirectAttributes redirect) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        String url = donacionesServiceUrl + "/api/asignaciones/" + idDonacionSegmentada
                + "/confirmar/" + idNecesidad + "?responsable=" + authentication.getName();

        try {
            webApiCallerService.post(url, null, String.class);
            redirect.addFlashAttribute("exito", "Asignación confirmada: la donación " + idDonacionSegmentada
                    + " quedó asignada a la necesidad " + idNecesidad + ".");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo confirmar la asignación: " + e.getMessage());
        }

        return "redirect:/admin/asignar-algoritmo";
    }

    // --- REQ 18: ADMINISTRAR CAMIONES ---

    @GetMapping("/admin/camiones")
    public String mostrarCamiones(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        List<CamionResponse> camiones = List.of();
        try {
            camiones = webApiCallerService.getList(logisticaServiceUrl + "/logistica/camiones", CamionResponse[].class);
        } catch (ApiException | ServicioNoDisponibleException e) {
            model.addAttribute("error", "No se pudo cargar la flota: " + e.getMessage());
        }
        model.addAttribute("camiones", camiones);
        model.addAttribute("disponibles", camiones.stream().filter(c -> Boolean.TRUE.equals(c.disponible())).count());

        return "admin/camiones";
    }

    @PostMapping("/admin/camiones")
    public String registrarCamion(
            @RequestParam String patente,
            @RequestParam(defaultValue = "true") boolean disponible,
            @RequestParam Double volumen,
            @RequestParam Double altura,
            @RequestParam Double carga,
            @RequestParam(required = false) String chofer,
            Authentication authentication,
            RedirectAttributes redirect) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        String patenteNormalizada = patente.trim().toUpperCase();
        String nombreChofer = chofer == null || chofer.isBlank() ? null : chofer.trim();
        CamionCreateRequest request = new CamionCreateRequest(patenteNormalizada, volumen, altura, carga, nombreChofer);

        try {
            webApiCallerService.post(logisticaServiceUrl + "/logistica/camiones", request, CamionResponse.class);
            if (!disponible) {
                cambiarDisponibilidadEnLogistica(patenteNormalizada, false);
            }
            redirect.addFlashAttribute("exito", "Camión " + patenteNormalizada + " registrado en la flota.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo registrar el camión: " + e.getMessage());
        }

        return "redirect:/admin/camiones";
    }

    @PostMapping("/admin/camiones/{patente}/disponibilidad")
    public String cambiarDisponibilidad(
            @PathVariable String patente,
            @RequestParam boolean disponible,
            Authentication authentication,
            RedirectAttributes redirect) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            cambiarDisponibilidadEnLogistica(patente, disponible);
            redirect.addFlashAttribute("exito", "Camión " + patente + (disponible ? " disponible." : " fuera de servicio."));
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo cambiar la disponibilidad: " + e.getMessage());
        }

        return "redirect:/admin/camiones";
    }

    private void cambiarDisponibilidadEnLogistica(String patente, boolean disponible) {
        String url = UriComponentsBuilder.fromUriString(logisticaServiceUrl)
                .path("/logistica/camiones/{patente}/disponibilidad")
                .queryParam("disponible", disponible)
                .buildAndExpand(patente)
                .toUriString();
        webApiCallerService.put(url, null, CamionResponse.class);
    }

    // --- REQ 19: RANKINGS MENSUALES ---

    @GetMapping("/admin/rankings")
    public String mostrarRankings(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        try {
            String url = "http://localhost:8082/incentivos/donantes/ranking-mensual";
            model.addAttribute("rankingMensual", webApiCallerService.getList(url, PuestoDTO[].class));
        } catch (Exception e) {
            model.addAttribute("rankingMensual", List.of());
            System.out.println("Error cargando rankings: " + e.getMessage());
        }

        return "admin/rankings";
    }

    // --- REQ 20: IMPORTAR CSV ---

    @GetMapping("/admin/importar-csv")
    public String mostrarImportacion(
            @RequestParam(required = false) String archivoGrande,
            Authentication authentication,
            Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";
        if (archivoGrande != null) {
            model.addAttribute("error", "El archivo supera el tamaño máximo permitido (20 MB).");
        }
        return "admin/importar-csv";
    }

    @PostMapping("/admin/importar-csv")
    public String procesarImportacion(
            @RequestParam("archivo") MultipartFile archivo,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        if (archivo.isEmpty()) {
            model.addAttribute("error", "Por favor, seleccioná un archivo CSV válido.");
            return "admin/importar-csv";
        }

        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        try {
            partes.add("archivo", new ByteArrayResource(archivo.getBytes()) {
                @Override
                public String getFilename() {
                    return archivo.getOriginalFilename();
                }
            });
        } catch (IOException e) {
            model.addAttribute("error", "No se pudo leer el archivo subido.");
            return "admin/importar-csv";
        }

        try {
            ResultadoImportacionResponse resultado = webApiCallerService.postMultipart(
                    donacionesServiceUrl + "/donantes/importar", partes, ResultadoImportacionResponse.class);
            model.addAttribute("exito", "El archivo fue procesado.");
            model.addAttribute("resultado", resultado);
        } catch (ApiException | ServicioNoDisponibleException e) {
            model.addAttribute("error", "No se pudo importar el archivo: " + e.getMessage());
        }

        return "admin/importar-csv";
    }
}