package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record NotificacionResponse (
    Long id,
    String titulo,
    String mensaje,
    String medio,
    String estado,
    LocalDateTime fechaEnvio,
    Long destinatarioIdExterno,
    String destinatarioNombre,
    List<EstadoDTO> historialEstados
){}