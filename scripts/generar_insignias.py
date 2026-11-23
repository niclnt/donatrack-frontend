"""Dibuja las 9 insignias de DonaTrack (PNG 512x512, fondo transparente).

Uso, desde la raiz del repo del front (necesita Pillow: pip install pillow):
    python scripts/generar_insignias.py

Salida: src/main/resources/static/img/insignias/<nombre-de-la-insignia>.png
El color del marco indica la categoria de la mision: bronce (Colaborador), plata (Sostenedor), oro (Transformador).
Incentivos arma la URL de cada imagen a partir del nombre de la insignia (mismo slug que aca).
"""
import math
import unicodedata
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

N = 2048                    # se dibuja grande y se achica (bordes suaves)
C = N // 2
SALIDA = 512
K = 1.4                     # escala del icono: 1 unidad = K px
BLANCO = (248, 250, 252, 255)
NAVY = (15, 23, 42, 255)
ROJO = (239, 68, 68, 255)
VERDE = (34, 197, 94, 255)
CELESTE = (125, 211, 252, 255)
NARANJA = (251, 146, 60, 255)
AMARILLO = (253, 224, 71, 255)
VIOLETA = (167, 139, 250, 255)
MARRON = (180, 120, 70, 255)

MARCOS = {
    "bronce": ((246, 196, 140), (122, 74, 28)),
    "plata": ((244, 246, 248), (110, 122, 136)),
    "oro": ((255, 236, 160), (168, 123, 0)),
}


def p(x, y):
    return (C + x * K, C + y * K)


def caja(x0, y0, x1, y1):
    return [p(x0, y0), p(x1, y1)]


def slug(nombre):
    s = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode().lower()
    return "-".join(s.split())


