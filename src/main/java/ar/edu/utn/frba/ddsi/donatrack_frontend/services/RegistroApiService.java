package ar.edu.utn.frba.ddsi.donatrack_frontend.services;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.EntidadBeneficiariaRegistroRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.MedioDeContactoDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.PersonaHumanaRegistroRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.PersonaJuridicaRegistroRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.RegistroForm;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.TipoRegistro;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Alta de cuentas desde la web. Donaciones crea el donante/entidad y su usuario en el auth-service
// en la misma transaccion, por eso el front no llama al auth directamente.
@Service
public class RegistroApiService {

    static final int LARGO_MINIMO_PASSWORD = 8;
    private static final Set<String> MEDIOS = Set.of("EMAIL", "TELEFONO", "WHATSAPP");
    private static final Set<String> TIPOS_JURIDICOS = Set.of("EMPRESA", "ONG", "GUBERNAMENTAL", "INSTITUCION");
    private static final Set<String> GENEROS = Set.of("F", "M", "X");

    private final WebApiCallerService webApiCallerService;
    private final String donacionesServiceUrl;

    public RegistroApiService(WebApiCallerService webApiCallerService,
                              @Value("${donaciones.service.url}") String donacionesServiceUrl) {
        this.webApiCallerService = webApiCallerService;
        this.donacionesServiceUrl = donacionesServiceUrl;
    }

    public void validar(TipoRegistro tipo, RegistroForm form, Errors errores) {
        switch (tipo) {
            case PERSONA -> validarPersona(form, errores);
            case ORGANIZACION -> validarOrganizacion(form, errores);
            case ENTIDAD -> validarEntidad(form, errores);
        }
        validarCuenta(form, errores);
    }

    public void registrar(TipoRegistro tipo, RegistroForm form) {
        Object body = switch (tipo) {
            case PERSONA -> personaRequest(form);
            case ORGANIZACION -> organizacionRequest(form);
            case ENTIDAD -> entidadRequest(form);
        };
        webApiCallerService.postPublico(donacionesServiceUrl + tipo.getEndpoint(), body, Void.class);
    }

    private void validarPersona(RegistroForm form, Errors errores) {
        requerido(form.getNombre(), "nombre", "Ingresá tu nombre y apellido", errores);
        Integer edad = entero(form.getEdad());
        if (edad == null || edad < 1 || edad > 120) {
            errores.rejectValue("edad", "invalida", "Ingresá una edad válida");
        }
        if (requerido(form.getDocumento(), "documento", "Ingresá tu número de documento", errores)
                && !form.getDocumento().trim().matches("\\d{6,9}")) {
            errores.rejectValue("documento", "invalido", "Usá solo números, sin puntos");
        }
        if (!valorPermitido(GENEROS, form.getGenero())) {
            errores.rejectValue("genero", "requerido", "Elegí una opción");
        }
        requerido(form.getDireccion(), "direccion", "Ingresá tu dirección", errores);
        if (!valorPermitido(MEDIOS, form.getMedioPreferido())) {
            errores.rejectValue("medioPreferido", "invalido", "Elegí cómo querés que te contactemos");
        } else if (!"EMAIL".equals(form.getMedioPreferido()) && vacio(form.getTelefono())) {
            errores.rejectValue("telefono", "requerido", "Para que te contactemos por teléfono o WhatsApp necesitamos tu número");
        }
        validarTelefonoOpcional(form, errores);
    }

    private void validarOrganizacion(RegistroForm form, Errors errores) {
        requerido(form.getRazonSocial(), "razonSocial", "Ingresá la razón social", errores);

        if (!valorPermitido(TIPOS_JURIDICOS, form.getTipoJuridico())) {
            errores.rejectValue("tipoJuridico", "invalido", "Elegí el tipo de organización");
        }
        requerido(form.getRubro(), "rubro", "Ingresá el rubro", errores);

        if (!valorPermitido(MEDIOS, form.getMedioPreferido())) {
            errores.rejectValue(
                "medioPreferido",
                "invalido",
                "Elegí cómo querés que te contactemos");
        } else if (!"EMAIL".equals(form.getMedioPreferido())
            && vacio(form.getTelefono())) {
            errores.rejectValue(
                "telefono",
                "requerido",
                "Para usar ese medio necesitamos un teléfono");
        }

        validarTelefonoOpcional(form, errores);
    }

    private void validarEntidad(RegistroForm form, Errors errores) {
        requerido(form.getRazonSocial(), "razonSocial", "Ingresá el nombre de la entidad", errores);
        requerido(form.getDireccion(), "direccion", "Ingresá la dirección", errores);
        requerido(form.getLocalidad(), "localidad", "Ingresá la localidad o barrio", errores);
        requerido(form.getCiudad(), "ciudad", "Ingresá la ciudad o partido", errores);
        requerido(form.getProvincia(), "provincia", "Ingresá la provincia", errores);
        if (requerido(form.getTelefono(), "telefono", "Ingresá un teléfono de contacto", errores)) {
            validarTelefonoOpcional(form, errores);
        }
    }

