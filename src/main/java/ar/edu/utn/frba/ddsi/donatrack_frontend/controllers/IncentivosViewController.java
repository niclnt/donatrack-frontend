package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.InsigniaDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.MisionResponseDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.PuestoDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.SesionExpiradaException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/donante/incentivos")
public class IncentivosViewController {

    private static final Logger log = LoggerFactory.getLogger(IncentivosViewController.class);

    private final String INCENTIVOS_SERVICE_URL = "http://localhost:8082";

    // Manda el token de la sesion en cada llamada (y lo refresca si vencio)
    private final WebApiCallerService webApiCallerService;

    public IncentivosViewController(WebApiCallerService webApiCallerService) {
        this.webApiCallerService = webApiCallerService;
    }

    @GetMapping
    public String verIncentivos(Model model) {
        Long donanteId = 1L; // Fallback para entorno de pruebas local

        // 1. Obtención y loggeo de datos de la sesión
        try {
            Object usuarioIdSesion = SesionUsuario.actual().getAttribute(SesionUsuario.USUARIO_ID);
            Object rolesSesion = SesionUsuario.actual().getAttribute(SesionUsuario.ROLES);
            Long idSesion = SesionUsuario.donanteId();

            log.info("[INCENTIVOS-VIEW] ===== INICIO DE CÁLCULO DE VISTA =====");
            log.info("[INCENTIVOS-VIEW] USUARIO_ID en Sesión: {}", usuarioIdSesion);
            log.info("[INCENTIVOS-VIEW] ROLES en Sesión: {}", rolesSesion);
            log.info("[INCENTIVOS-VIEW] DONANTE_ID recuperado de Sesión: {}", idSesion);

            if (idSesion != null) {
                donanteId = idSesion;
                log.info("[INCENTIVOS-VIEW] -> Usando DONANTE_ID REAL: {}", donanteId);
            } else {
                log.warn("[INCENTIVOS-VIEW] -> DONANTE_ID es NULL en la sesión. Usando FALLBACK: {}", donanteId);
            }
        } catch (Exception e) {
            log.error("[INCENTIVOS-VIEW] Error al acceder a la sesión: {}", e.getMessage());
        }

        // 2. Consulta a los microservicios de Incentivos
        try {
            // A. Misión Actual
            log.info("[INCENTIVOS-VIEW] Consultando Misión Actual: {}/incentivos/donantes/{}/mision-actual", INCENTIVOS_SERVICE_URL, donanteId);
            Map<String, Object> misionActualMap = webApiCallerService.get(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/" + donanteId + "/mision-actual",
                    Map.class
            );
            log.info("[INCENTIVOS-VIEW] Respuesta Misión Actual: {}", misionActualMap);

            // B. Métricas del Donante (Categoría y avance en misiones)
            log.info("[INCENTIVOS-VIEW] Consultando Métricas: {}/incentivos/donantes/{}/metricas", INCENTIVOS_SERVICE_URL, donanteId);
            Map<String, Object> metricasMap = webApiCallerService.get(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/" + donanteId + "/metricas",
                    Map.class
            );
            log.info("[INCENTIVOS-VIEW] Respuesta Métricas: {}", metricasMap);

            // C. Insignias Obtenidas
            log.info("[INCENTIVOS-VIEW] Consultando Insignias: {}/incentivos/donantes/{}/insignias", INCENTIVOS_SERVICE_URL, donanteId);
            List<InsigniaDTO> insignias = webApiCallerService.getList(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/" + donanteId + "/insignias",
                    InsigniaDTO[].class
            );
            log.info("[INCENTIVOS-VIEW] Insignias obtenidas: {}", insignias.size());

            // 3. Procesamiento y mapeo para Thymeleaf

            // Mapeo de Categoría y Misiones Completadas en el período/categoría
            String categoriaActual = "COLABORADOR";
            int misionesCompletadasCat = 0;
            int totalMisionesCat = 3; // Misiones estándar necesarias por categoría

            if (metricasMap != null) {
                if (metricasMap.get("categoriaActual") != null) {
                    categoriaActual = metricasMap.get("categoriaActual").toString();
                }
                if (metricasMap.get("misionesCompletadasPeriodoActual") != null) {
                    misionesCompletadasCat = ((Number) metricasMap.get("misionesCompletadasPeriodoActual")).intValue();
                }
            }

            int misionesFaltantes = Math.max(0, totalMisionesCat - misionesCompletadasCat);

            // Mapeo de Misión Actual (con progreso en % y detalle de objetivo)
            List<MisionResponseDTO> misiones = new ArrayList<>();
            if (misionActualMap != null && misionActualMap.get("nombre") != null) {
                String nombre = (String) misionActualMap.get("nombre");
                String descripcion = (String) misionActualMap.get("descripcion");

                Double progreso = 0.0;
                if (misionActualMap.get("progreso") != null) {
                    progreso = ((Number) misionActualMap.get("progreso")).doubleValue();
                }

                String detalleProgreso = "";
                if (misionActualMap.get("detalleProgreso") != null) {
                    detalleProgreso = (String) misionActualMap.get("detalleProgreso");
                }

                InsigniaDTO insigniaRecompensa = null;
                if (misionActualMap.get("insignia") != null) {
                    insigniaRecompensa = new InsigniaDTO((String) misionActualMap.get("insignia"), "Recompensa al completar esta misión");
                }

                misiones.add(new MisionResponseDTO(
                        null,
                        nombre,
                        descripcion,
                        progreso,
                        detalleProgreso,
                        "EN_PROGRESO",
                        insigniaRecompensa
                ));
            }

            // DTOs para el modelo de Thymeleaf
            model.addAttribute("categoriaActual", categoriaActual);
            model.addAttribute("misionesCompletadasCat", misionesCompletadasCat);
            model.addAttribute("totalMisionesCat", totalMisionesCat);
            model.addAttribute("misionesFaltantes", misionesFaltantes);
            model.addAttribute("misiones", misiones);
            model.addAttribute("insignias", insignias);

            // Ranking Mensual de Donantes
            List<PuestoDTO> ranking = List.of(
                    new PuestoDTO(1, "sebibalas11@gmail.com", 5),
                    new PuestoDTO(2, "nicolnt@gmail.com", 3)
            );
            model.addAttribute("ranking", ranking);

        } catch (SesionExpiradaException e) {
            throw e; // ErroresControllerAdvice manda al login
        } catch (Exception e) {
            log.error("[INCENTIVOS-VIEW] Error al conectar con el servicio de incentivos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al conectar con el servicio de incentivos: " + e.getMessage());
        }

        return "donante/incentivos";
    }
}