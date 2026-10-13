package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.importacion;

import java.util.List;

// Respuesta de POST /donantes/importar (donaciones-service)
public record ResultadoImportacionResponse(
        int filasLeidas,
        int donantesCreados,
        int usuariosCreados,
        List<FilaOmitida> omitidas
) {
    public record FilaOmitida(String mail, String motivo) {
    }

    public int cantidadOmitidas() {
        return omitidas == null ? 0 : omitidas.size();
    }
}
