//Inicializa el mapa en Buenos aires
var mapa = L.map("mapa").setView([-34.6037, -58.3816], 11);

//Agrego capas de OpenStreetMap
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 19
}).addTo(mapa);

//Obtengo donaciones desde Thymeleaf y las marco en el mapa
var donaciones = window.donacionesEntregadas;
var marcadores = [];
donaciones.forEach(function (donacion) {
    // Comprueba que ambas coordenadas sean números finitos
    if (!Number.isFinite(donacion.latitud) ||
        !Number.isFinite(donacion.longitud)) {
        return;
    }
    // Comprueba que las coordenadas estén dentro de los rangos geográficos válidos
    if (Math.abs(donacion.latitud) > 90 ||
        Math.abs(donacion.longitud) > 180) {
        return;
    }
    // Creamos el contenido del popup
    var contenido = document.createElement("div");

    var titulo = document.createElement("strong");
    var nombreEntidad = donacion.entidadBeneficiaria;

    if (typeof nombreEntidad !== "string" || nombreEntidad.trim() === "") {
        nombreEntidad = "Entidad no especificada";
    }

    titulo.textContent = nombreEntidad;

    var fecha = document.createElement("p");
    var fechaEntrega = donacion.fechaEntrega;

    if (typeof fechaEntrega !== "string" || fechaEntrega.trim() === "") {
        fechaEntrega = "No disponible";
    }

    fecha.textContent = "Fecha de entrega: " + fechaEntrega;

    var bienes = document.createElement("p");
    var descripcionBienes = "No especificados";

    if (Array.isArray(donacion.bienes) && donacion.bienes.length > 0) {
        descripcionBienes = donacion.bienes.join(", ");
    }

    bienes.textContent = "Bienes entregados: " + descripcionBienes;

    contenido.appendChild(titulo);
    contenido.appendChild(fecha);
    contenido.appendChild(bienes);

    // Fotos que cargo la entidad al recibir la donacion (publicas, sin login)
    if (Array.isArray(donacion.fotos) && donacion.fotos.length > 0) {
        var galeria = document.createElement("div");
        galeria.style.cssText = "display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px;";
        donacion.fotos.forEach(function (fotoId) {
            var enlace = document.createElement("a");
            enlace.href = "/fotos/" + encodeURIComponent(fotoId);
            enlace.target = "_blank";
            enlace.rel = "noopener";
            enlace.setAttribute("aria-label", "Ver foto de la entrega en tamaño completo");

            var img = document.createElement("img");
            img.src = "/fotos/" + encodeURIComponent(fotoId);
            img.alt = "Foto de la donación entregada a " + nombreEntidad;
            img.loading = "lazy";
            img.style.cssText = "width: 72px; height: 72px; object-fit: cover; border-radius: 6px;";

            enlace.appendChild(img);
            galeria.appendChild(enlace);
        });
        contenido.appendChild(galeria);
    }

    // Creamos el marcador y le asociamos el popup
    var marcador = L.marker([donacion.latitud, donacion.longitud])
        .addTo(mapa)
        .bindPopup(contenido);

    marcadores.push(marcador);
});

if (marcadores.length === 1) {
    mapa.setView(marcadores[0].getLatLng(), 15);
}

if (marcadores.length > 1) {
    var grupoMarcadores = L.featureGroup(marcadores);

    mapa.fitBounds(grupoMarcadores.getBounds(), {
        padding: [40, 40]
    });
}