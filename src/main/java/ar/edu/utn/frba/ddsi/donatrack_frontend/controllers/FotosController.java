package ar.edu.utn.frba.ddsi.donatrack_frontend.controllers;

import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;

// Sirve las fotos de evidencia de las entregas (guardadas en logistica) para que el navegador
// no tenga que conocer la URL de logistica. Es publico: la landing puede mostrarlas sin login.
@Controller
public class FotosController {

    private final WebApiCallerService webApiCallerService;
    private final String logisticaServiceUrl;

    public FotosController(
            WebApiCallerService webApiCallerService,
            @Value("${logistica.service.url}") String logisticaServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.logisticaServiceUrl = logisticaServiceUrl;
    }

    @GetMapping("/fotos/{fotoId:[0-9a-fA-F]{24}}")
    public ResponseEntity<byte[]> foto(@PathVariable String fotoId) {
        try {
            ResponseEntity<byte[]> respuesta = webApiCallerService.getArchivoPublico(
                    logisticaServiceUrl + "/logistica/trazabilidad/fotos/" + fotoId);
            MediaType tipo = respuesta.getHeaders().getContentType();
            return ResponseEntity.ok()
                    .contentType(tipo != null ? tipo : MediaType.APPLICATION_OCTET_STREAM)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                    .body(respuesta.getBody());
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ApiException | ServicioNoDisponibleException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
