// Recorrido del chofer: dibuja la ruta en curso y transmite la posicion del camion a logistica
// (a traves del front, que agrega el token de la sesion). Dos fuentes de posicion:
//   - GPS del celular (navigator.geolocation): el caso real. Requiere HTTPS (o localhost).
//   - Simulador: mueve el camion por las calles desde su ultima posicion (o el deposito) hasta cada
//     parada pendiente, con una velocidad acelerable para la demo.
// Los donantes y entidades ven el movimiento en su mapa de seguimiento, que se refresca solo.
(function () {
    "use strict";

    if (typeof L === "undefined") {
        return;
    }

    var TICK_MS = 2000;                 // cada cuanto se reporta una posicion
    var VELOCIDAD_KMH = 40;             // velocidad nominal del camion
    var PAUSA_EN_PARADA_MS = 5000;      // tiempo detenido en cada parada
    var INTERVALO_GPS_MS = 5000;        // con GPS real, no reportar mas seguido que esto
    var OSRM_URL = "https://router.project-osrm.org/route/v1/driving/";
    var NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=ar&q=";
    var BUENOS_AIRES = L.latLng(-34.6037, -58.3816);

    var csrfToken = contenidoMeta("_csrf");
    var csrfHeader = contenidoMeta("_csrf_header");

    document.querySelectorAll("[data-mapa-ruta]").forEach(prepararRuta);

    function contenidoMeta(nombre) {
        var meta = document.querySelector('meta[name="' + nombre + '"]');
        return meta ? meta.getAttribute("content") : null;
    }

    function aLatLng(lat, lng) {
        var la = parseFloat(lat);
        var lo = parseFloat(lng);
        if (!Number.isFinite(la) || !Number.isFinite(lo)) {
            return null;
        }
        return L.latLng(la, lo);
    }

    function esperar(ms) {
        return new Promise(function (resolver) { setTimeout(resolver, ms); });
    }

    function prepararRuta(contenedor) {
        var tarjeta = contenedor.closest("[data-ruta]");
        var rutaId = contenedor.dataset.rutaId;
        var estado = tarjeta.querySelector("[data-estado-recorrido]");
        var botonSimular = tarjeta.querySelector('[data-accion="simular"]');
        var botonGps = tarjeta.querySelector('[data-accion="gps"]');
        var selectorVelocidad = tarjeta.querySelector("[data-velocidad]");

        var deposito = aLatLng(contenedor.dataset.origenLat, contenedor.dataset.origenLng);
        var ultimaPosicion = aLatLng(contenedor.dataset.ultimaLat, contenedor.dataset.ultimaLng);

        var paradas = Array.prototype.map.call(tarjeta.querySelectorAll("[data-parada]"), function (li, i) {
            return {
                nro: i + 1,
                elemento: li,
                id: li.dataset.entregaId,
                direccion: li.dataset.direccion || ("Parada " + (i + 1)),
                pendiente: li.dataset.estado === "EN_TRASLADO",
                posicion: aLatLng(li.dataset.lat, li.dataset.lng)
            };
        });

        // --- Mapa ---
        var mapa = L.map(contenedor).setView(deposito || BUENOS_AIRES, 12);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            attribution: "&copy; OpenStreetMap contributors",
            maxZoom: 19
        }).addTo(mapa);

        if (deposito) {
            L.marker(deposito, {
                icon: L.divIcon({ className: "icono-deposito", html: "🏭", iconSize: [24, 24] }),
                title: "Depósito"
            }).addTo(mapa).bindTooltip("Depósito (origen)");
        }

        var camion = L.marker(ultimaPosicion || deposito || BUENOS_AIRES, {
            icon: L.divIcon({ className: "icono-camion", html: "🚚", iconSize: [30, 30] }),
            title: "Camión",
            zIndexOffset: 1000
        }).addTo(mapa).bindTooltip("Tu camión");

        var capaRecorrido = null;
        var simulacion = null;       // { pausada, cancelada }
        var vigilanciaGps = null;    // id de watchPosition
        var ultimoEnvioGps = 0;

        function mostrarEstado(texto) {
            estado.textContent = texto;
        }

        function dibujarParadas() {
            paradas.forEach(function (p) {
                if (!p.posicion) {
                    return;
                }
                var clase = "icono-parada" + (p.pendiente ? "" : " icono-parada--cerrada");
                L.marker(p.posicion, {
                    icon: L.divIcon({ className: "", html: '<div class="' + clase + '">' + p.nro + "</div>", iconSize: [28, 28] }),
                    title: "Parada " + p.nro
                }).addTo(mapa).bindTooltip("Parada " + p.nro + ": " + p.direccion);
            });
            var puntos = paradas.filter(function (p) { return p.posicion; }).map(function (p) { return p.posicion; });
            if (deposito) { puntos.push(deposito); }
            if (ultimaPosicion) { puntos.push(ultimaPosicion); }
            if (puntos.length > 1) {
                mapa.fitBounds(L.latLngBounds(puntos), { padding: [30, 30] });
            }
        }

        // Paradas cargadas sin coordenadas: se buscan por direccion (Nominatim pide 1 consulta por segundo)
        async function completarCoordenadas() {
            var faltantes = paradas.filter(function (p) { return p.pendiente && !p.posicion && p.direccion; });
            for (var i = 0; i < faltantes.length; i++) {
                try {
                    var respuesta = await fetch(NOMINATIM_URL + encodeURIComponent(faltantes[i].direccion));
                    var resultados = await respuesta.json();
                    if (resultados.length > 0) {
                        faltantes[i].posicion = L.latLng(parseFloat(resultados[0].lat), parseFloat(resultados[0].lon));
                    }
                } catch (e) {
                    // sin coordenadas: la parada se saltea en la simulacion
                }
                await esperar(1100);
            }
        }

        // --- Envio de posiciones ---
        async function reportarPosicion(posicion, velocidadKmh) {
            var encabezados = { "Content-Type": "application/json", "Accept": "application/json" };
            if (csrfHeader && csrfToken) {
                encabezados[csrfHeader] = csrfToken;
            }
            var respuesta;
            try {
                respuesta = await fetch("/chofer/rutas/" + encodeURIComponent(rutaId) + "/posiciones", {
                    method: "POST",
                    headers: encabezados,
                    credentials: "same-origin",
                    body: JSON.stringify({ latitud: posicion.lat, longitud: posicion.lng, velocidad: velocidadKmh })
                });
            } catch (e) {
                return { ok: false, detener: false, mensaje: "Sin conexión: se reintenta con la próxima posición." };
            }
            if (respuesta.redirected || respuesta.status === 401) {
                return { ok: false, detener: true, mensaje: "Tu sesión expiró. Volvé a iniciar sesión." };
            }
            if (respuesta.status === 409) {
                return { ok: false, detener: true, mensaje: "La ruta ya no está en curso: se dejó de transmitir la posición." };
            }
            if (respuesta.status === 429) {
                return { ok: false, detener: false, mensaje: "Demasiadas posiciones seguidas: se reintenta en unos segundos." };
            }
            if (!respuesta.ok) {
                return { ok: false, detener: false, mensaje: "Logística no respondió (se reintenta con la próxima posición)." };
            }
            return { ok: true };
        }

        // --- Trazado por calles (OSRM); si no responde, se va en linea recta entre paradas ---
        async function trazarRecorrido(puntos) {
            try {
                var coordenadas = puntos.map(function (p) { return p.lng + "," + p.lat; }).join(";");
                var respuesta = await fetch(OSRM_URL + coordenadas + "?overview=full&geometries=geojson");
                if (!respuesta.ok) {
                    throw new Error("OSRM " + respuesta.status);
                }
                var datos = await respuesta.json();
                var linea = datos.routes[0].geometry.coordinates.map(function (c) { return L.latLng(c[1], c[0]); });
                return { linea: linea, porCalles: true };
            } catch (e) {
                return { linea: puntos.slice(), porCalles: false };
            }
        }

        // Indice del punto de la linea mas cercano a cada parada (buscando hacia adelante, en orden)
        function indicesDeParadas(linea, paradasDestino) {
            var indices = [];
            var desde = 0;
            paradasDestino.forEach(function (p) {
                var mejor = desde;
                var mejorDistancia = Infinity;
                for (var i = desde; i < linea.length; i++) {
                    var d = linea[i].distanceTo(p.posicion);
                    if (d < mejorDistancia) {
                        mejorDistancia = d;
                        mejor = i;
                    }
                }
                indices.push(mejor);
                desde = mejor;
            });
            return indices;
        }

        // Avanza "metros" sobre la linea desde (indice, fraccion). Devuelve la nueva ubicacion.
        function avanzar(linea, cursor, metros) {
            var i = cursor.indice;
            var actual = cursor.punto;
            while (i < linea.length - 1) {
                var siguiente = linea[i + 1];
                var tramo = actual.distanceTo(siguiente);
                if (metros < tramo) {
                    var t = metros / tramo;
                    var punto = L.latLng(actual.lat + (siguiente.lat - actual.lat) * t,
                                         actual.lng + (siguiente.lng - actual.lng) * t);
                    return { indice: i, punto: punto };
                }
                metros -= tramo;
                i++;
                actual = siguiente;
            }
            return { indice: linea.length - 1, punto: linea[linea.length - 1] };
        }

        function marcarParadaActual(parada) {
            paradas.forEach(function (p) { p.elemento.classList.remove("parada--actual"); });
            if (parada) {
                parada.elemento.classList.add("parada--actual");
            }
        }

        async function simular() {
            await completarCoordenadas();
            var destinos = paradas.filter(function (p) { return p.pendiente && p.posicion; });
            if (destinos.length === 0) {
                mostrarEstado("No hay paradas pendientes con dirección ubicable en el mapa.");
                return;
            }
            var partida = ultimaPosicion || deposito || destinos[0].posicion;
            mostrarEstado("Calculando el recorrido por calles…");
            var recorrido = await trazarRecorrido([partida].concat(destinos.map(function (p) { return p.posicion; })));
            if (capaRecorrido) {
                mapa.removeLayer(capaRecorrido);
            }
            capaRecorrido = L.polyline(recorrido.linea, { color: "#6d5dfc", weight: 4, opacity: 0.7 }).addTo(mapa);
            mapa.fitBounds(capaRecorrido.getBounds(), { padding: [30, 30] });

            var indices = indicesDeParadas(recorrido.linea, destinos);
            var cursor = { indice: 0, punto: recorrido.linea[0] };
            var proxima = 0;
            var control = { pausada: false, cancelada: false };
            simulacion = control;
            actualizarBotones();

            while (!control.cancelada && proxima < destinos.length) {
                if (control.pausada) {
                    await esperar(500);
                    continue;
                }
                var factor = parseFloat(selectorVelocidad.value) || 1;
                var metrosPorTick = VELOCIDAD_KMH / 3.6 * (TICK_MS / 1000) * factor;
                var siguienteCursor = avanzar(recorrido.linea, cursor, metrosPorTick);
                // No pasarse de la proxima parada: el camion se detiene ahi
                var llego = siguienteCursor.indice >= indices[proxima];
                if (llego) {
                    siguienteCursor = { indice: indices[proxima], punto: recorrido.linea[indices[proxima]] };
                }
                cursor = siguienteCursor;
                camion.setLatLng(cursor.punto);

                var resultado = await reportarPosicion(cursor.punto, llego ? 0 : VELOCIDAD_KMH);
                if (resultado.detener) {
                    mostrarEstado(resultado.mensaje);
                    break;
                }
                var destino = destinos[proxima];
                marcarParadaActual(destino);
                if (llego) {
                    mostrarEstado("Llegaste a la parada " + destino.nro + " (" + destino.direccion + "). "
                        + "La entidad confirma la recepción desde su panel; si no podés entregar, usá \"No se pudo entregar\".");
                    proxima++;
                    await esperar(PAUSA_EN_PARADA_MS);
                } else {
                    mostrarEstado((resultado.ok ? "Transmitiendo posición (simulada). " : resultado.mensaje + " ")
                        + "En camino a la parada " + destino.nro + ": " + destino.direccion
                        + (recorrido.porCalles ? "" : " (trazado en línea recta)"));
                    await esperar(TICK_MS);
                }
            }

            if (!control.cancelada && proxima >= destinos.length) {
                marcarParadaActual(null);
                mostrarEstado("Recorrido completo. Las entregas pasan a \"Entregada\" cuando cada entidad confirma la recepción.");
            }
            simulacion = null;
            actualizarBotones();
        }

        // --- GPS real del celular ---
        function iniciarGps() {
            if (!("geolocation" in navigator)) {
                mostrarEstado("Este navegador no permite obtener la ubicación.");
                return;
            }
            if (!window.isSecureContext) {
                mostrarEstado("El GPS del navegador solo funciona con HTTPS. Usá el simulador o accedé por HTTPS.");
                return;
            }
            mostrarEstado("Esperando la señal del GPS…");
            vigilanciaGps = navigator.geolocation.watchPosition(async function (lectura) {
                var ahora = Date.now();
                var posicion = L.latLng(lectura.coords.latitude, lectura.coords.longitude);
                camion.setLatLng(posicion);
                if (ahora - ultimoEnvioGps < INTERVALO_GPS_MS) {
                    return;
                }
                ultimoEnvioGps = ahora;
                var velocidad = Number.isFinite(lectura.coords.speed) ? lectura.coords.speed * 3.6 : null;
                var resultado = await reportarPosicion(posicion, velocidad);
                if (resultado.detener) {
                    detenerGps();
                }
                mostrarEstado(resultado.ok
                    ? "Transmitiendo tu ubicación real (precisión ±" + Math.round(lectura.coords.accuracy) + " m)."
                    : resultado.mensaje);
            }, function (error) {
                mostrarEstado(error.code === error.PERMISSION_DENIED
                    ? "No diste permiso para usar la ubicación."
                    : "No se pudo obtener la ubicación: " + error.message);
                detenerGps();
            }, { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 });
            actualizarBotones();
        }

        function detenerGps() {
            if (vigilanciaGps !== null) {
                navigator.geolocation.clearWatch(vigilanciaGps);
                vigilanciaGps = null;
            }
            actualizarBotones();
        }

        function actualizarBotones() {
            if (simulacion) {
                botonSimular.textContent = simulacion.pausada ? "▶ Continuar" : "⏸ Pausar";
            } else {
                botonSimular.textContent = "▶ Simular recorrido";
            }
            botonGps.textContent = vigilanciaGps !== null ? "⏹ Dejar de compartir GPS" : "📍 Usar GPS del celular";
            botonGps.disabled = simulacion !== null;
            botonSimular.disabled = vigilanciaGps !== null;
        }

        botonSimular.addEventListener("click", function () {
            if (simulacion) {
                simulacion.pausada = !simulacion.pausada;
                mostrarEstado(simulacion.pausada ? "Simulación en pausa." : "Simulación reanudada.");
                actualizarBotones();
                return;
            }
            simular();
        });

        botonGps.addEventListener("click", function () {
            if (vigilanciaGps !== null) {
                detenerGps();
                mostrarEstado("Dejaste de compartir tu ubicación.");
            } else {
                iniciarGps();
            }
        });

        window.addEventListener("beforeunload", function () {
            if (simulacion) { simulacion.cancelada = true; }
            detenerGps();
        });

        dibujarParadas();
        if (ultimaPosicion) {
            mostrarEstado("Última posición reportada del camión. Podés continuar el recorrido desde ahí.");
        }
    }
})();
