package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.catalogo.CategoriaCatalogoResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.BienDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.DonacionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.asignacion.PropuestaAsignacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.importacion.ResultadoImportacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.PuestoDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.CamionCreateRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.CamionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.EntregaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.representantes.RepresentanteResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;
    private final String logisticaServiceUrl;
    private final String authServiceUrl;
    private final String incentivosServiceUrl;

    public AdminController(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl,
            @Value("${logistica.service.url}") String logisticaServiceUrl,
            @Value("${auth.service.url}") String authServiceUrl,
            @Value("${incentivos.service.url}") String incentivosServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
        this.logisticaServiceUrl = logisticaServiceUrl;
        this.authServiceUrl = authServiceUrl;
        this.incentivosServiceUrl = incentivosServiceUrl;
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
            // Un input por bien (vacio si no se adjunto foto): la posicion indica a que bien corresponde
            @RequestParam(value = "fotoBien", required = false) List<MultipartFile> fotosBienes,
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
            MultiValueMap<String, Object> partesFotos = partesDeFotos(fotosBienes);
            if (partesFotos.isEmpty()) {
                webApiCallerService.post(backendUrl, request, String.class);
            } else {
                // Con fotos: multipart con la donacion en JSON y una parte "foto-{i}" por bien
                HttpHeaders json = new HttpHeaders();
                json.setContentType(MediaType.APPLICATION_JSON);
                partesFotos.add("donacion", new HttpEntity<>(request, json));
                webApiCallerService.postMultipart(backendUrl, partesFotos, String.class);
            }
            model.addAttribute("exito", "Donación registrada correctamente en depósito (" + bienes.size() + " bien(es)).");
        } catch (ApiException e) {
            // Validacion del back: no se registro nada; se listan los campos con error
            model.addAttribute("error", "No se registró la donación. " + e.getMessage());
            model.addAttribute("erroresCampo", e.mensajesParaMostrar());
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar la donación: " + e.getMessage());
        }

        cargarCategorias(model);
        return "admin/registrar-donacion";
    }

    // Cada foto viaja con su nombre y Content-Type; donaciones la guarda en el almacenamiento de imagenes
    private static MultiValueMap<String, Object> partesDeFotos(List<MultipartFile> fotos) throws IOException {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        if (fotos == null) return partes;
        for (int i = 0; i < fotos.size(); i++) {
            MultipartFile foto = fotos.get(i);
            if (foto == null || foto.isEmpty()) continue;
            HttpHeaders encabezado = new HttpHeaders();
            encabezado.setContentType(foto.getContentType() != null
                    ? MediaType.parseMediaType(foto.getContentType()) : MediaType.APPLICATION_OCTET_STREAM);
            String nombre = foto.getOriginalFilename();
            partes.add("foto-" + i, new HttpEntity<>(new ByteArrayResource(foto.getBytes()) {
                @Override
                public String getFilename() {
                    return nombre;
                }
            }, encabezado));
        }
        return partes;
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

    // --- REQ 16: STOCK DEL DEPÓSITO ---

    private static final List<String> ESTADOS_DEPOSITO = List.of("EN_DEPOSITO", "ENTREGA_FALLIDA", "VENCIDA", "DESCARTADA");

    @GetMapping("/admin/deposito-vencidas")
    public String mostrarDeposito(@RequestParam(defaultValue = "EN_DEPOSITO") String estado,
                                  Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        if (!ESTADOS_DEPOSITO.contains(estado)) estado = "EN_DEPOSITO";

        // Cantidad de segmentos por estado, para los filtros
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        List<Map> donaciones = List.of();
        for (String e : ESTADOS_DEPOSITO) {
            List<Map> porEstado;
            try {
                String url = donacionesServiceUrl + "/donaciones-service/donaciones?estado=" + e;
                porEstado = webApiCallerService.getList(url, Map[].class);
            } catch (Exception ex) {
                porEstado = List.of();
            }
            cantidades.put(e, porEstado.stream()
                    .mapToInt(d -> d.get("donacionesSegmentadas") instanceof List<?> l ? l.size() : 0)
                    .sum());
            if (e.equals(estado)) donaciones = porEstado;
        }

        model.addAttribute("estado", estado);
        model.addAttribute("cantidades", cantidades);
        model.addAttribute("donaciones", donaciones);
        return "admin/deposito-vencidas";
    }

    @PostMapping("/admin/deposito/cambiar-estado")
    public String cambiarEstadoDeposito(
            @RequestParam(required = false) List<String> segmentosIds,
            @RequestParam String nuevoEstado,
            @RequestParam String motivo,
            @RequestParam(defaultValue = "EN_DEPOSITO") String estado,
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        if (!List.of("VENCIDA", "DESCARTADA").contains(nuevoEstado)) {
            model.addAttribute("error", "Acción no válida.");
            return mostrarDeposito(estado, authentication, model);
        }
        if (segmentosIds == null || segmentosIds.isEmpty()) {
            model.addAttribute("error", "Seleccioná al menos una donación.");
            return mostrarDeposito(estado, authentication, model);
        }

        int fallidos = 0;
        for (String parIds : segmentosIds) {
            String[] ids = parIds.split("-");
            String donacionId = ids[0];
            String segmentadaId = ids[1];

            String url = donacionesServiceUrl + "/donaciones-service/donaciones/" + donacionId + "/segmentadas/" + segmentadaId + "/estado";

            Map<String, String> requestBody = Map.of(
                    "nuevoEstado", nuevoEstado,
                    "motivo", motivo,
                    "responsable", authentication.getName()
            );

            try {
                webApiCallerService.put(url, requestBody, String.class);
            } catch (Exception e) {
                fallidos++;
                System.out.println("Error al cambiar estado a " + nuevoEstado + ": " + e.getMessage());
            }
        }

        if (fallidos > 0) {
            model.addAttribute("error", "No se pudieron actualizar " + fallidos + " de " + segmentosIds.size() + " donaciones.");
            return mostrarDeposito(estado, authentication, model);
        }
        return "redirect:/admin/deposito-vencidas?estado=" + estado + "&exito=true";
    }

    // Entregas fallidas: el admin confirma que la caja volvio al galpon. Lo registra logistica (la entrega
    // vuelve a PENDIENTE) y avisa por RabbitMQ a donaciones, que pasa la donacion a EN_DEPOSITO para reasignarla.
    @PostMapping("/admin/deposito/regresar")
    public String regresarADeposito(@RequestParam(required = false) List<String> segmentosIds,
                                    Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        if (segmentosIds == null || segmentosIds.isEmpty()) {
            model.addAttribute("error", "Seleccioná al menos una donación.");
            return mostrarDeposito("ENTREGA_FALLIDA", authentication, model);
        }

        // Una entrega puede llevar varias donaciones: al regresarla vuelven todas, asi que no se repite el pedido
        Set<Long> yaRegresadas = new HashSet<>();
        int fallidos = 0;
        for (String parIds : segmentosIds) {
            Long segmentadaId = Long.valueOf(parIds.split("-")[1]);
            if (yaRegresadas.contains(segmentadaId)) continue;
            try {
                EntregaResponse entrega = webApiCallerService.post(
                        logisticaServiceUrl + "/logistica/trazabilidad/donaciones/" + segmentadaId + "/regresar",
                        null, EntregaResponse.class);
                yaRegresadas.add(segmentadaId);
                if (entrega != null && entrega.donacionesSegmentadas() != null) {
                    entrega.donacionesSegmentadas().forEach(d -> yaRegresadas.add(d.id()));
                }
            } catch (Exception e) {
                fallidos++;
                System.out.println("Error al regresar al depósito la donación " + segmentadaId + ": " + e.getMessage());
            }
        }

        if (fallidos > 0) {
            model.addAttribute("error", "No se pudieron regresar al depósito " + fallidos + " de " + segmentosIds.size() + " donaciones.");
            return mostrarDeposito("ENTREGA_FALLIDA", authentication, model);
        }
        return "redirect:/admin/deposito-vencidas?estado=ENTREGA_FALLIDA&regresadas=true";
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

    // Mismo proceso que corre el scheduler de donaciones; la fecha (opcional) simula el paso del tiempo en la demo
    @PostMapping("/admin/necesidades/renovar")
    public String renovarRecurrentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirect) {
        String url = donacionesServiceUrl + "/donaciones/necesidades/recurrentes/renovar"
                + (fecha != null ? "?fecha=" + fecha : "");
        try {
            Map<?, ?> resultado = webApiCallerService.post(url, null, Map.class);
            Object renovadas = resultado != null ? resultado.get("renovadas") : 0;
            redirect.addFlashAttribute("exito", "Renovación de necesidades recurrentes al "
                    + (fecha != null ? fecha : LocalDate.now()) + ": " + renovadas + " con período nuevo.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudieron renovar las necesidades recurrentes: " + e.getMessage());
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

    // Dispara a demanda la planificacion de rutas (mismo flujo que el scheduler nocturno de logistica)
    @PostMapping("/admin/planificacion/ejecutar")
    public String ejecutarPlanificacion(RedirectAttributes redirect) {
        try {
            Map<?, ?> resumen = webApiCallerService.post(
                    logisticaServiceUrl + "/logistica/rutas/planificacion/ejecutar", null, Map.class);
            Object enviadas = resumen != null ? resumen.get("donacionesEnviadas") : 0;
            Object lotes = resumen != null ? resumen.get("lotesEnviados") : 0;
            redirect.addFlashAttribute("exito", "Planificación enviada al motor de ruteo: " + enviadas
                    + " donaciones en " + lotes + " lote(s). Las rutas aparecen cuando el motor responde.");
        } catch (ApiException | ServicioNoDisponibleException e) {
            redirect.addFlashAttribute("error", "No se pudo ejecutar la planificación: " + e.getMessage());
        }
        return "redirect:/admin/camiones";
    }

    @PostMapping("/admin/camiones")
    public String registrarCamion(
            @RequestParam String patente,
            @RequestParam(defaultValue = "true") boolean disponible,
            @RequestParam Double volumen,
            @RequestParam Double altura,
            @RequestParam Double carga,
            @RequestParam(required = false) String chofer,
            @RequestParam(required = false) String choferMail,
            @RequestParam(required = false) String choferPassword,
            Authentication authentication,
            RedirectAttributes redirect) {

        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        String patenteNormalizada = patente.trim().toUpperCase();
        String nombreChofer = chofer == null || chofer.isBlank() ? null : chofer.trim();
        String mailChofer = choferMail == null || choferMail.isBlank() ? null : choferMail.trim();
        boolean conPassword = choferPassword != null && !choferPassword.isBlank();

        // El chofer necesita cuenta para ver e iniciar sus rutas: nombre, mail y contraseña van juntos
        if (nombreChofer != null && (mailChofer == null || !conPassword)) {
            redirect.addFlashAttribute("error", "Para asignar un chofer cargá también su correo y una contraseña inicial.");
            return "redirect:/admin/camiones";
        }
        if (conPassword && choferPassword.length() < 8) {
            redirect.addFlashAttribute("error", "La contraseña del chofer debe tener al menos 8 caracteres.");
            return "redirect:/admin/camiones";
        }

        try {
            Long usuarioIdChofer = null;
            if (nombreChofer != null) {
                // Solo un admin puede crear usuarios CHOFER: se usa el token de la sesion
                Map<String, String> usuario = Map.of("mail", mailChofer, "password", choferPassword, "rol", SesionUsuario.ROL_CHOFER);
                Map<?, ?> creado = webApiCallerService.post(authServiceUrl + "/usuarios", usuario, Map.class);
                usuarioIdChofer = creado != null && creado.get("id") instanceof Number id ? id.longValue() : null;
            }

            CamionCreateRequest request = new CamionCreateRequest(patenteNormalizada, volumen, altura, carga, nombreChofer, usuarioIdChofer);
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

    // Ranking mensual a demanda (paso 5 del flujo 8): incentivos lo calcula con el mismo criterio que el cierre de mes
    @PostMapping("/admin/rankings/generar")
    public String generarRankingMensual(RedirectAttributes redirect) {
        try {
            PuestoDTO[] ranking = webApiCallerService.post(
                    incentivosServiceUrl + "/incentivos/ranking-mensual/generar", Map.of(), PuestoDTO[].class);
            int puestos = ranking != null ? ranking.length : 0;
            redirect.addFlashAttribute("exitoRanking", "Ranking del mes generado: " + puestos
                    + (puestos == 1 ? " donante." : " donantes."));
        } catch (Exception e) {
            log.error("[RANKING] No se pudo generar el ranking mensual: {}", e.getMessage());
            redirect.addFlashAttribute("errorRanking", "No se pudo generar el ranking. Intentá de nuevo en unos minutos.");
        }
        return "redirect:/admin/rankings";
    }

    // Ranking mensual + historial: sin periodo muestra el ultimo; con ?periodo=yyyy-MM, el de ese mes
    @GetMapping("/admin/rankings")
    public String mostrarRankings(@RequestParam(required = false) String periodo,
                                  Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";

        // Meses con ranking guardado, para el selector ("2026-11" -> "Noviembre 2026")
        Map<String, String> periodos = new LinkedHashMap<>();
        try {
            String[] lista = webApiCallerService.get(incentivosServiceUrl + "/incentivos/ranking-mensual/periodos", String[].class);
            for (String p : lista == null ? new String[0] : lista) {
                periodos.put(p, nombreDelMes(p));
            }
        } catch (Exception e) {
            log.warn("[RANKING] No se pudo cargar el historial de rankings: {}", e.getMessage());
        }

        String seleccionado = periodo != null && periodos.containsKey(periodo) ? periodo : null;
        try {
            String url = incentivosServiceUrl + "/incentivos/donantes/ranking-mensual"
                    + (seleccionado != null ? "?periodo=" + seleccionado : "");
            model.addAttribute("rankingMensual", webApiCallerService.getList(url, PuestoDTO[].class));
        } catch (Exception e) {
            model.addAttribute("rankingMensual", List.of());
            log.warn("[RANKING] Error cargando el ranking mensual: {}", e.getMessage());
        }

        // Sin seleccion se muestra el ultimo ranking guardado: el mes mas nuevo del selector
        String mostrado = seleccionado != null ? seleccionado : periodos.keySet().stream().findFirst().orElse(null);
        model.addAttribute("periodos", periodos);
        model.addAttribute("periodoSeleccionado", mostrado);
        model.addAttribute("nombrePeriodo", mostrado != null ? periodos.get(mostrado) : null);
        return "admin/rankings";
    }

    private static String nombreDelMes(String periodo) {
        try {
            java.time.YearMonth mes = java.time.YearMonth.parse(periodo);
            String nombre = mes.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.of("es", "AR"));
            return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
        } catch (RuntimeException e) {
            return periodo;
        }
    }

    // --- ALTA DE ADMINISTRADORES ---

    @GetMapping("/admin/administradores")
    public String mostrarAltaAdministrador() {
        return "admin/administradores";
    }

    // Auth solo deja crear el rol ADMIN si quien llama es admin: se usa el token de la sesion.
    // La contraseña queda como inicial y el nuevo admin la tiene que cambiar al entrar.
    @PostMapping("/admin/administradores")
    public String crearAdministrador(
            @RequestParam String mail,
            @RequestParam String password,
            @RequestParam String confirmacion,
            RedirectAttributes redirect) {

        String mailNormalizado = mail == null ? "" : mail.trim();
        String error = null;
        if (mailNormalizado.isEmpty() || password == null || password.isBlank()) {
            error = "Completá el correo y la contraseña inicial.";
        } else if (password.length() < 8) {
            error = "La contraseña inicial debe tener al menos 8 caracteres.";
        } else if (!password.equals(confirmacion)) {
            error = "Las contraseñas no coinciden.";
        } else {
            try {
                Map<String, String> usuario = Map.of("mail", mailNormalizado, "password", password, "rol", SesionUsuario.ROL_ADMIN);
                webApiCallerService.post(authServiceUrl + "/usuarios", usuario, Map.class);
                redirect.addFlashAttribute("exito", "Administrador " + mailNormalizado
                        + " creado. Al ingresar por primera vez va a tener que cambiar la contraseña.");
                return "redirect:/admin/administradores";
            } catch (ApiException | ServicioNoDisponibleException e) {
                error = "No se pudo crear el administrador: " + e.getMessage();
            }
        }
        // Se conserva el correo para no tener que tipearlo de nuevo
        redirect.addFlashAttribute("mail", mailNormalizado);
        redirect.addFlashAttribute("error", error);
        return "redirect:/admin/administradores";
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