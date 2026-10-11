/* DonaTrack — interacciones de la maqueta (progressive enhancement).
   Sin dependencias. Todo funciona con el HTML aunque falle el JS. */
(function () {
  "use strict";

  /* --- Nav responsive (hamburguesa) --- */
  document.querySelectorAll("[data-nav-toggle]").forEach(function (btn) {
    var menu = document.getElementById(btn.getAttribute("aria-controls"));
    if (!menu) return;
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

  /* Mostrar/ocultar contraseña: agrega un boton con ojo a cada campo de contraseña.
     Sin JS el campo sigue funcionando como un password comun. */
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

    // Al enviar se vuelve a ocultar, para que el navegador no la guarde como texto en el historial de formularios
    if (input.form) {
      input.form.addEventListener("submit", function () { actualizar(false); });
    }
  });
})();
