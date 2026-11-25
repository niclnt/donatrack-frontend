package ar.edu.utn.frba.ddsi.donatrack_frontend.config.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Rate limiting con el algoritmo Token Bucket, un balde por cliente (clave).
 * Es el mismo algoritmo que usan los servicios del backend (common-lib): el front es otro repo y no comparte esa libreria.
 *
 * <p>Cada balde arranca lleno con {@code capacidad} fichas; cada solicitud consume una y el balde se recarga de forma
 * continua a razon de {@code solicitudesPorMinuto}. Permite rafagas cortas pero acota el promedio sostenido.
 */
public class LimitadorDeSolicitudes {

    // Tope de clientes en memoria: al superarlo se descartan los baldes llenos (clientes inactivos)
    static final int MAX_BALDES = 10_000;

    private static final double NANOS_POR_MINUTO = 60_000_000_000d;

    private final long capacidad;
    private final double fichasPorNano;
    private final LongSupplier reloj;
    private final Map<String, Balde> baldes = new ConcurrentHashMap<>();

    public LimitadorDeSolicitudes(long capacidad, long solicitudesPorMinuto) {
        this(capacidad, solicitudesPorMinuto, System::nanoTime);
    }

    LimitadorDeSolicitudes(long capacidad, long solicitudesPorMinuto, LongSupplier reloj) {
        if (capacidad <= 0 || solicitudesPorMinuto <= 0) {
            throw new IllegalArgumentException("La capacidad y las solicitudes por minuto deben ser mayores a 0");
        }
        this.capacidad = capacidad;
        this.fichasPorNano = solicitudesPorMinuto / NANOS_POR_MINUTO;
        this.reloj = reloj;
    }

    public Resultado intentarConsumir(String clave) {
        if (baldes.size() > MAX_BALDES) {
            long ahora = reloj.getAsLong();
            baldes.values().removeIf(balde -> balde.estaLleno(ahora));
        }
        return baldes.computeIfAbsent(clave, c -> new Balde(capacidad, reloj.getAsLong())).consumir();
    }

    public long getCapacidad() {
        return capacidad;
    }

    public record Resultado(boolean permitida, long restantes, long segundosParaReintentar) {
    }

    private final class Balde {
        private double fichas;
        private long ultimaRecarga;

        private Balde(double fichas, long ahora) {
            this.fichas = fichas;
            this.ultimaRecarga = ahora;
        }

        private synchronized Resultado consumir() {
            recargar(reloj.getAsLong());
            if (fichas >= 1) {
                fichas -= 1;
                return new Resultado(true, (long) fichas, 0);
            }
            long segundos = (long) Math.ceil((1 - fichas) / fichasPorNano / 1_000_000_000d);
            return new Resultado(false, 0, Math.max(1, segundos));
        }

        private synchronized boolean estaLleno(long ahora) {
            recargar(ahora);
            return fichas >= capacidad;
        }

        private void recargar(long ahora) {
            long transcurrido = ahora - ultimaRecarga;
            if (transcurrido > 0) {
                fichas = Math.min(capacidad, fichas + transcurrido * fichasPorNano);
                ultimaRecarga = ahora;
            }
        }
    }
}
