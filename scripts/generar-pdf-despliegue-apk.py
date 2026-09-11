from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


OUTPUT = "output/pdf/despliegue-ubuntu-y-apk.pdf"


def styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "title",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=22,
            leading=27,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#1d252d"),
            spaceAfter=12,
        ),
        "subtitle": ParagraphStyle(
            "subtitle",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=10.5,
            leading=15,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#415366"),
            spaceAfter=16,
        ),
        "h1": ParagraphStyle(
            "h1",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=14,
            leading=18,
            textColor=colors.HexColor("#23405f"),
            spaceBefore=10,
            spaceAfter=6,
        ),
        "body": ParagraphStyle(
            "body",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9.4,
            leading=13.2,
            alignment=TA_LEFT,
            textColor=colors.HexColor("#1d252d"),
            spaceAfter=5,
        ),
        "code": ParagraphStyle(
            "code",
            parent=base["BodyText"],
            fontName="Courier",
            fontSize=7.2,
            leading=9.4,
            textColor=colors.HexColor("#25313d"),
            backColor=colors.HexColor("#f6f8fa"),
            borderColor=colors.HexColor("#d9e0e7"),
            borderWidth=0.5,
            borderPadding=5,
            spaceAfter=7,
        ),
    }


S = styles()


def p(text, style="body"):
    return Paragraph(text, S[style])


def code(text):
    return Preformatted(text.strip(), S["code"], maxLineLength=96)


def bullets(items):
    return [p("- " + item) for item in items]


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#5e7184"))
    canvas.drawString(1.5 * cm, 1.0 * cm, "Inventario Modular - Despliegue Ubuntu y APK")
    canvas.drawRightString(19.5 * cm, 1.0 * cm, f"Pagina {doc.page}")
    canvas.restoreState()


def build():
    doc = SimpleDocTemplate(
        OUTPUT,
        pagesize=A4,
        leftMargin=1.5 * cm,
        rightMargin=1.5 * cm,
        topMargin=1.45 * cm,
        bottomMargin=1.45 * cm,
        title="Despliegue Ubuntu y publicacion de APK",
        author="Centro Judicial San Pedro - Departamento de Informatica",
    )

    story = [
        p("Despliegue Ubuntu y publicacion de APK", "title"),
        p("Inventario Modular - guia PuTTY, GitLab, permisos y actualizacion Android", "subtitle"),
        p("Criterio general", "h1"),
        *bullets([
            "El servidor web/backend se actualiza en Ubuntu desde GitLab.",
            "La APK se genera primero en Windows, se firma y despues se copia a Ubuntu.",
            "Ubuntu ejecuta el JAR de Spring Boot y publica la APK como descarga autenticada.",
            "Las credenciales reales, tokens, claves LDAP y firma Android no se suben a GitLab.",
        ]),
        p("1. Actualizar Ubuntu via PuTTY", "h1"),
        code("""
hostname
cd /opt/inventario-modular
systemctl status inventario-modular.service --no-pager -l
git remote -v
git branch --show-current
git status --short
git fetch origin
git checkout primeros-pasos
git pull --ff-only origin primeros-pasos
sh ./mvnw --batch-mode test
sh ./mvnw --batch-mode -DskipTests package
sudo systemctl restart inventario-modular.service
systemctl status inventario-modular.service --no-pager -l
sudo journalctl -u inventario-modular.service -n 120 --no-pager
"""),
        p("Si git status muestra cambios locales no esperados, detenerse y revisarlos antes de actualizar. Este procedimiento no toca el inventario viejo ni inventario.service.", "body"),
        p("2. Verificaciones HTTP", "h1"),
        code("""
curl -I http://127.0.0.1:8081/movil/login
curl -s http://127.0.0.1:8081/api/v1/sistema/estado
"""),
        p("Desde PC o celular se prueba con http://IP_DEL_SERVIDOR:8081/movil/login. Si Ubuntu responde y el celular no, revisar firewall, Wi-Fi, VLAN o permisos LAN.", "body"),
        p("3. Generar APK en Windows", "h1"),
        code(r"""
cd C:\Users\Gustavo\Documents\ChatGPT\Inventario-Modular\android
.\gradlew.bat :app:assembleLanRelease
"""),
        p("Salida esperada:", "body"),
        code(r"""
C:\Users\Gustavo\Documents\ChatGPT\Inventario-Modular\android\app\build\outputs\apk\lanRelease\app-lanRelease.apk
"""),
        p("Antes de cada nueva version incrementar versionCode en android/app/build.gradle. versionName es la version visible; lanRelease agrega el sufijo -lan.", "body"),
        p("4. Copiar APK a Ubuntu", "h1"),
        code("""
sudo mkdir -p /opt/inventario-modular/distribucion
# Si se copio con WinSCP al home:
sudo cp /home/TU_USUARIO/tareas-lan.apk /opt/inventario-modular/distribucion/tareas-lan.apk
"""),
        p("La ruta estable recomendada es /opt/inventario-modular/distribucion/tareas-lan.apk. No es necesario guardar la APK operativa en GitLab.", "body"),
        p("5. Permisos de descarga", "h1"),
        code("""
sudo systemctl show inventario-modular.service -p User -p Group
sudo chown root:root /opt/inventario-modular/distribucion/tareas-lan.apk
sudo chmod 644 /opt/inventario-modular/distribucion/tareas-lan.apk
sudo chmod 755 /opt/inventario-modular/distribucion
"""),
        p("El archivo queda legible para el servicio. La descarga sigue protegida por login y permiso de tareas.", "body"),
        p("6. Configurar EnvironmentFile", "h1"),
        code("""
sudo nano /etc/inventario-modular/inventario-modular.env

INVENTARIO_MOVIL_APK_PATH=/opt/inventario-modular/distribucion/tareas-lan.apk

sudo chown root:root /etc/inventario-modular/inventario-modular.env
sudo chmod 600 /etc/inventario-modular/inventario-modular.env
sudo systemctl restart inventario-modular.service
"""),
        p("Verificar endpoint de APK:", "body"),
        code("""
curl -I http://127.0.0.1:8081/api/v1/movil/apk
curl -s http://127.0.0.1:8081/api/v1/movil/apk/info
"""),
        p("7. Checklist celular", "h1"),
        *bullets([
            "Abrir /movil/login o entrar desde la APK.",
            "Ingresar con usuario tecnico autorizado.",
            "Abrir Ajustes y confirmar version instalada y publicada.",
            "Descargar o actualizar APK desde Ajustes cuando corresponda.",
            "Activar avisos, probar sonido y crear una tarea desde otra cuenta.",
            "Probar con pantalla bloqueada, reposo, reconexion Wi-Fi y reinicio del servidor.",
        ]),
        p("8. Regla de continuidad", "h1"),
        code("""
GitLab conserva codigo y documentacion.
Windows genera la APK.
Ubuntu ejecuta el servidor y publica la APK generada.
"""),
    ]

    doc.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build()
