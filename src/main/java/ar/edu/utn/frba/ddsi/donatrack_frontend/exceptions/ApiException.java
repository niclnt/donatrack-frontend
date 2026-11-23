package ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions;

import java.util.List;
import java.util.Map;

// Error de negocio informado por un servicio (400, 403, 409...), con el mensaje que devolvio
public class ApiException extends RuntimeException {

    private final int status;
    // Errores de validacion por campo (campo -> mensaje), si el servicio los mando
    private final Map<String, String> errores;

    public ApiException(int status, String mensaje) {
        this(status, mensaje, Map.of());
    }

    public ApiException(int status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores != null ? errores : Map.of();
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getErrores() {
        return errores;
    }

    // Para la vista: los mensajes por campo, o el mensaje general si no vinieron
    public List<String> mensajesParaMostrar() {
        return errores.isEmpty() ? List.of(getMessage()) : List.copyOf(errores.values());
    }
}
