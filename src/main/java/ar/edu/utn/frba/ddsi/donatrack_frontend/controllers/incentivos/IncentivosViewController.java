package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

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
        Long donanteIdMock = 1L; // ID de un donante existente en tu DB

        try {
            // 1. Llamada al endpoint real de tu backend
            Map<String, Object> misionActualMap = restTemplate.getForObject(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/" + donanteIdMock + "/mision-actual",
                    Map.class
            );

            // Construimos el DTO que espera la vista
            List<MisionResponseDTO> misiones = new ArrayList<>();
            if (misionActualMap != null && misionActualMap.get("nombre") != null) {
                misiones.add(new MisionResponseDTO(
                        (String) misionActualMap.get("nombre"),
                        (String) misionActualMap.get("descripcion"),
                        misionActualMap.get("puntosOtorgados") != null ? (Integer) misionActualMap.get("puntosOtorgados") : 10,
                        false
                ));
            }

            DashboardIncentivosDTO progreso = new DashboardIncentivosDTO(
                    62, // Puntos acumulados mock
                    100, // Puntos siguiente nivel
                    62.0, // % avance
                    misiones
            );

            // 2. Ranking simulado o vacío temporalmente mientras agregás el GET de ranking
            List<PuestoDTO> ranking = List.of(
                    new PuestoDTO(1, "sebibalas11@gmail.com", 5),
                    new PuestoDTO(2, "nicolnt@gmail.com", 3)
            );

            model.addAttribute("progreso", progreso);
            model.addAttribute("ranking", ranking);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al conectar con el backend: " + e.getMessage());
        }

        return "donante/incentivos";
    }
}