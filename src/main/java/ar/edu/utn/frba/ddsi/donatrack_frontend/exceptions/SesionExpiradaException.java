package ar.edu.utn.frba.ddsi.donatrack_frontend.exceptions;

// El access token vencio y el refresh tambien: hay que volver a loguearse
public class SesionExpiradaException extends RuntimeException {
    public SesionExpiradaException() {
        super("La sesión expiró");
    }
}
