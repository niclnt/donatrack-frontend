package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.donaciones;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// DTO espejo del backend MedioDeContactoResponse.
// Usamos String para tipoContacto (en vez del enum del backend) porque el JSON
// lo serializa como String ("EMAIL", "TELEFONO", "WHATSAPP") y así evitamos
// tener que copiar el enum en el frontend.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedioDeContactoResponse(
    Long id,
    String valor,
    String tipoContacto
) {}
