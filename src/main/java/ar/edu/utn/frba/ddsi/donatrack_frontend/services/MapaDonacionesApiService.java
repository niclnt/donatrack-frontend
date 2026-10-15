package ar.edu.utn.frba.ddsi.donatrack_frontend.services;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.DonacionEntregadaMapaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MapaDonacionesApiService {

    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;

    public MapaDonacionesApiService(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl) {

        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
    }

    public List<DonacionEntregadaMapaResponse> obtenerDonacionesEntregadas() {

        return webApiCallerService.getListPublica(
                donacionesServiceUrl + "/donaciones-service/donaciones/mapa/entregadas",
                DonacionEntregadaMapaResponse[].class
        );
    }
}