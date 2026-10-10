package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos;

import java.time.LocalDateTime;

public record EstadoDTO (
  Long id,
  String estado,
  LocalDateTime fecha,
  String motivo
){}
