package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import java.util.List;

public record EntidadBeneficiariaResponse (
  Long id,
  String razonSocial,
  String direccion,
  String localidad,
  String ciudad,
  String provincia,
  Integer telefono,
  Double latitud,
  Double longitud,
  List<Object> mediosDeContacto,
  List<Object> necesidades,
  String mail
){}
