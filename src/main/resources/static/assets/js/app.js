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
  var resultadosDonantes = document.getElementById("resultadosDonantes");
  var resultadosMsg = document.getElementById("resultadosDonantesMsg");
  var formRegistrar = document.getElementById("formRegistrarDonacion");
  var bloqueRepresentante = document.getElementById("bloqueRepresentante");
  var selectRepresentante = document.getElementById("representanteId");

  // Cantidad maxima de resultados que se listan (el resto se pide refinar la busqueda)
  var MAX_RESULTADOS = 20;

  function nombreDe(d) {
    return d.razonSocial || ((d.nombre || "") + " " + (d.apellido || "")).trim() || "Sin nombre";
  }

  function documentoDe(d) {
    return (d.cuit || d.numeroDocumento || d.numDocumento || d.dni || d.documento || "").toString();
  }

  function mailDe(d) {
    return d.email || d.usuarioEmail || d.correo || d.mail || "";
  }

  function detalleDe(d) {
    var doc = documentoDe(d);
    return (doc ? "Doc: " + doc + " · " : "") + (mailDe(d) || "Sin email");
  }

  function mostrarMensajeResultados(texto) {
    if (!resultadosMsg) return;
    resultadosMsg.textContent = texto || "";
    resultadosMsg.hidden = !texto;
  }

  function seleccionarDonante(d, opcion) {
    if (!donanteIdInput || !cardDonanteSeleccionado) return;
    donanteIdInput.value = d.id;
    if (donanteNombre) donanteNombre.textContent = nombreDe(d);
    if (donanteDetalles) donanteDetalles.textContent = detalleDe(d);

    // Resalta la opcion elegida y apaga las demas
    if (resultadosDonantes) {
      resultadosDonantes.querySelectorAll(".donante-opcion").forEach(function (b) {
        b.setAttribute("aria-pressed", b === opcion ? "true" : "false");
      });
    }

    if (donanteVacio) donanteVacio.style.display = "none";
    cardDonanteSeleccionado.style.display = "flex";
    cargarRepresentantes(d);
  }

  // Las personas juridicas donan a traves de un representante: se elige entre los que tiene cargados
  function cargarRepresentantes(d) {
    if (!bloqueRepresentante || !selectRepresentante) return;
    var esJuridica = Array.isArray(d && d.representantes);
    var representantes = esJuridica ? d.representantes : [];

    selectRepresentante.innerHTML = "";
    var inicial = document.createElement("option");
    inicial.value = "";
    inicial.textContent = representantes.length ? "Seleccionar representante" : "La organización no tiene representantes cargados";
    selectRepresentante.appendChild(inicial);
    representantes.forEach(function (r) {
      var opt = document.createElement("option");
      opt.value = r.id;
      opt.textContent = r.nombre + (r.documento ? " (" + (r.tipoDoc || "Doc") + " " + r.documento + ")" : "");
      selectRepresentante.appendChild(opt);
    });

    // Si tiene representantes es obligatorio elegir uno; si no tiene, se registra sin representante
    selectRepresentante.disabled = representantes.length === 0;
    selectRepresentante.required = representantes.length > 0;
    bloqueRepresentante.hidden = !esJuridica;
  }

  window.deseleccionarDonante = function () {
    if (!donanteIdInput || !cardDonanteSeleccionado) return;
    donanteIdInput.value = "";
    cardDonanteSeleccionado.style.display = "none";
    if (donanteVacio) donanteVacio.style.display = "block";
    cargarRepresentantes(null);
    if (resultadosDonantes) {
      resultadosDonantes.querySelectorAll(".donante-opcion").forEach(function (b) {
        b.setAttribute("aria-pressed", "false");
      });
    }
    if (inputBuscar) inputBuscar.focus();
  };

  function renderizarResultados(encontrados) {
    resultadosDonantes.innerHTML = "";
    encontrados.slice(0, MAX_RESULTADOS).forEach(function (d) {
      var opcion = document.createElement("button");
      opcion.type = "button";
      opcion.className = "donante-opcion";
      opcion.setAttribute("role", "listitem");
      opcion.setAttribute("aria-pressed", String(donanteIdInput.value) === String(d.id) ? "true" : "false");

      var nombre = document.createElement("span");
      nombre.className = "donante-opcion__nombre";
      nombre.textContent = nombreDe(d);
      var detalle = document.createElement("span");
      detalle.className = "donante-opcion__detalle";
      detalle.textContent = detalleDe(d);

      opcion.appendChild(nombre);
      opcion.appendChild(detalle);
      opcion.addEventListener("click", function () { seleccionarDonante(d, opcion); });
      resultadosDonantes.appendChild(opcion);
    });
    resultadosDonantes.hidden = encontrados.length === 0;
  }

  function buscarDonantes() {
    var query = inputBuscar.value.trim().toLowerCase();
    var tipo = selectTipo ? selectTipo.value : "HUMANA";

    if (!query) {
      mostrarMensajeResultados("Ingresá un nombre, DNI, CUIT o email para buscar.");
      return;
    }

    mostrarMensajeResultados("Buscando...");
    fetch("/admin/donantes/buscar?tipo=" + encodeURIComponent(tipo))
        .then(function (res) {
          if (!res.ok) throw new Error("Error al consultar el servicio de donantes");
          return res.json();
        })
        .then(function (donantes) {
          var encontrados = (donantes || []).filter(function (d) {
            var texto = [nombreDe(d), documentoDe(d), mailDe(d)].join(" ").toLowerCase();
            return texto.includes(query);
          });

          renderizarResultados(encontrados);

          if (encontrados.length === 0) {
            mostrarMensajeResultados("No se encontró ningún donante de tipo " + tipo.toLowerCase() + " con esos datos.");
          } else if (encontrados.length > MAX_RESULTADOS) {
            mostrarMensajeResultados("Se muestran " + MAX_RESULTADOS + " de " + encontrados.length + " resultados. Refiná la búsqueda si no está el que buscás.");
          } else {
            mostrarMensajeResultados(encontrados.length + " resultado(s). Hacé click en uno para seleccionarlo.");
          }
        })
        .catch(function (err) {
          console.error("Error buscando donante:", err);
          renderizarResultados([]);
          mostrarMensajeResultados("Ocurrió un error al intentar consultar los donantes.");
        });
  }

  if (btnBuscar && inputBuscar && resultadosDonantes) {
    btnBuscar.addEventListener("click", buscarDonantes);
    // Enter en el buscador busca en vez de enviar el formulario
    inputBuscar.addEventListener("keydown", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        buscarDonantes();
      }
    });
  }

  // El id del donante es un hidden: el navegador no lo valida solo
  if (formRegistrar && donanteIdInput) {
    formRegistrar.addEventListener("submit", function (e) {
      if (!donanteIdInput.value) {
        e.preventDefault();
        mostrarMensajeResultados("Seleccioná un donante antes de registrar la donación.");
        if (inputBuscar) inputBuscar.focus();
      }
    });
  }

  /* 2. Alta de Nuevo Donante (Vista Gestión de Donantes) */
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