    private void validarCuenta(RegistroForm form, Errors errores) {
        if (requerido(form.getMail(), "mail", "Ingresá tu correo electrónico", errores)
                && !form.getMail().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            errores.rejectValue("mail", "invalido", "El correo no parece válido");
        }
        if (form.getPassword() == null || form.getPassword().length() < LARGO_MINIMO_PASSWORD) {
            errores.rejectValue("password", "corta", "La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres");
        } else if (!form.getPassword().equals(form.getConfirmacion())) {
            errores.rejectValue("confirmacion", "distinta", "Las contraseñas no coinciden");
        }
    }

    // Donaciones guarda el telefono de la entidad como int: lo validamos aca para no llegar con un 500
    private void validarTelefonoOpcional(RegistroForm form, Errors errores) {
        if (!vacio(form.getTelefono()) && entero(soloDigitos(form.getTelefono())) == null) {
            errores.rejectValue("telefono", "invalido", "Usá solo números, sin 0 ni 15 (hasta 9 dígitos)");
        }
    }

    private PersonaHumanaRegistroRequest personaRequest(RegistroForm form) {
        String mail = form.getMail().trim();
        String telefono = vacio(form.getTelefono()) ? null : soloDigitos(form.getTelefono());

        // Donaciones exige al menos un EMAIL entre los medios de contacto
        String medio = form.getMedioPreferido();
        List<MedioDeContactoDTO> medios = new ArrayList<>();
        medios.add(new MedioDeContactoDTO("EMAIL", mail));
        if (telefono != null) {
            medios.add(new MedioDeContactoDTO("TELEFONO", telefono));
            if ("WHATSAPP".equals(medio)) {
                medios.add(new MedioDeContactoDTO("WHATSAPP", telefono));
            }
        }
        MedioDeContactoDTO preferido = new MedioDeContactoDTO(medio, "EMAIL".equals(medio) ? mail : telefono);

        return new PersonaHumanaRegistroRequest(form.getNombre().trim(), entero(form.getEdad()), form.getTipoDoc(),
                form.getDocumento().trim(), form.getGenero(), form.getDireccion().trim(), medios, preferido,
                mail, form.getPassword());
    }

    private PersonaJuridicaRegistroRequest organizacionRequest(RegistroForm form) {
        String mail = form.getMail().trim();
        String telefono = vacio(form.getTelefono())
            ? null
            : soloDigitos(form.getTelefono());

        List<MedioDeContactoDTO> medios = new ArrayList<>();
        medios.add(new MedioDeContactoDTO("EMAIL", mail));

        if (telefono != null) {
            medios.add(new MedioDeContactoDTO("TELEFONO", telefono));

            if ("WHATSAPP".equals(form.getMedioPreferido())) {
                medios.add(new MedioDeContactoDTO("WHATSAPP", telefono));
            }
        }

        MedioDeContactoDTO preferido = new MedioDeContactoDTO(
            form.getMedioPreferido(),
            "EMAIL".equals(form.getMedioPreferido()) ? mail : telefono
        );
        return new PersonaJuridicaRegistroRequest(
            form.getRazonSocial().trim(),
            form.getTipoJuridico(),
            medios,
            preferido,
            form.getRubro().trim(),
            mail,
            form.getPassword());
    }

    private EntidadBeneficiariaRegistroRequest entidadRequest(RegistroForm form) {
        String mail = form.getMail().trim();
        String telefono = soloDigitos(form.getTelefono());
        List<MedioDeContactoDTO> medios = List.of(
                new MedioDeContactoDTO("EMAIL", mail),
                new MedioDeContactoDTO("TELEFONO", telefono));

        return new EntidadBeneficiariaRegistroRequest(form.getRazonSocial().trim(), form.getDireccion().trim(),
                form.getLocalidad().trim(), form.getCiudad().trim(), form.getProvincia().trim(),
                Integer.parseInt(telefono), medios, mail, form.getPassword());
    }

    private static boolean requerido(String valor, String campo, String mensaje, Errors errores) {
        if (vacio(valor)) {
            errores.rejectValue(campo, "requerido", mensaje);
            return false;
        }
        return true;
    }

    // Set.of(...).contains(null) tira NullPointerException
    private static boolean valorPermitido(Set<String> permitidos, String valor) {
        return valor != null && permitidos.contains(valor);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    // Permite escribir el telefono con espacios o guiones
    private static String soloDigitos(String valor) {
        return valor.replaceAll("[\\s-]", "");
    }

    private static Integer entero(String valor) {
        if (valor == null) return null;
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
