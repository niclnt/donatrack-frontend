/* DonaTrack — interacciones de la maqueta (progressive enhancement).
   Sin dependencias. Todo funciona con el HTML aunque falle el JS. */
(function () {
  "use strict";

  /* --- Nav responsive (hamburguesa) --- */
  document.querySelectorAll("[data-nav-toggle]").forEach(function (btn) {
    var barra = btn.closest(".topbar");
    var menu = null;
    if (barra){
      menu = barra.querySelector(".topbar__nav");
    }
    if (!menu) return;
    btn.setAttribute("aria-controls", menu.id);
    btn.addEventListener("click", function () {
      var open = btn.getAttribute("aria-expanded") === "true";
      btn.setAttribute("aria-expanded", String(!open));
      menu.classList.toggle("is-open", !open);
    });
  });

  /* --- Filtros tipo pill / tabs (aria-pressed) --- */
  document.querySelectorAll("[data-filter-group]").forEach(function (group) {
    group.addEventListener("click", function (e) {
      var btn = e.target.closest(".filter-pill");
      if (!btn) return;
      group.querySelectorAll(".filter-pill").forEach(function (b) {
        b.setAttribute("aria-pressed", String(b === btn));
      });
    });
  });

  /* --- Toasts (notificaciones no intrusivas) --- */
  function toast(msg, kind) {
    var region = document.getElementById("toast-region");
    if (!region) {
      region = document.createElement("div");
      region.id = "toast-region";
      region.className = "toast-region";
      region.setAttribute("role", "status");
      region.setAttribute("aria-live", "polite");
      document.body.appendChild(region);
    }
    var el = document.createElement("div");
    el.className = "toast" + (kind ? " toast--" + kind : "");
    el.textContent = msg;
    region.appendChild(el);
    setTimeout(function () { el.remove(); }, 3500);
  }
  window.donaTrackToast = toast;
  document.querySelectorAll("[data-toast]").forEach(function (btn) {
    btn.addEventListener("click", function () { toast(btn.getAttribute("data-toast"), "success"); });
  });

  /* --- Modales --- */
  document.querySelectorAll("[data-modal-open]").forEach(function (btn) {
    var modal = document.getElementById(btn.getAttribute("data-modal-open"));
    if (!modal) return;
    btn.addEventListener("click", function () { modal.hidden = false; });
    modal.addEventListener("click", function (e) {
      if (e.target === modal || e.target.hasAttribute("data-modal-close")) modal.hidden = true;
    });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") modal.hidden = true;
    });
  });

  /* --- Demo de carga asincrónica (skeleton -> contenido) --- */
  document.querySelectorAll("[data-skeleton]").forEach(function (region) {
    var real = region.querySelector("[data-content]");
    var skel = region.querySelector("[data-loader]");
    if (real && skel) {
      setTimeout(function () { skel.hidden = true; real.hidden = false; }, 1400);
    }
  });
})();

/* ---- Agregados para el front SSR (Entrega 5) ---- */
(function () {
  "use strict";

  /* Mensajes flash del servidor (ej. "Camión registrado") -> toast */
  document.querySelectorAll("[data-flash]").forEach(function (el) {
    if (window.donaTrackToast) window.donaTrackToast(el.textContent.trim(), el.getAttribute("data-tipo") || "success");
  });

  /* Loader en el boton al enviar formularios: evita doble envio y muestra que se esta procesando */
  document.querySelectorAll("form[data-loading]").forEach(function (form) {
    form.addEventListener("submit", function () {
      var btn = form.querySelector("button[type=submit]");
      if (!btn) return;
      btn.setAttribute("aria-busy", "true");
      btn.disabled = true;
      var spin = document.createElement("span");
      spin.className = "spinner spinner--sm";
      spin.setAttribute("aria-hidden", "true");
      btn.prepend(spin);
    });
  });

  /* Mostrar/ocultar contraseña: agrega un boton con ojo a cada campo de contraseña. */
  var ICONO_OJO = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>';
  var ICONO_OJO_TACHADO = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-11-7-11-7a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

  document.querySelectorAll('input[type="password"]').forEach(function (input) {
    var contenedor = document.createElement("div");
    contenedor.className = "password-field";
    input.parentNode.insertBefore(contenedor, input);
    contenedor.appendChild(input);

    var btn = document.createElement("button");
    btn.type = "button";
    btn.className = "password-toggle";
    btn.setAttribute("aria-controls", input.id);
    contenedor.appendChild(btn);

    function actualizar(visible) {
      input.type = visible ? "text" : "password";
      btn.innerHTML = visible ? ICONO_OJO_TACHADO : ICONO_OJO;
      btn.setAttribute("aria-pressed", String(visible));
      btn.setAttribute("aria-label", visible ? "Ocultar contraseña" : "Mostrar contraseña");
      btn.title = btn.getAttribute("aria-label");
    }
    actualizar(false);

    btn.addEventListener("click", function () {
      actualizar(input.type === "password");
      input.focus();
    });

    if (input.form) {
      input.form.addEventListener("submit", function () { actualizar(false); });
    }
  });
})();

