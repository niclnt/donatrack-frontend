package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.necesidades;

public record NecesidadResponse (
    Long id,
    String categoriaNombre,
    String subcategoriaNombre,
    Long entidadBeneficiariaId,
    String entidadBeneficiariaNombre,
    String descripcion,
    int cantidadSolicitada,
    int cantidadRecibida,
    boolean satisfecha,
    String tipoNecesidad,
    Integer cantidadDias,
    String situacion
) {
}
