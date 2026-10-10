package ar.edu.utn.frba.ddsi.donatrack_frontend.services;

import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.EntidadBeneficiariaRegistroRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.MedioDeContactoDTO;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.PersonaHumanaRegistroRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.PersonaJuridicaRegistroRequest;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.RegistroForm;
import ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro.TipoRegistro;
import ar.edu.utn.frba.ddsi.donatrack_frontend.services.internal.WebApiCallerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RegistroApiServiceTest {

    private static final String DONACIONES = "http://donaciones";

    private WebApiCallerService caller;
    private RegistroApiService service;

    @BeforeEach
    void setUp() {
        caller = mock(WebApiCallerService.class);
        service = new RegistroApiService(caller, DONACIONES);
    }

    @Test
    void personaValidaNoTieneErrores() {
        assertFalse(validar(TipoRegistro.PERSONA, personaValida()).hasErrors());
    }

    @Test
    void personaSinDatosMarcaCadaCampo() {
        Errors errores = validar(TipoRegistro.PERSONA, new RegistroForm());

        for (String campo : List.of("nombre", "edad", "documento", "genero", "direccion", "mail", "password")) {
            assertTrue(errores.hasFieldErrors(campo), "falta error en " + campo);
        }
    }

    @Test
    void contraseniasDistintasSeRechazan() {
        RegistroForm form = personaValida();
        form.setConfirmacion("otraCosa123");

        assertTrue(validar(TipoRegistro.PERSONA, form).hasFieldErrors("confirmacion"));
    }

    @Test
    void whatsappComoPreferidoExigeTelefono() {
        RegistroForm form = personaValida();
        form.setMedioPreferido("WHATSAPP");
        form.setTelefono("");

        assertTrue(validar(TipoRegistro.PERSONA, form).hasFieldErrors("telefono"));
    }

    @Test
    void telefonoDeEntidadQueNoEntraEnUnIntSeRechaza() {
        RegistroForm form = entidadValida();
        form.setTelefono("2214567890123");

        assertTrue(validar(TipoRegistro.ENTIDAD, form).hasFieldErrors("telefono"));
    }

    @Test
    void personaSeMandaConEmailEntreLosMediosYElPreferido() {
        RegistroForm form = personaValida();
        form.setMedioPreferido("WHATSAPP");
        form.setTelefono("11 4455-6677");

        service.registrar(TipoRegistro.PERSONA, form);

        PersonaHumanaRegistroRequest body = capturarBody(DONACIONES + "/donantes/personaHumana", PersonaHumanaRegistroRequest.class);
        assertEquals("ana@test.com", body.mail());
        assertEquals(30, body.edad());
        assertTrue(body.mediosDeContacto().contains(new MedioDeContactoDTO("EMAIL", "ana@test.com")));
        assertEquals(new MedioDeContactoDTO("WHATSAPP", "1144556677"), body.medioDeContactoPreferido());
    }

    @Test
    void organizacionVaAlEndpointDePersonaJuridica() {
        RegistroForm form = new RegistroForm();
        form.setRazonSocial(" Arcor S.A. ");
        form.setTipoJuridico("EMPRESA");
        form.setRubro("Alimenticio");
        completarCuenta(form);
        assertFalse(validar(TipoRegistro.ORGANIZACION, form).hasErrors());

        service.registrar(TipoRegistro.ORGANIZACION, form);

        PersonaJuridicaRegistroRequest body = capturarBody(DONACIONES + "/donantes/personaJuridica", PersonaJuridicaRegistroRequest.class);
        assertEquals("Arcor S.A.", body.razonSocial());
        assertEquals("EMPRESA", body.tipoJuridico());
    }

    @Test
    void entidadMandaElTelefonoComoNumero() {
        service.registrar(TipoRegistro.ENTIDAD, entidadValida());

        EntidadBeneficiariaRegistroRequest body = capturarBody(DONACIONES + "/donaciones/entidades-beneficiarias", EntidadBeneficiariaRegistroRequest.class);
        assertEquals(49191234, body.telefono());
        assertTrue(body.mediosDeContacto().contains(new MedioDeContactoDTO("EMAIL", "ana@test.com")));
    }

    private Errors validar(TipoRegistro tipo, RegistroForm form) {
        Errors errores = new BeanPropertyBindingResult(form, "registro");
        service.validar(tipo, form, errores);
        return errores;
    }

    private <T> T capturarBody(String url, Class<T> tipo) {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(caller).postPublico(eq(url), body.capture(), eq(Void.class));
        return tipo.cast(body.getValue());
    }

    private static RegistroForm personaValida() {
        RegistroForm form = new RegistroForm();
        form.setNombre("Ana Gómez");
        form.setEdad("30");
        form.setDocumento("38451923");
        form.setGenero("F");
        form.setDireccion("Av. Medrano 951");
        completarCuenta(form);
        return form;
    }

    private static RegistroForm entidadValida() {
        RegistroForm form = new RegistroForm();
        form.setRazonSocial("Comedor Test");
        form.setDireccion("Plumerillo 3450");
        form.setLocalidad("Villa Soldati");
        form.setCiudad("CABA");
        form.setTelefono("4919-1234");
        completarCuenta(form);
        return form;
    }

    private static void completarCuenta(RegistroForm form) {
        form.setMail("ana@test.com");
        form.setPassword("clave1234");
        form.setConfirmacion("clave1234");
    }
}
