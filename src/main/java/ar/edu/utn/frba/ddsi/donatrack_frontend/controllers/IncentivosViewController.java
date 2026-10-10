package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.DashboardIncentivosDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.MisionResponseDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.PuestoDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/donante/incentivos")
public class IncentivosViewController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String INCENTIVOS_SERVICE_URL = "http://localhost:8082";

    @GetMapping
    public String verIncentivos(Model model) {
        Long donanteId = 1L;

        // 1. Obtención segura del donanteId desde la Sesión
        try {
            Long idSesion = SesionUsuario.donanteId();
            if (idSesion != null) {
                donanteId = idSesion;
            }
        } catch (Exception e) {
            System.out.println("Sesión no inicializada o usuario no logueado. Usando donanteId fallback = " + donanteId);
        }

        // 2. Consulta a los endpoints reales del backend
        try {
            // A. Obtener Misión Actual desde tu IncentivoController
            Map<String, Object> misionActualMap = restTemplate.getForObject(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/" + donanteId + "/mision-actual",
                    Map.class
            );

            // B. Obtener Métricas del Donante (Puntos acumulados)
            Map<String, Object> metricasMap = restTemplate.getForObject(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/" + donanteId + "/metricas",
                    Map.class
            );

            // Construimos la lista de misiones para la vista
            List<MisionResponseDTO> misiones = new ArrayList<>();
            if (misionActualMap != null && misionActualMap.get("nombre") != null) {
                String nombre = (String) misionActualMap.get("nombre");
                String descripcion = (String) misionActualMap.get("descripcion");
                Integer puntos = misionActualMap.get("puntosOtorgados") != null ?
                        ((Number) misionActualMap.get("puntosOtorgados")).intValue() : 10;
                Boolean completada = misionActualMap.get("completada") != null && (Boolean) misionActualMap.get("completada");

                misiones.add(new MisionResponseDTO(nombre, descripcion, puntos, completada));
            }

            // Mapeo de métricas reales
            int puntosTotales = 0;
            if (metricasMap != null && metricasMap.get("puntosTotales") != null) {
                puntosTotales = ((Number) metricasMap.get("puntosTotales")).intValue();
            }

            int puntosSiguienteNivel = 100;
            double porcentajeAvance = Math.min(100.0, (puntosTotales / (double) puntosSiguienteNivel) * 100);

            DashboardIncentivosDTO progreso = new DashboardIncentivosDTO(
                    puntosTotales,
                    puntosSiguienteNivel,
                    porcentajeAvance,
                    misiones
            );

            // C. Ranking (Mocked temporalmente hasta tener un GET /ranking-mensual expuesto)
            List<PuestoDTO> ranking = List.of(
                    new PuestoDTO(1, "sebibalas11@gmail.com", 5),
                    new PuestoDTO(2, "nicolnt@gmail.com", 3)
            );

            model.addAttribute("progreso", progreso);
            model.addAttribute("ranking", ranking);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al conectar con el servicio de incentivos: " + e.getMessage());
        }

        return "donante/incentivos";
    }
}