package ar.edu.utn.frba.ddsi.donatrack_frontend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

public final class SesionUsuario {

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String USUARIO_ID = "usuarioId";
    public static final String DONANTE_ID = "donanteId"; // <-- AGREGAR ESTA LÍNEA
    public static final String ROLES = "roles";

    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_DONANTE_HUMANO = "DONANTE_HUMANO";
    public static final String ROL_DONANTE_JURIDICO = "DONANTE_JURIDICO";
    public static final String ROL_BENEFICIARIO = "BENEFICIARIO";
    public static final String ROL_CHOFER = "CHOFER";

    private SesionUsuario() {
    }

    public static HttpSession actual() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        return request.getSession();
    }

    public static String accessToken() {
        return (String) actual().getAttribute(ACCESS_TOKEN);
    }

    public static String refreshToken() {
        return (String) actual().getAttribute(REFRESH_TOKEN);
    }

    public static Long donanteId() {
        Object id = actual().getAttribute(DONANTE_ID);
        return id != null ? Long.valueOf(id.toString()) : null;
    }

    public static String panelPara(List<String> roles) {
        if (roles.contains(ROL_ADMIN)) return "/admin/inicio";
        if (roles.contains(ROL_DONANTE_HUMANO) || roles.contains(ROL_DONANTE_JURIDICO)) return "/donante/mis-donaciones";
        if (roles.contains(ROL_BENEFICIARIO)) return "/ong/asignadas";
        return "/403";
    }

    public static Long usuarioId() {
        Object id = actual().getAttribute(USUARIO_ID);
        return id != null ? Long.valueOf(id.toString()) : null;
    }
}