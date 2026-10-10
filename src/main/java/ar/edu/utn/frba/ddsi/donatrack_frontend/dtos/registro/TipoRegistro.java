package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro;

import java.util.Arrays;
import java.util.Optional;

// Tipos de cuenta que se pueden crear desde la web. El slug es el que va en la URL (/registro/{slug})
public enum TipoRegistro {
    PERSONA("persona", "/donantes/personaHumana"),
    ORGANIZACION("organizacion", "/donantes/personaJuridica"),
    ENTIDAD("entidad", "/donaciones/entidades-beneficiarias");

    private final String slug;
    private final String endpoint;

    TipoRegistro(String slug, String endpoint) {
        this.slug = slug;
        this.endpoint = endpoint;
    }

    public String getSlug() {
        return slug;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public static Optional<TipoRegistro> desdeSlug(String slug) {
        return Arrays.stream(values()).filter(t -> t.slug.equals(slug)).findFirst();
    }
}
