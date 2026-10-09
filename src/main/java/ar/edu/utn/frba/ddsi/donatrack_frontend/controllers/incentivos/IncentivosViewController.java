package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers.incentivos;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.DashboardIncentivosDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.incentivos.PuestoDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Controller
@RequestMapping("/donante/incentivos")
public class IncentivosViewController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String INCENTIVOS_SERVICE_URL = "http://localhost:8082/incentivos-service"; // Ajustar si el puerto de incentivos-service es distinto

    @GetMapping
    public String verIncentivos(Model model) {
        String usernameMock = "sebibalas11@gmail.com"; // Mock temporal hasta integrar el token/SSO de Lean

        try {
            // 1. Obtener misiones y progreso del donante
            DashboardIncentivosDTO progreso = restTemplate.getForObject(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/mis-misiones?username=" + usernameMock,
                    DashboardIncentivosDTO.class
            );

            // 2. Obtener el ranking mensual de la comunidad
            List<PuestoDTO> ranking = restTemplate.exchange(
                    INCENTIVOS_SERVICE_URL + "/incentivos/donantes/destacados/ranking-mensual",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<PuestoDTO>>() {}
            ).getBody();

            model.addAttribute("progreso", progreso);
            model.addAttribute("ranking", ranking);

        } catch (Exception e) {
            model.addAttribute("error", "No se pudo conectar con el servicio de incentivos.");
        }

        return "donante/incentivos"; // Apunta a src/main/resources/templates/donante/incentivos.html
    }
}