/* ---- MÓDULO REGISTRAR DONACIÓN & DONANTES ---- */
(function () {
  "use strict";

  /* 1. Búsqueda y Selección Dinámica de Donante (Vista Registrar Donación) */
  var btnBuscar = document.getElementById("btnBuscarDonante");
  var inputBuscar = document.getElementById("buscar-donante");
  var selectTipo = document.getElementById("tipo-persona");
  var donanteIdInput = document.getElementById("donanteIdInput");
  var donanteNombre = document.getElementById("donanteNombre");
  var donanteDetalles = document.getElementById("donanteDetalles");
  var donanteVacio = document.getElementById("donanteVacio");
  var cardDonanteSeleccionado = document.getElementById("cardDonanteSeleccionado");

  function seleccionarDonante(id, nombre, documento, email) {
    if (!donanteIdInput || !cardDonanteSeleccionado) return;
    donanteIdInput.value = id;
    if (donanteNombre) donanteNombre.textContent = nombre;
    if (donanteDetalles) donanteDetalles.textContent = (documento ? "Doc: " + documento + " · " : "") + email;

    if (donanteVacio) donanteVacio.style.display = "none";
    cardDonanteSeleccionado.style.display = "flex";
  }

  window.deseleccionarDonante = function () {
    if (!donanteIdInput || !cardDonanteSeleccionado) return;
    donanteIdInput.value = "";
    cardDonanteSeleccionado.style.display = "none";
    if (donanteVacio) donanteVacio.style.display = "block";
  };

  if (btnBuscar && inputBuscar) {
    btnBuscar.addEventListener("click", function () {
      var query = inputBuscar.value.trim().toLowerCase();
      var tipo = selectTipo ? selectTipo.value : "HUMANA";

      if (!query) {
        alert("Ingresá un nombre, DNI, CUIT o email para buscar.");
        return;
      }

      var endpoint = "/admin/donantes/buscar?tipo=" + encodeURIComponent(tipo);

      fetch(endpoint)
          .then(function (res) {
            if (!res.ok) throw new Error("Error al consultar el servicio de donantes");
            return res.json();
          })
          .then(function (donantes) {
            if (!donantes || donantes.length === 0) {
              alert("No hay ningún donante registrado de tipo " + tipo + " en la base de datos.");
              return;
            }

            var encontrado = donantes.find(function (d) {
              var doc = (d.numeroDocumento || d.numDocumento || d.cuit || d.dni || d.documento || "").toString().toLowerCase();
              var nombreCompleto = ((d.nombre || "") + " " + (d.apellido || "") + " " + (d.razonSocial || "")).toLowerCase();
              var mail = (d.email || d.usuarioEmail || d.correo || d.mail || "").toLowerCase();

              return doc.includes(query) || nombreCompleto.includes(query) || mail.includes(query);
            });

            if (encontrado) {
              var nombreDisplay = encontrado.razonSocial || ((encontrado.nombre || "") + " " + (encontrado.apellido || "")).trim();
              var docDisplay = encontrado.cuit || encontrado.numeroDocumento || encontrado.numDocumento || encontrado.dni || encontrado.documento || "";
              var mailDisplay = encontrado.email || encontrado.usuarioEmail || encontrado.mail || "Sin email";

              seleccionarDonante(encontrado.id, nombreDisplay, docDisplay, mailDisplay);
            } else {
              alert("No se encontró ningún donante registrado con esos datos.");
            }
          })
          .catch(function (err) {
            console.error("Error buscando donante:", err);
            alert("Ocurrió un error al intentar consultar los donantes.");
          });
    });
  }

  /* 2. Carga Dinámica de Múltiples Bienes (Vista Donante) */
  var contenedorBienes = document.getElementById("contenedorBienes");
  var btnAgregarBien = document.getElementById("btnAgregarBien");

  if (contenedorBienes) {
    window.agregarNuevoBien = function () {
      var primerBloque = contenedorBienes.querySelector(".bien-bloque");
      if (!primerBloque) return;

      var nuevoBloque = primerBloque.cloneNode(true);

      nuevoBloque.querySelectorAll("input").forEach(function (i) {
        if (i.type === "number") i.value = 1;
        else i.value = "";
      });

      var catSelect = nuevoBloque.querySelector(".select-categoria");
      var subSelect = nuevoBloque.querySelector(".select-subcategoria");
      if (catSelect) catSelect.value = "";
      if (subSelect) {
        subSelect.innerHTML = '<option value="" disabled selected hidden>Primero seleccioná una categoría</option>';
        subSelect.disabled = true;
      }

      if (!nuevoBloque.querySelector(".btn-quitar-bien")) {
        var btnQuitar = document.createElement("button");
        btnQuitar.type = "button";
        btnQuitar.className = "btn btn--danger btn--sm btn-quitar-bien";
        btnQuitar.style.cssText = "position: absolute; top: 15px; right: 15px;";
        btnQuitar.innerText = "✕ Quitar";
        btnQuitar.onclick = function () { nuevoBloque.remove(); };
        nuevoBloque.appendChild(btnQuitar);
      }

      contenedorBienes.appendChild(nuevoBloque);
    };

    if (btnAgregarBien) {
      btnAgregarBien.addEventListener("click", window.agregarNuevoBien);
    }
  }

  /* 3. Alta de Nuevo Donante (Vista Gestión de Donantes) */
  var selectNuevoTipo = document.getElementById("nuevoTipoPersona");
  var secHumana = document.getElementById("secPersonaHumana");
  var secJuridica = document.getElementById("secPersonaJuridica");

  if (selectNuevoTipo) {
    selectNuevoTipo.addEventListener("change", function () {
      if (this.value === "JURIDICA") {
        if (secHumana) secHumana.style.display = "none";
        if (secJuridica) secJuridica.style.display = "block";
      } else {
        if (secHumana) secHumana.style.display = "block";
        if (secJuridica) secJuridica.style.display = "none";
      }
    });
  }

  function mostrarAlertaVisual(mensaje, esError) {
    var contenedor = document.getElementById("alertaMensaje");
    if (window.donaTrackToast) {
      window.donaTrackToast(mensaje, esError ? "error" : "success");
    }
    if (contenedor) {
      contenedor.className = esError ? "alert alert--error" : "alert alert--success";
      contenedor.textContent = mensaje;
      contenedor.style.display = "block";
      setTimeout(function () { contenedor.style.display = "none"; }, 5000);
    }
  }

  var btnGuardarDonanteSolo = document.getElementById("btnGuardarDonanteSolo");
  if (btnGuardarDonanteSolo) {
    btnGuardarDonanteSolo.addEventListener("click", function () {
      var tipo = selectNuevoTipo ? selectNuevoTipo.value : "HUMANA";
      var email = document.getElementById("nuevoEmail").value.trim();
      var telefono = document.getElementById("nuevoTelefono").value.trim();

      var radios = document.getElementsByName("medioPreferido");
      var medioPreferidoVal = "EMAIL";
      for (var r of radios) {
        if (r.checked) medioPreferidoVal = r.value;
      }

      if (!email) {
        mostrarAlertaVisual("El correo electrónico es obligatorio.", true);
        return;
      }

      var mediosDeContacto = [
        { valor: email, tipoContacto: "EMAIL" }
      ];
      if (telefono) {
        mediosDeContacto.push({ valor: telefono, tipoContacto: "TELEFONO" });
      }

      var medioContactoPreferidoObj = {
        valor: medioPreferidoVal === "EMAIL" ? email : (telefono || email),
        tipoContacto: medioPreferidoVal === "EMAIL" ? "EMAIL" : "TELEFONO"
      };

      var bodyData = {
        tipoPersona: tipo,
        mail: email,
        mediosDeContacto: mediosDeContacto,
        medioDeContactoPreferido: medioContactoPreferidoObj
      };

      if (tipo === "HUMANA") {
        var nombre = document.getElementById("nuevoNombre").value.trim();
        var tipoDoc = document.getElementById("nuevoTipoDoc").value;
        var doc = document.getElementById("nuevoDoc").value.trim();
        var edadVal = document.getElementById("nuevaEdad").value;
        var genero = document.getElementById("nuevoGenero").value;
        var direccion = document.getElementById("nuevaDireccion").value.trim();

        if (!nombre || !doc || !edadVal || !genero) {
          mostrarAlertaVisual("Por favor completá Nombre, Documento, Edad y Género.", true);
          return;
        }

        bodyData.nombre = nombre;
        bodyData.tipoDoc = tipoDoc;
        bodyData.documento = doc;
        bodyData.edad = parseInt(edadVal, 10);
        bodyData.genero = genero;
        bodyData.direccion = direccion;
      } else {
        var razonSocial = document.getElementById("nuevaRazonSocial").value.trim();
        var tipoJuridico = document.getElementById("nuevoTipoJuridico").value;
        var rubro = document.getElementById("nuevoRubro").value.trim();

        if (!razonSocial || !rubro) {
          mostrarAlertaVisual("Por favor completá Razón Social y Rubro.", true);
          return;
        }

        bodyData.razonSocial = razonSocial;
        bodyData.tipoJuridico = tipoJuridico;
        bodyData.rubro = rubro;
      }

      fetch("/admin/donantes/crear", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bodyData)
      })
          .then(function (res) {
            if (!res.ok) {
              return res.json().then(function(errData) {
                throw new Error(errData.error || errData.message || "Error al procesar el alta");
              });
            }
            return res.json();
          })
          .then(function (donanteCreado) {
            mostrarAlertaVisual("¡Donante registrado exitosamente en el sistema!", false);

            ["nuevoNombre", "nuevoDoc", "nuevaEdad", "nuevoEmail", "nuevoTelefono", "nuevaDireccion", "nuevaRazonSocial", "nuevoRubro"].forEach(function(id) {
              var el = document.getElementById(id);
              if (el) el.value = "";
            });
          })
          .catch(function (err) {
            console.error("Error al registrar donante:", err);
            mostrarAlertaVisual("Ocurrió un error al intentar registrar el donante: " + err.message, true);
          });
    });
  }
})();