package ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions;

// Error de negocio informado por un servicio (400, 403, 409...), con el mensaje que devolvio
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
