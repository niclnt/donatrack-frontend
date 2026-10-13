package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades.NecesidadResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// @JsonIgnoreProperties evita que campos extra del backend (ej: campos nuevos que aún no mapeamos)
// rompan la deserialización con un error de tipo UnrecognizedPropertyException
@JsonIgnoreProperties(ignoreUnknown = true)
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
  List<MedioDeContactoResponse> mediosDeContacto,
  List<NecesidadResponse> necesidades,
  String mail
){}
