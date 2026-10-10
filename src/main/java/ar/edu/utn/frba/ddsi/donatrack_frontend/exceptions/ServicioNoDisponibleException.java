package ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions;

public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
