package ar.edu.utn.frba.ddsi.donatrack_frontend.config;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component("estados")
public class EstadosVista {

    private static final Map<String, String> TEXTOS = Map.ofEntries(
            Map.entry("EN_DEPOSITO", "En depósito"),
            Map.entry("ASIGNACION_REALIZADA", "Asignación realizada"),
            Map.entry("LISTA_PARA_ENTREGAR", "Lista para entregar"),
            Map.entry("EN_TRASLADO", "En traslado"),
            Map.entry("ENTREGADA", "Entregada"),
            Map.entry("ENTREGA_FALLIDA", "Entrega fallida"),
            Map.entry("VENCIDA", "Vencida"),
            Map.entry("DESCARTADA", "Descartada"),
            Map.entry("PENDIENTE_RECEPCION", "Pendiente de recepción"),
            Map.entry("PENDIENTE", "Pendiente"),
            Map.entry("NO_RECIBIDA", "No recibida"),
            Map.entry("ENVIADA", "Enviada"),
            Map.entry("FALLIDA", "Fallida"));

    public String texto(String estado) {
        return estado == null ? "" : TEXTOS.getOrDefault(estado, estado);
    }
}
