package ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String mensaje) {
        super(mensaje);
    }
}
