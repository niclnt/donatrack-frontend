package ar.edu.utn.frba.ddsi.donatrack_frontend.services;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class DonacionesGraphQlService {

    private final WebApiCallerService webApiCallerService;
    private final String graphqlUrl;

    public DonacionesGraphQlService(
            WebApiCallerService webApiCallerService,
            @Value("${donaciones.service.url}") String donacionesServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.graphqlUrl = donacionesServiceUrl + "/graphql";
    }

    // Consulta 1: Trae entidades y sus necesidades anidadas en una única llamada HTTP (evita N+1)
    public List<EntidadBeneficiariaResponse> listarEntidadesConNecesidades() {
        String query = """
                query {
                  entidades {
                    id
                    razonSocial
                    direccion
                    localidad
                    ciudad
                    provincia
                    telefono
                    latitud
                    longitud
                    mailUsuario
                    necesidades {
                      id
                      descripcion
                      cantidadSolicitada
                      cantidadRecibida
                      satisfecha
                      fechaCreacion
                      subcategoria {
                        nombre
                      }
                      categoriaNombre
                      tipoNecesidad
                      cantidadDias
                      situacion
                      inicioPeriodo
                      finPeriodo
                    }
                  }
                }
                """;

        EntidadesGraphQlResponse respuesta = webApiCallerService.post(
                graphqlUrl,
                Map.of("query", query),
                EntidadesGraphQlResponse.class
        );

        validarErrores(respuesta != null ? respuesta.errors() : null);

        if (respuesta == null || respuesta.data() == null || respuesta.data().entidades() == null) {
            return List.of();
        }
        return respuesta.data().entidades().stream()
                .map(EntidadGql::toResponse)
                .toList();
    }

    // Consulta 2: Usa FiltroNecesidadInput para filtrar dinámicamente por entidadId
    public List<NecesidadResponse> listarNecesidadesPorEntidad(Long entidadId) {
        String query = """
                query($filtro: FiltroNecesidadInput) {
                  necesidades(filtro: $filtro) {
                    id
                    descripcion
                    cantidadSolicitada
                    cantidadRecibida
                    satisfecha
                    fechaCreacion
                    subcategoria {
                      nombre
                    }
                    entidad {
                      id
                      razonSocial
                    }
                    categoriaNombre
                    tipoNecesidad
                    cantidadDias
                    situacion
                    inicioPeriodo
                    finPeriodo
                  }
                }
                """;

        Map<String, Object> body = Map.of(
                "query", query,
                "variables", Map.of("filtro", Map.of("entidadId", entidadId))
        );

        NecesidadesGraphQlResponse respuesta = webApiCallerService.post(
                graphqlUrl,
                body,
                NecesidadesGraphQlResponse.class
        );

        validarErrores(respuesta != null ? respuesta.errors() : null);

        if (respuesta == null || respuesta.data() == null || respuesta.data().necesidades() == null) {
            return List.of();
        }
        return respuesta.data().necesidades().stream()
                .map(NecesidadGql::toResponse)
                .toList();
    }

    // En GraphQL los errores de ejecución viajan con HTTP 200 dentro del array "errors"
    private void validarErrores(List<GraphQlError> errors) {
        if (errors != null && !errors.isEmpty()) {
            String mensaje = errors.get(0).message();
            throw new ApiException(500, mensaje != null ? mensaje : "Error en consulta GraphQL");
        }
    }

    // --- Proyecciones internas para deserializar el JSON de GraphQL ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphQlError(String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EntidadesGraphQlResponse(EntidadesData data, List<GraphQlError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EntidadesData(List<EntidadGql> entidades) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NecesidadesGraphQlResponse(NecesidadesData data, List<GraphQlError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NecesidadesData(List<NecesidadGql> necesidades) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SubcategoriaGql(Long id, String nombre) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EntidadRefGql(Long id, String razonSocial) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NecesidadGql(
            Long id,
            String descripcion,
            Integer cantidadSolicitada,
            Integer cantidadRecibida,
            Boolean satisfecha,
            String fechaCreacion,
            SubcategoriaGql subcategoria,
            EntidadRefGql entidad,
            String categoriaNombre,
            String tipoNecesidad,
            Integer cantidadDias,
            String situacion,
            String inicioPeriodo,
            String finPeriodo
    ) {
        NecesidadResponse toResponse() {
            return new NecesidadResponse(
                    id,
                    categoriaNombre != null ? categoriaNombre : "",
                    subcategoria != null ? subcategoria.nombre() : "",
                    entidad != null ? entidad.id() : null,
                    entidad != null ? entidad.razonSocial() : "",
                    descripcion,
                    cantidadSolicitada != null ? cantidadSolicitada : 0,
                    cantidadRecibida != null ? cantidadRecibida : 0,
                    Boolean.TRUE.equals(satisfecha),
                    tipoNecesidad,
                    cantidadDias,
                    situacion,
                    fecha(fechaCreacion),
                    fecha(inicioPeriodo),
                    fecha(finPeriodo)
            );
        }

        private static LocalDate fecha(String texto) {
            return texto != null && !texto.isBlank() ? LocalDate.parse(texto) : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EntidadGql(
            Long id,
            String razonSocial,
            String direccion,
            String localidad,
            String ciudad,
            String provincia,
            Integer telefono,
            Double latitud,
            Double longitud,
            String mailUsuario,
            List<NecesidadGql> necesidades
    ) {
        EntidadBeneficiariaResponse toResponse() {
            List<NecesidadResponse> necesidadesResp = necesidades == null
                    ? List.of()
                    : necesidades.stream().map(NecesidadGql::toResponse).toList();
            return new EntidadBeneficiariaResponse(
                    id,
                    razonSocial,
                    direccion,
                    localidad,
                    ciudad,
                    provincia,
                    telefono,
                    latitud,
                    longitud,
                    List.of(),
                    necesidadesResp,
                    mailUsuario
            );
        }
    }
}