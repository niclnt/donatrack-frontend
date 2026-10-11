package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades;

public record NecesidadCreateRequest (
  String categoriaNombre,
  String subcategoriaNombre,
  Long entidadBeneficiariaId,
  String descripcion,
  int cantidadSolicitada,
  String tipoNecesidad,
  Integer cantidadDias,
  String situacion
){
}
