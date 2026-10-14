//Inicializa el mapa en Buenos aires
var mapa = L.map("mapa").setView([-34.6037, -58.3816], 11);

//Agrego capas de OpenStreetMap
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 19
}).addTo(mapa);