def gradiente_vertical(arriba, abajo):
    g = Image.new("RGBA", (N, N))
    d = ImageDraw.Draw(g)
    for y in range(N):
        t = y / (N - 1)
        d.line([(0, y), (N, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(arriba, abajo)) + (255,))
    return g


def fondo(marco):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    # Sombra suave
    sombra = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(sombra).ellipse([C - 960, C - 940, C + 960, C + 980], fill=(0, 0, 0, 110))
    img.alpha_composite(sombra.filter(ImageFilter.GaussianBlur(30)))
    # Marco metalico (aro)
    claro, oscuro = MARCOS[marco]
    mascara = Image.new("L", (N, N), 0)
    dm = ImageDraw.Draw(mascara)
    dm.ellipse([C - 960, C - 960, C + 960, C + 960], fill=255)
    img.paste(gradiente_vertical(claro, oscuro), (0, 0), mascara)
    brillo = Image.new("L", (N, N), 0)
    ImageDraw.Draw(brillo).ellipse([C - 900, C - 900, C + 900, C + 900], fill=255)
    img.paste(gradiente_vertical(oscuro, claro), (0, 0), brillo)
    # Disco interior azul oscuro
    interior = Image.new("L", (N, N), 0)
    ImageDraw.Draw(interior).ellipse([C - 840, C - 840, C + 840, C + 840], fill=255)
    img.paste(gradiente_vertical((37, 99, 235), (15, 23, 42)), (0, 0), interior)
    # Reflejo arriba
    reflejo = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ImageDraw.Draw(reflejo).ellipse([C - 640, C - 790, C + 640, C - 120], fill=(255, 255, 255, 28))
    img.alpha_composite(reflejo.filter(ImageFilter.GaussianBlur(40)))
    return img


def corazon(d, cx, cy, t, color):
    r = t * 0.55
    d.ellipse(caja(cx - t, cy - t * 0.75, cx - t + 2 * r, cy - t * 0.75 + 2 * r), fill=color)
    d.ellipse(caja(cx + t - 2 * r, cy - t * 0.75, cx + t, cy - t * 0.75 + 2 * r), fill=color)
    d.polygon([p(cx - t * 0.98, cy - t * 0.25), p(cx + t * 0.98, cy - t * 0.25), p(cx, cy + t)], fill=color)


def icono_manos_a_la_obra(d):
    d.polygon([p(-260, -60), p(260, -60), p(225, -140), p(-225, -140)], fill=(226, 232, 240, 255))
    d.rectangle(caja(-230, -60, 230, 230), fill=BLANCO)
    d.rectangle(caja(-30, -140, 30, 0), fill=(203, 213, 225, 255))
    corazon(d, 0, 100, 95, ROJO)
    for lado in (-1, 1):  # manos sosteniendo la caja
        d.ellipse(caja(lado * 120 - 140, 190, lado * 120 + 140, 320), fill=(253, 186, 116, 255))
        for i in range(3):
            x = lado * (60 + i * 55)
            d.ellipse(caja(x - 30, 160, x + 30, 240), fill=(253, 186, 116, 255))


def icono_surtido_solidario(d):
    d.rectangle(caja(-230, -150, -90, 60), fill=CELESTE)                      # lata
    d.ellipse(caja(-230, -180, -90, -120), fill=(186, 230, 253, 255))
    d.rounded_rectangle(caja(-40, -170, 70, 60), radius=30 * K, fill=BLANCO)  # botella de leche
    d.rectangle(caja(-10, -260, 40, -160), fill=BLANCO)
    d.rectangle(caja(-18, -285, 48, -250), fill=CELESTE)
    d.polygon([p(110, -110), p(170, -150), p(230, -150), p(290, -110), p(270, -60),  # remera
               p(250, -70), p(250, 60), p(150, 60), p(150, -70), p(130, -60)], fill=NARANJA)
    d.polygon([p(-320, 20), p(320, 20), p(250, 300), p(-250, 300)], fill=MARRON)  # canasta
    for y in (90, 160, 230):
        d.line([p(-300 + (y - 20) * 0.25, y), p(300 - (y - 20) * 0.25, y)], fill=(120, 72, 36, 255), width=int(18 * K))
    d.arc(caja(-260, -260, 260, 160), start=200, end=340, fill=MARRON, width=int(26 * K))


def icono_cargamento_generoso(d):
    d.rectangle(caja(-140, -250, 10, -100), fill=NARANJA)
    d.rectangle(caja(-330, -250, -160, -100), fill=AMARILLO)
    d.rectangle(caja(-250, -390, -80, -260), fill=CELESTE)
    d.rectangle(caja(-340, -90, 60, 130), fill=BLANCO)                        # caja del camion
    d.polygon([p(70, -20), p(220, -20), p(330, 70), p(330, 130), p(70, 130)], fill=BLANCO)
    d.polygon([p(100, 0), p(205, 0), p(285, 65), p(100, 65)], fill=CELESTE)  # ventana
    for x in (-220, 210):
        d.ellipse(caja(x - 80, 110, x + 80, 270), fill=NAVY)
        d.ellipse(caja(x - 35, 155, x + 35, 225), fill=(203, 213, 225, 255))


def icono_vision_integral(d):
    puntos_arriba = [p(x, -230 * (1 - (x / 400) ** 2)) for x in range(-400, 401, 10)]
    puntos_abajo = [p(x, 230 * (1 - (x / 400) ** 2)) for x in range(400, -401, -10)]
    d.polygon(puntos_arriba + puntos_abajo, fill=BLANCO)
    colores = [ROJO, NARANJA, AMARILLO, VERDE, CELESTE, VIOLETA]
    for i, color in enumerate(colores):
        d.pieslice(caja(-180, -180, 180, 180), start=i * 60, end=(i + 1) * 60, fill=color)
    d.ellipse(caja(-70, -70, 70, 70), fill=NAVY)
    d.ellipse(caja(-45, -55, -5, -15), fill=BLANCO)


def icono_mision_cumplida(d):
    d.polygon([p(-280, -120), p(140, -120), p(240, -220), p(-180, -220)], fill=(226, 232, 240, 255))
    d.polygon([p(140, -120), p(240, -220), p(240, 140), p(140, 240)], fill=(203, 213, 225, 255))
    d.rectangle(caja(-280, -120, 140, 240), fill=BLANCO)
    d.rectangle(caja(-95, -120, -45, 240), fill=(203, 213, 225, 255))
    d.ellipse(caja(40, 60, 340, 360), fill=VERDE)
    d.line([p(100, 210), p(165, 280), p(285, 135)], fill=BLANCO, width=int(48 * K), joint="curve")


def icono_columna_vertebral(d):
    d.rectangle(caja(-130, -420, 130, -280), fill=NARANJA)                    # caja que sostiene
    d.rectangle(caja(-20, -420, 20, -280), fill=(194, 65, 12, 255))
    d.rectangle(caja(-220, -270, 220, -200), fill=BLANCO)                     # capitel
    d.rectangle(caja(-150, -200, 150, 250), fill=BLANCO)                      # fuste
    for x in (-80, 0, 80):
        d.line([p(x, -180), p(x, 230)], fill=(148, 163, 184, 255), width=int(22 * K))
    d.rectangle(caja(-240, 250, 240, 320), fill=BLANCO)                       # base


def icono_constancia_de_hierro(d):
    for i in range(12):  # cadena de eslabones alrededor
        a = 2 * math.pi * i / 12
        x, y = 320 * math.cos(a), 320 * math.sin(a)
        r = 70 if i % 2 == 0 else 55
        d.ellipse(caja(x - r, y - r, x + r, y + r), outline=(203, 213, 225, 255), width=int(26 * K))
    d.polygon([p(0, -230), p(120, -40), p(140, 70), p(90, 170), p(0, 210), p(-90, 170),
               p(-140, 70), p(-120, -40)], fill=NARANJA)                      # llama
    d.polygon([p(0, -60), p(65, 50), p(60, 130), p(0, 170), p(-60, 130), p(-65, 50)], fill=AMARILLO)


def icono_pilar_comunitario(d):
    d.rectangle(caja(-120, -330, 120, -280), fill=BLANCO)
    d.rectangle(caja(-80, -280, 80, 200), fill=BLANCO)
    d.rectangle(caja(-130, 200, 130, 250), fill=BLANCO)
    for x, y, color in ((-290, -40, CELESTE), (290, -40, VERDE), (-200, 170, NARANJA), (200, 170, VIOLETA)):
        d.ellipse(caja(x - 60, y - 60, x + 60, y + 60), fill=color)           # cabeza
        d.pieslice(caja(x - 110, y + 70, x + 110, y + 290), start=180, end=360, fill=color)  # cuerpo


def icono_motor_de_cambio(d):
    puntos = []
    dientes = 10
    for i in range(dientes * 4):
        a = 2 * math.pi * i / (dientes * 4)
        r = 380 if i % 4 in (1, 2) else 300
        puntos.append(p(r * math.cos(a), r * math.sin(a)))
    d.polygon(puntos, fill=(203, 213, 225, 255))
    d.ellipse(caja(-200, -200, 200, 200), fill=NAVY)
    corazon(d, 0, 10, 130, ROJO)


INSIGNIAS = [
    ("Manos a la Obra", "bronce", icono_manos_a_la_obra),
    ("Surtido Solidario", "bronce", icono_surtido_solidario),
    ("Cargamento Generoso", "bronce", icono_cargamento_generoso),
    ("Visión Integral", "plata", icono_vision_integral),
    ("Misión Cumplida", "plata", icono_mision_cumplida),
    ("Columna Vertebral", "plata", icono_columna_vertebral),
    ("Constancia de Hierro", "oro", icono_constancia_de_hierro),
    ("Pilar Comunitario", "oro", icono_pilar_comunitario),
    ("Motor de Cambio", "oro", icono_motor_de_cambio),
]


def main():
    destino = Path(__file__).resolve().parent.parent / "src/main/resources/static/img/insignias"
    destino.mkdir(parents=True, exist_ok=True)
    for nombre, marco, dibujar in INSIGNIAS:
        img = fondo(marco)
        dibujar(ImageDraw.Draw(img))
        archivo = destino / f"{slug(nombre)}.png"
        img.resize((SALIDA, SALIDA), Image.LANCZOS).save(archivo, optimize=True)
        print(archivo.name)


if __name__ == "__main__":
    main()
