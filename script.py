import re

file_path = r'c:\Users\gmurad\Desktop\inventario-modular\inventario-modular\src\main\resources\templates\admin\equipo-detalle.html'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '<div style="display: flex; gap: 4px; flex-wrap: wrap;">',
    '<div style="display: flex; gap: 4px; flex-wrap: wrap;" th:if="">'
)

borrar_btn = '''												<form th:action="@{/admin/equipos/{equipoId}/componentes/{componenteId}/borrar-definitivo(equipoId=, componenteId=)}" method="post" onsubmit="return confirm('¿Borrar definitivamente este componente del sistema? (Usar solo si se cargó por error)');">
													<button type="submit" class="secondary-action" title="Borrar del sistema definitivamente" style="font-size: 0.76rem; padding: 3px 6px; background: rgba(0, 0, 0, 0.05); color: #475569; border: 1px solid var(--border-color); cursor: pointer;">
														🗑️ Borrar
													</button>
												</form>
											</div>'''

content = content.replace('											</div>', borrar_btn)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done")
