package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.config.SesionUsuario;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.logistica.SeguimientoEntregaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.function.Supplier;

// REQ. 9 y 14 — Seguimiento en tiempo real de los camiones (donante y entidad beneficiaria).
// La pagina se renderiza en el servidor y el mapa refresca las posiciones con /datos,
// porque el navegador no tiene el token para llamar a logistica directamente.
@Controller
public class SeguimientoController {

    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;
    private final String logisticaServiceUrl;

    public SeguimientoController(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl,
            @Value("${logistica.service.url}") String logisticaServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
        this.logisticaServiceUrl = logisticaServiceUrl;
    }

    @GetMapping("/donante/mapa")
    public String mapaDonante(Model model) {
        model.addAttribute("subtitulo", "Seguimiento en tiempo real de los camiones que llevan tus donaciones.");
        model.addAttribute("urlDatos", "/donante/mapa/datos");
        model.addAttribute("esEntidad", false);
        return cargarMapa(model, this::seguimientoDelDonante);
    }

    @GetMapping("/donante/mapa/datos")
    @ResponseBody
    public ResponseEntity<List<SeguimientoEntregaResponse>> datosDonante() {
        return datos(this::seguimientoDelDonante);
    }

    @GetMapping("/ong/mapa")
    public String mapaEntidad(Model model) {
        model.addAttribute("subtitulo", "Seguimiento en tiempo real de los camiones que traen donaciones a tu entidad.");
        model.addAttribute("urlDatos", "/ong/mapa/datos");
        model.addAttribute("esEntidad", true);
        return cargarMapa(model, this::seguimientoDeLaEntidad);
    }

    @GetMapping("/ong/mapa/datos")
    @ResponseBody
    public ResponseEntity<List<SeguimientoEntregaResponse>> datosEntidad() {
        return datos(this::seguimientoDeLaEntidad);
    }

    private String cargarMapa(Model model, Supplier<List<SeguimientoEntregaResponse>> seguimiento) {
        try {
            model.addAttribute("entregas", seguimiento.get());
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException | IllegalStateException e) {
            model.addAttribute("entregas", List.of());
            model.addAttribute("error", "No se pudo cargar el seguimiento: " + e.getMessage());
        }
        return "seguimiento/mapa";
    }

    private ResponseEntity<List<SeguimientoEntregaResponse>> datos(Supplier<List<SeguimientoEntregaResponse>> seguimiento) {
        try {
            return ResponseEntity.ok(seguimiento.get());
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private List<SeguimientoEntregaResponse> seguimientoDelDonante() {
        Long donanteId = SesionUsuario.donanteId();
        if (donanteId == null) {
            throw new IllegalStateException("no hay un perfil de donante asociado a tu cuenta");
        }
        return webApiCallerService.getList(
                logisticaServiceUrl + "/logistica/monitoreo/donantes/" + donanteId + "/seguimiento",
                SeguimientoEntregaResponse[].class);
    }

    private List<SeguimientoEntregaResponse> seguimientoDeLaEntidad() {
        EntidadBeneficiariaResponse entidad = webApiCallerService.get(
                donacionesServiceUrl + "/donaciones/entidades-beneficiarias/usuario/" + SesionUsuario.usuarioId(),
                EntidadBeneficiariaResponse.class);
        return webApiCallerService.getList(
                logisticaServiceUrl + "/logistica/monitoreo/beneficiarios/" + entidad.id() + "/seguimiento",
                SeguimientoEntregaResponse[].class);
    }
}
