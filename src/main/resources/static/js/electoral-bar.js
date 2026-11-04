(() => {
	'use strict';

	document.addEventListener('DOMContentLoaded', () => {
		const widget = document.getElementById('electoral-tracker-card');
		if (!widget) return;

		const rangeButtons = widget.querySelectorAll('.electoral-range-btn');
		const bar = widget.querySelector('.electoral-bar');
		const tooltip = widget.querySelector('.electoral-tooltip');
		const tooltipTitle = widget.querySelector('.electoral-tooltip-title');
		const tooltipSub = widget.querySelector('.electoral-tooltip-sub');

		const candidateLeft = widget.querySelector('.electoral-candidate-left');
		const candidateRight = widget.querySelector('.electoral-candidate-right');
		const targetNum = widget.querySelector('.electoral-target-num');
		const subCenter = widget.querySelector('.electoral-sub-center');
		const legend = widget.querySelector('.electoral-legend');

		const activeRangeBtn = widget.querySelector('.electoral-range-btn.is-active');
		let currentRange = activeRangeBtn ? (activeRangeBtn.getAttribute('data-range') || 'HOY') : 'HOY';

		function obtenerFechaFiltro() {
			const fechaInput = document.getElementById('kfilter-fecha');
			if (fechaInput && fechaInput.value) {
				return fechaInput.value;
			}
			const urlParams = new URLSearchParams(window.location.search);
			return urlParams.get('fecha') || '';
		}

		// Interacción con los botones de rango (con soporte ARIA)
		rangeButtons.forEach(btn => {
			btn.addEventListener('click', () => {
				const range = btn.getAttribute('data-range');
				if (!range || range === currentRange) return;

				rangeButtons.forEach(b => {
					b.classList.remove('is-active');
					b.setAttribute('aria-pressed', 'false');
				});
				btn.classList.add('is-active');
				btn.setAttribute('aria-pressed', 'true');
				currentRange = range;
				cargarRendimiento(range);
			});
		});

		async function cargarRendimiento(rango) {
			try {
				widget.style.opacity = '0.75';
				const fecha = obtenerFechaFiltro();
				let url = `/api/v1/tareas/estadisticas-rendimiento?rango=${encodeURIComponent(rango)}`;
				if (fecha) {
					url += `&fecha=${encodeURIComponent(fecha)}`;
				}
				const resp = await fetch(url, {
					credentials: 'same-origin',
					cache: 'no-store'
				});
				if (!resp.ok) throw new Error('HTTP ' + resp.status);
				const data = await resp.json();
				renderizarWidget(data);
			} catch (err) {
				console.error('Error cargando estadísticas de rendimiento:', err);
			} finally {
				widget.style.opacity = '1';
			}
		}

		// Exponer función para actualización en vivo por polling
		window.actualizarBarraElectoral = () => cargarRendimiento(currentRange);

		function renderizarWidget(data) {
			if (!data) return;

			// 1. Líder Izquierda
			if (data.lider) {
				candidateLeft.innerHTML = `
					<span class="electoral-candidate-name">
						<span class="electoral-dot" style="background-color: ${data.lider.color};"></span>
						${escapeHtml(data.lider.nombreVisible)}
					</span>
					<strong class="electoral-candidate-num">${data.lider.realizadas}</strong>
				`;
			} else {
				candidateLeft.innerHTML = `
					<span class="electoral-candidate-name" style="opacity: 0.6;">Sin tareas cerradas</span>
					<strong class="electoral-candidate-num">0</strong>
				`;
			}

			// 2. Marcador Central
			targetNum.textContent = data.totalRealizadas;
			subCenter.textContent = `${data.totalPendientes} por asignar / pendientes`;

			// 3. Segundo Derecha
			if (data.segundo) {
				candidateRight.innerHTML = `
					<strong class="electoral-candidate-num">${data.segundo.realizadas}</strong>
					<span class="electoral-candidate-name">
						${escapeHtml(data.segundo.nombreVisible)}
						<span class="electoral-dot" style="background-color: ${data.segundo.color};"></span>
					</span>
				`;
			} else if (data.tecnicos && data.tecnicos.length === 1) {
				candidateRight.innerHTML = `
					<strong class="electoral-candidate-num">100%</strong>
					<span class="electoral-candidate-name" style="font-size: 0.95rem; opacity: 0.85;">Total realizadas</span>
				`;
			} else {
				candidateRight.innerHTML = `
					<strong class="electoral-candidate-num">0</strong>
					<span class="electoral-candidate-name" style="opacity: 0.6;">Equipo técnico</span>
				`;
			}

			// 4. Barra Segmentada (asociada por data-username)
			let slicesHtml = '';
			const tecnicos = data.tecnicos || [];

			if (data.totalTareas === 0) {
				slicesHtml = `
					<div class="electoral-bar-slice is-pending"
						 style="width: 100%;"
						 data-title="Sin tareas registradas"
						 data-sub="0 tareas en este período"></div>
				`;
			} else {
				tecnicos.forEach(tec => {
					if (tec.porcentajeDelTotal > 0) {
						slicesHtml += `
							<div class="electoral-bar-slice"
								 style="width: ${tec.porcentajeDelTotal}%; background-color: ${tec.color};"
								 data-title="${escapeHtml(tec.nombreVisible)}"
								 data-sub="${tec.realizadas} tareas realizadas (${tec.porcentajeDelTotal}% del total)"
								 data-username="${escapeHtml(tec.username)}"></div>
						`;
					}
				});

				if (data.porcentajePendientes > 0) {
					slicesHtml += `
						<div class="electoral-bar-slice is-pending"
							 style="width: ${data.porcentajePendientes}%;"
							 data-title="Por asignar / Pendientes"
							 data-sub="${data.totalPendientes} tareas pendientes (${data.porcentajePendientes}% del total)"></div>
					`;
				}
			}

			bar.innerHTML = slicesHtml;

			// 5. Leyenda Inferior (asociada por data-username)
			let legendHtml = '';
			tecnicos.forEach((tec, idx) => {
				legendHtml += `
					<span class="electoral-chip" data-idx="${idx}" data-username="${escapeHtml(tec.username)}">
						<span class="electoral-dot" style="background-color: ${tec.color};"></span>
						<strong>${escapeHtml(tec.nombreVisible)}:</strong> ${tec.realizadas} (${tec.porcentajeDeRealizadas}%)
					</span>
				`;
			});

			if (data.totalPendientes > 0) {
				legendHtml += `
					<span class="electoral-chip" data-pending="true">
						<span class="electoral-dot" style="background-color: #94a3b8;"></span>
						<strong>Pendientes:</strong> ${data.totalPendientes}
					</span>
				`;
			}

			legend.innerHTML = legendHtml;

			// 6. Apartado Especial: Telefonista / Mesa de Entrada
			if (data.telefonista) {
				const telOp = widget.querySelector('#electoral-telefonia-operator');
				const telPeriodo = widget.querySelector('#electoral-tel-periodo');
				const telHoy = widget.querySelector('#electoral-tel-hoy');
				const telTotal = widget.querySelector('#electoral-tel-total');

				if (telOp && data.telefonista.operadorNombre) {
					telOp.textContent = data.telefonista.operadorNombre;
				}
				if (telPeriodo) {
					telPeriodo.textContent = data.telefonista.ingresadasPeriodo ?? 0;
				}
				if (telHoy) {
					telHoy.textContent = data.telefonista.ingresadasHoy ?? 0;
				}
				if (telTotal) {
					telTotal.textContent = data.telefonista.ingresadasTotal ?? 0;
				}
			}

			vincularInteractividadBarra();
		}

		function vincularInteractividadBarra() {
			const slices = bar.querySelectorAll('.electoral-bar-slice');

			slices.forEach(slice => {
				slice.addEventListener('mouseenter', e => {
					mostrarTooltip(slice, e);
				});

				slice.addEventListener('mousemove', e => {
					posicionarTooltip(e);
				});

				slice.addEventListener('mouseleave', () => {
					ocultarTooltip();
				});
			});

			// Hover en chips de la leyenda (vinculado por data-username o data-pending)
			const chips = legend.querySelectorAll('.electoral-chip');
			chips.forEach((chip) => {
				chip.addEventListener('mouseenter', () => {
					chip.classList.add('is-active');
					if (chip.getAttribute('data-pending') === 'true') {
						const pendingSlice = bar.querySelector('.electoral-bar-slice.is-pending');
						if (pendingSlice) pendingSlice.classList.add('is-hovered');
					} else {
						const u = chip.getAttribute('data-username');
						if (u) {
							const targetSlice = bar.querySelector(`.electoral-bar-slice[data-username="${CSS.escape(u)}"]`);
							if (targetSlice) targetSlice.classList.add('is-hovered');
						}
					}
				});

				chip.addEventListener('mouseleave', () => {
					chip.classList.remove('is-active');
					slices.forEach(s => s.classList.remove('is-hovered'));
				});
			});
		}

		function mostrarTooltip(slice, e) {
			const title = slice.getAttribute('data-title') || '';
			const sub = slice.getAttribute('data-sub') || '';

			tooltipTitle.textContent = title;
			tooltipSub.textContent = sub;
			tooltip.classList.add('is-visible');

			posicionarTooltip(e);
		}

		function posicionarTooltip(e) {
			const barRect = bar.getBoundingClientRect();
			const mouseX = e.clientX - barRect.left;
			const clampedX = Math.max(20, Math.min(barRect.width - 20, mouseX));

			tooltip.style.left = `${clampedX}px`;
		}

		function ocultarTooltip() {
			tooltip.classList.remove('is-visible');
		}

		function escapeHtml(text) {
			if (!text) return '';
			return text
				.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&#039;');
		}

		vincularInteractividadBarra();
	});
})();
