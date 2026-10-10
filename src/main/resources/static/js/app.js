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
})();
