// Mapa de seguimiento: la lista de entregas viene renderizada por el servidor
// y aca solo se dibujan los camiones y se refresca su posicion periodicamente.
(function () {
    var contenedor = document.getElementById("mapa");
    if (!contenedor || typeof L === "undefined") {
        return;
    }

    var INTERVALO_MS = 15000;
    var urlDatos = contenedor.dataset.url;
    var estadoActualizacion = document.getElementById("estado-actualizacion");
    var tarjetas = document.querySelectorAll("[data-entrega]");

    // Entregas y estados que mostro el servidor: si cambian, se recarga la pagina para re-renderizar la lista
    var firmaInicial = Array.prototype.map.call(tarjetas, function (t) {
        return t.dataset.entrega + ":" + t.dataset.estado;
    }).sort().join("|");

    // Inicializa el mapa en Buenos Aires
    var mapa = L.map(contenedor).setView([-34.6037, -58.3816], 11);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(mapa);

    var marcadores = {};
    var primeraCarga = true;

    function tieneCoordenadas(posicion) {
        return posicion != null
            && Number.isFinite(posicion.latitud) && Number.isFinite(posicion.longitud)
            && Math.abs(posicion.latitud) <= 90 && Math.abs(posicion.longitud) <= 180;
    }

    function textoUltimaPosicion(posicion) {
        if (posicion == null) {
            return "El camión todavía no reportó su posición";
        }
        var segundos = posicion.segundosDesdeActualizacion;
        var texto = segundos < 60
            ? "Última posición: hace instantes"
            : "Última posición: hace " + Math.floor(segundos / 60) + " min";
        return posicion.desactualizado ? texto + " (señal desactualizada)" : texto;
    }

    function contenidoPopup(entrega) {
        var contenido = document.createElement("div");
        var titulo = document.createElement("strong");
        titulo.textContent = "Camión " + entrega.patente;
        var destino = document.createElement("p");
        destino.textContent = "Destino: " + entrega.direccionDestino;
        var ultima = document.createElement("p");
        ultima.textContent = textoUltimaPosicion(entrega.ultimaPosicion);
        contenido.appendChild(titulo);
        contenido.appendChild(destino);
        contenido.appendChild(ultima);
        if (Number.isFinite(entrega.ultimaPosicion.velocidad)) {
            var velocidad = document.createElement("p");
            velocidad.textContent = "Velocidad: " + Math.round(entrega.ultimaPosicion.velocidad) + " km/h";
            contenido.appendChild(velocidad);
        }
        return contenido;
    }

    function dibujar(entregas) {
        var firma = entregas.map(function (e) {
            return e.entregaId + ":" + e.estadoEntrega;
        }).sort().join("|");
        if (firma !== firmaInicial) {
            window.location.reload();
            return;
        }

        var vigentes = {};
        entregas.forEach(function (entrega) {
            var tarjeta = document.querySelector('[data-entrega="' + CSS.escape(entrega.entregaId) + '"] [data-ultima]');
            if (tarjeta) {
                tarjeta.textContent = textoUltimaPosicion(entrega.ultimaPosicion);
            }
            if (!tieneCoordenadas(entrega.ultimaPosicion)) {
                return;
            }

            var punto = [entrega.ultimaPosicion.latitud, entrega.ultimaPosicion.longitud];
            vigentes[entrega.entregaId] = true;
            if (marcadores[entrega.entregaId]) {
                marcadores[entrega.entregaId].setLatLng(punto).setPopupContent(contenidoPopup(entrega));
            } else {
                marcadores[entrega.entregaId] = L.marker(punto).addTo(mapa).bindPopup(contenidoPopup(entrega));
            }
        });

        Object.keys(marcadores).forEach(function (id) {
            if (!vigentes[id]) {
                mapa.removeLayer(marcadores[id]);
                delete marcadores[id];
            }
        });

        // Solo se encuadra la primera vez, para no mover el mapa mientras el usuario lo mira
        var lista = Object.keys(marcadores).map(function (id) { return marcadores[id]; });
        if (primeraCarga && lista.length === 1) {
            mapa.setView(lista[0].getLatLng(), 14);
        } else if (primeraCarga && lista.length > 1) {
            mapa.fitBounds(L.featureGroup(lista).getBounds(), { padding: [40, 40] });
        }
        primeraCarga = false;
    }

    function actualizar() {
        fetch(urlDatos, { headers: { "Accept": "application/json" }, credentials: "same-origin" })
            .then(function (respuesta) {
                if (!respuesta.ok) {
                    throw new Error("HTTP " + respuesta.status);
                }
                return respuesta.json();
            })
            .then(function (entregas) {
                dibujar(entregas);
                estadoActualizacion.textContent = "Actualizado a las " + new Date().toLocaleTimeString("es-AR");
            })
            .catch(function () {
                estadoActualizacion.textContent = "No se pudo actualizar la posición. Reintentando…";
            });
    }

    // Click en una tarjeta: centra el mapa en su camion
    Array.prototype.forEach.call(tarjetas, function (tarjeta) {
        tarjeta.addEventListener("click", function (evento) {
            if (evento.target.closest("a")) {
                return;
            }
            var marcador = marcadores[tarjeta.dataset.entrega];
            if (marcador) {
                mapa.setView(marcador.getLatLng(), 15);
                marcador.openPopup();
            }
        });
    });

    actualizar();
    setInterval(actualizar, INTERVALO_MS);
})();
