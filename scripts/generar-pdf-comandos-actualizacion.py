import os
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer, Table, TableStyle

OUTPUT = "output/pdf/guia-actualizacion-ubuntu-mysql.pdf"


def get_styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "title",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=18,
            leading=22,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#0f2744"),
            spaceAfter=4,
        ),
        "subtitle": ParagraphStyle(
            "subtitle",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9.5,
            leading=13,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#475569"),
            spaceAfter=12,
        ),
        "h1": ParagraphStyle(
            "h1",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=12,
            leading=16,
            textColor=colors.HexColor("#1e3a8a"),
            spaceBefore=10,
            spaceAfter=4,
        ),
        "h2": ParagraphStyle(
            "h2",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=10,
            leading=13,
            textColor=colors.HexColor("#0369a1"),
            spaceBefore=6,
            spaceAfter=3,
        ),
        "body": ParagraphStyle(
            "body",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.5,
            leading=12,
            alignment=TA_LEFT,
            textColor=colors.HexColor("#1e293b"),
            spaceAfter=4,
        ),
        "body_bold": ParagraphStyle(
            "body_bold",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=8.5,
            leading=12,
            textColor=colors.HexColor("#0f172a"),
            spaceAfter=2,
        ),
        "code": ParagraphStyle(
            "code",
            parent=base["BodyText"],
            fontName="Courier",
            fontSize=8,
            leading=10.5,
            textColor=colors.HexColor("#0f172a"),
        ),
        "footer": ParagraphStyle(
            "footer",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=7.5,
            leading=10,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#94a3b8"),
        ),
    }


def make_code_box(code_text, s):
    p = Preformatted(code_text.strip(), s["code"])
    t = Table([[p]], colWidths=[17.5 * cm])
    t.setStyle(
        TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#f8fafc")),
            ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#cbd5e1")),
            ("PADDING", (0, 0), (-1, -1), 6),
            ("LEFTPADDING", (0, 0), (-1, -1), 8),
            ("RIGHTPADDING", (0, 0), (-1, -1), 8),
            ("TOPPADDING", (0, 0), (-1, -1), 6),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ])
    )
    return t


def build_pdf():
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    doc = SimpleDocTemplate(
        OUTPUT,
        pagesize=A4,
        leftMargin=1.6 * cm,
        rightMargin=1.6 * cm,
        topMargin=1.4 * cm,
        bottomMargin=1.4 * cm,
    )
    s = get_styles()
    story = []

    # Encabezado
    story.append(Paragraph("INVENTARIO MODULAR", s["title"]))
    story.append(
        Paragraph(
            "Guía Operativa de Actualización: Servidor Ubuntu (PuTTY) y Base de Datos MySQL<br/>"
            "<b>Poder Judicial de Jujuy &bull; Centro Judicial San Pedro &bull; Rama: Develop</b>",
            s["subtitle"],
        )
    )

    # Paso 1: MySQL
    story.append(Paragraph("Paso 1: Actualización de Base de Datos MySQL (Panel / phpMyAdmin)", s["h1"]))
    story.append(
        Paragraph(
            "Desde el panel web de MySQL (phpMyAdmin, cPanel o MySQL Workbench) seleccionar la base "
            "<b>inventario_modular</b> y ejecutar el siguiente script para incorporar el campo de IP de impresora detectada (V34):",
            s["body"],
        )
    )
    sql_code = (
        "-- V34: Columna para registrar la IP de impresoras detectadas por script\n"
        "ALTER TABLE equipos ADD COLUMN impresora_ip VARCHAR(45) NULL AFTER impresora;"
    )
    story.append(make_code_box(sql_code, s))
    story.append(Spacer(1, 4))
    story.append(
        Paragraph(
            "<i>Nota preventiva:</i> Si el servidor aún no tenía ejecutada la migración V33 (Taller y Ciclo de Vida ITAM), "
            "ejecutar también:<br/>"
            "<code>ALTER TABLE tareas_tecnicas ADD COLUMN tipo VARCHAR(40) NOT NULL DEFAULT 'INCIDENTE' AFTER equipo_id;<br/>"
            "ALTER TABLE equipos ADD COLUMN estado_ciclo VARCHAR(40) NOT NULL DEFAULT 'EN_SERVICIO' AFTER activo;</code>",
            s["body"],
        )
    )

    story.append(Spacer(1, 6))

    # Paso 2: Ubuntu PuTTY
    story.append(Paragraph("Paso 2: Comandos en Ubuntu Server vía PuTTY", s["h1"]))
    story.append(
        Paragraph(
            "Conectarse por SSH en PuTTY al servidor Ubuntu y ejecutar los comandos paso a paso:",
            s["body"],
        )
    )

    # 2.1 Directorio
    story.append(Paragraph("1. Ubicarse en el directorio del proyecto:", s["body_bold"]))
    story.append(make_code_box("cd /opt/inventario-modular", s))
    story.append(Spacer(1, 4))

    # 2.2 Git pull
    story.append(Paragraph("2. Sincronizar y descargar los últimos cambios de GitLab:", s["body_bold"]))
    git_code = (
        "# 1. Verificar estado actual del repositorio\n"
        "git status --short\n\n"
        "# 2. Traer ramas y cambios remotos desde GitLab\n"
        "git fetch origin\n\n"
        "# 3. Posicionarse en la rama Develop y descargar commits\n"
        "git checkout Develop\n"
        "git pull origin Develop"
    )
    story.append(make_code_box(git_code, s))
    story.append(Spacer(1, 4))

    # 2.3 Compilar
    story.append(Paragraph("3. Compilar el código y generar el paquete JAR de producción:", s["body_bold"]))
    mvn_code = "sh ./mvnw --batch-mode -DskipTests package"
    story.append(make_code_box(mvn_code, s))
    story.append(
        Paragraph(
            "<i>Verificar que la salida final de Maven indique: <b>[INFO] BUILD SUCCESS</b></i>",
            s["body"],
        )
    )
    story.append(Spacer(1, 4))

    # 2.4 Reinicio
    story.append(Paragraph("4. Reiniciar el servicio en segundo plano (systemd):", s["body_bold"]))
    service_code = "sudo systemctl restart inventario-modular.service"
    story.append(make_code_box(service_code, s))

    story.append(Spacer(1, 6))

    # Paso 3: Verificación
    story.append(Paragraph("Paso 3: Comprobación del Servicio y Monitoreo de Logs", s["h1"]))
    story.append(
        Paragraph(
            "Para confirmar que la aplicación inició correctamente y atiende en el puerto 8081:",
            s["body"],
        )
    )

    check_code = (
        "# Comprobar estado del servicio (debe indicar: Active: active (running))\n"
        "systemctl status inventario-modular.service --no-pager -l\n\n"
        "# Monitorear los logs en tiempo real (para salir presionar Ctrl + C)\n"
        "sudo journalctl -u inventario-modular.service -n 60 -f\n\n"
        "# Comprobacion HTTP local\n"
        "curl -I http://127.0.0.1:8081/admin/equipos"
    )
    story.append(make_code_box(check_code, s))

    story.append(Spacer(1, 10))
    story.append(
        Paragraph(
            "Documento generado automáticamente &bull; Inventario Modular San Pedro &bull; Soporte Informático Judicial",
            s["footer"],
        )
    )

    doc.build(story)
    print("PDF generado exitosamente en:", OUTPUT)


if __name__ == "__main__":
    build_pdf()
