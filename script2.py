import re

file_path = r'c:\Users\gmurad\Desktop\inventario-modular\inventario-modular\src\main\resources\templates\admin\equipo-detalle.html'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace <div style="display: flex; gap: 4px; flex-wrap: wrap;"> with th:if attached
content = content.replace(
    '<div style="display: flex; gap: 4px; flex-wrap: wrap;">',
    '<div style="display: flex; gap: 4px; flex-wrap: wrap;" th:if="">'
)

# The 'eliminar' form is the last one in the div. Let's find it.
# We will use regex to find the 'eliminar' form and append our 'borrar' form after it.

eliminar_form_pattern = re.compile(
    r'(<form th:action="@\{/admin/equipos/\{equipoId\}/componentes/\{componenteId\}/eliminar.*?</form>)',
    re.DOTALL
)

def add_borrar(match):
    form = match.group(1)
    borrar_form = '''
												<form th:action="@{/admin/equipos/{equipoId}/componentes/{componenteId}/borrar-definitivo(equipoId=, componenteId=)}" method="post" onsubmit="return confirm('¿Borrar definitivamente este componente del sistema? (Usar solo si se cargó por error)');">
													<button type="submit" class="secondary-action" title="Borrar del sistema definitivamente" style="font-size: 0.76rem; padding: 3px 6px; background: rgba(0, 0, 0, 0.05); color: #475569; border: 1px solid var(--border-color); cursor: pointer;">
														🗑️ Borrar
													</button>
												</form>'''
    return form + borrar_form

new_content = eliminar_form_pattern.sub(add_borrar, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)
print("Done")
