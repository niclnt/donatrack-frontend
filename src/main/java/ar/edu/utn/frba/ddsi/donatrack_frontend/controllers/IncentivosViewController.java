package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.InsigniaDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.MisionResponseDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.PuestoDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.SesionExpiradaException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final String incentivosServiceUrl;
    private final WebApiCallerService webApiCallerService;

    public IncentivosViewController(
            WebApiCallerService webApiCallerService,
            @Value("${incentivos.service.url:http://localhost:8082}") String incentivosServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.incentivosServiceUrl = incentivosServiceUrl;
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
            log.info("[INCENTIVOS-VIEW] Consultando Misión Actual: {}/incentivos/donantes/{}/mision-actual", incentivosServiceUrl, donanteId);
            Map<String, Object> misionActualMap = webApiCallerService.get(
                    incentivosServiceUrl + "/incentivos/donantes/" + donanteId + "/mision-actual",
                    Map.class
            );
            log.info("[INCENTIVOS-VIEW] Respuesta Misión Actual: {}", misionActualMap);

            // B. Métricas del Donante (Categoría y avance en misiones)
            log.info("[INCENTIVOS-VIEW] Consultando Métricas: {}/incentivos/donantes/{}/metricas", incentivosServiceUrl, donanteId);
            Map<String, Object> metricasMap = webApiCallerService.get(
                    incentivosServiceUrl + "/incentivos/donantes/" + donanteId + "/metricas",
                    Map.class
            );
            log.info("[INCENTIVOS-VIEW] Respuesta Métricas: {}", metricasMap);

            // C. Insignias Obtenidas
            log.info("[INCENTIVOS-VIEW] Consultando Insignias: {}/incentivos/donantes/{}/insignias", incentivosServiceUrl, donanteId);
            List<InsigniaDTO> insignias = webApiCallerService.getList(
                    incentivosServiceUrl + "/incentivos/donantes/" + donanteId + "/insignias",
                    InsigniaDTO[].class
            );
            log.info("[INCENTIVOS-VIEW] Insignias obtenidas: {}", insignias != null ? insignias.size() : 0);

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
            model.addAttribute("insignias", insignias != null ? insignias : List.of());

            // Ranking Mensual de Donantes desde el endpoint REST
            List<PuestoDTO> ranking = List.of();
            try {
                log.info("[INCENTIVOS-VIEW] Consultando Ranking Mensual: {}/incentivos/donantes/ranking-mensual", incentivosServiceUrl);
                ranking = webApiCallerService.getList(
                        incentivosServiceUrl + "/incentivos/donantes/ranking-mensual",
                        PuestoDTO[].class
                );
                log.info("[INCENTIVOS-VIEW] Ranking mensual recuperado con {} puestos.", ranking != null ? ranking.size() : 0);
            } catch (Exception e) {
                log.warn("[INCENTIVOS-VIEW] No se pudo obtener el ranking mensual desde el servicio de incentivos: {}", e.getMessage());
                ranking = List.of();
            }
            model.addAttribute("ranking", ranking != null ? ranking : List.of());

        } catch (SesionExpiradaException e) {
            throw e; // ErroresControllerAdvice manda al login
        } catch (Exception e) {
            log.error("[INCENTIVOS-VIEW] Error al conectar con el servicio de incentivos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al conectar con el servicio de incentivos: " + e.getMessage());
        }

        return "donante/incentivos";
    }
}