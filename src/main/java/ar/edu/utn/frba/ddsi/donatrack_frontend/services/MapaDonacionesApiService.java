package ar.edu.utn.frba.ddsi.donatrack_frontend.services;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.DonacionEntregadaMapaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.FotosDonacionResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.NotFoundException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ServicioNoDisponibleException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Donaciones entregadas para las paginas publicas (mapa y landing). Los datos salen de donaciones
// y las fotos de la recepcion de logistica: se cruzan por el id de la donacion
@Service
public class MapaDonacionesApiService {

    private static final Logger log = LoggerFactory.getLogger(MapaDonacionesApiService.class);

    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;
    private final String logisticaServiceUrl;

    public MapaDonacionesApiService(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl,
            @Value("${logistica.service.url}") String logisticaServiceUrl) {

        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
        this.logisticaServiceUrl = logisticaServiceUrl;
    }

    public List<DonacionEntregadaMapaResponse> obtenerDonacionesEntregadas() {
        List<DonacionEntregadaMapaResponse> entregadas = webApiCallerService.getListPublica(
                donacionesServiceUrl + "/donaciones-service/donaciones/mapa/entregadas",
                DonacionEntregadaMapaResponse[].class
        );

        Map<Long, List<String>> fotosPorDonacion = obtenerFotosPorDonacion();
        return entregadas.stream()
                .map(d -> d.conFotos(fotosPorDonacion.get(d.id())))
                .toList();
    }

    // Las entregadas desde hace 'dias' dias, de la mas nueva a la mas vieja
    public List<DonacionEntregadaMapaResponse> obtenerEntregadasRecientes(int dias, int maximo) {
        LocalDate desde = LocalDate.now().minusDays(dias);
        return obtenerDonacionesEntregadas().stream()
                .filter(d -> d.fechaEntrega() != null && !d.fechaEntrega().isBefore(desde))
                .sorted(Comparator.comparing(DonacionEntregadaMapaResponse::fechaEntrega).reversed())
                .limit(maximo)
                .toList();
    }

    // Si logistica no responde se muestran las donaciones igual, sin fotos
    private Map<Long, List<String>> obtenerFotosPorDonacion() {
        try {
            return webApiCallerService.getListPublica(
                            logisticaServiceUrl + "/logistica/trazabilidad/fotos-entregadas",
                            FotosDonacionResponse[].class)
                    .stream()
                    .collect(Collectors.toMap(FotosDonacionResponse::idDonacion, FotosDonacionResponse::fotos, (a, b) -> a));
        } catch (ApiException | NotFoundException | ServicioNoDisponibleException e) {
            log.warn("No se pudieron obtener las fotos de las entregas desde logistica: {}", e.getMessage());
            return Map.of();
        }
    }
}
