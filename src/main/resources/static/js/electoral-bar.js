(() => {
	'use strict';

	document.addEventListener('DOMContentLoaded', () => {
		const widget = document.getElementById('electoral-tracker-card');
		if (!widget) return;

		const rangeButtons = widget.querySelectorAll('.electoral-range-btn');
		const bar = widget.querySelector('.electoral-bar');
		const tooltip = widget.querySelector('.electoral-tooltip');

		const candidateLeft = widget.querySelector('.electoral-candidate-left');
		const candidateRight = widget.querySelector('.electoral-candidate-right');
		const targetNum = widget.querySelector('.electoral-target-num');
		const subCenter = widget.querySelector('.electoral-sub-center');
		const legend = widget.querySelector('.electoral-legend');

		const activeRangeBtn = widget.querySelector('.electoral-range-btn.is-active');
		let currentRange = activeRangeBtn ? (activeRangeBtn.getAttribute('data-range') || 'HOY') : 'HOY';
		let currentData = null;
		let activeTechFilter = null;

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
				currentData = data;
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
			const pendInfo = data.pendientesDetalle;
			if (pendInfo && pendInfo.enProceso > 0) {
				subCenter.textContent = `${data.totalPendientes} activas (${pendInfo.sinAsignar} nuevas + ${pendInfo.enProceso} en proceso)`;
			} else {
				subCenter.textContent = `${data.totalPendientes} por asignar / pendientes`;
			}

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
								 data-username="${escapeHtml(tec.username)}"
								 data-color="${tec.color}"></div>
						`;
					}
				});

				if (data.porcentajePendientes > 0) {
					const pend = data.pendientesDetalle;
					const subDesc = (pend && pend.enProceso > 0)
						? `${data.totalPendientes} tareas activas (${pend.sinAsignar} nuevas + ${pend.enProceso} en proceso, ${data.porcentajePendientes}% del total)`
						: `${data.totalPendientes} tareas pendientes (${data.porcentajePendientes}% del total)`;
					slicesHtml += `
						<div class="electoral-bar-slice is-pending"
							 style="width: ${data.porcentajePendientes}%;"
							 data-title="Tareas Activas (Pendientes / En Proceso)"
							 data-sub="${subDesc}"
							 data-pending="true"></div>
					`;
				}
			}

			bar.innerHTML = slicesHtml;

			// 5. Leyenda Inferior (asociada por data-username)
			let legendHtml = '';
			tecnicos.forEach((tec, idx) => {
				legendHtml += `
					<span class="electoral-chip" data-idx="${idx}" data-username="${escapeHtml(tec.username)}" data-color="${tec.color}">
						<span class="electoral-dot" style="background-color: ${tec.color};"></span>
						<strong>${escapeHtml(tec.nombreVisible)}:</strong> ${tec.realizadas} (${tec.porcentajeDeRealizadas}%)
					</span>
				`;
			});

			if (data.totalPendientes > 0) {
				const pend = data.pendientesDetalle;
				const pendChipLabel = (pend && pend.enProceso > 0)
					? `${data.totalPendientes} (${pend.sinAsignar} nuevas, ${pend.enProceso} en proceso)`
					: `${data.totalPendientes}`;
				legendHtml += `
					<span class="electoral-chip" data-pending="true" title="Tareas activas del tablero (Nuevas y En proceso)">
						<span class="electoral-dot" style="background-color: #94a3b8;"></span>
						<strong>Activas / Pendientes:</strong> ${pendChipLabel}
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

			// Si había un filtro activo, reaplicarlo
			if (activeTechFilter) {
				const activeTec = tecnicos.find(t => t.username === activeTechFilter);
				if (activeTec) {
					filtrarTableroPorTecnico(activeTec.username, activeTec.nombreVisible, activeTec.color, false);
				} else {
					limpiarFiltroTablero();
				}
			}
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

				slice.addEventListener('click', () => {
					if (slice.getAttribute('data-pending') === 'true') {
						filtrarTableroPendientes();
					} else {
						const u = slice.getAttribute('data-username');
						const tec = currentData?.tecnicos?.find(t => t.username === u);
						if (tec) {
							filtrarTableroPorTecnico(tec.username, tec.nombreVisible, tec.color, true);
						}
					}
				});
			});

			// Chips de la leyenda
			const chips = legend.querySelectorAll('.electoral-chip');
			chips.forEach((chip) => {
				chip.addEventListener('mouseenter', e => {
					chip.classList.add('is-active');
					if (chip.getAttribute('data-pending') === 'true') {
						const pendingSlice = bar.querySelector('.electoral-bar-slice.is-pending');
						if (pendingSlice) {
							pendingSlice.classList.add('is-hovered');
							mostrarTooltip(pendingSlice, e);
						}
					} else {
						const u = chip.getAttribute('data-username');
						if (u) {
							const targetSlice = bar.querySelector(`.electoral-bar-slice[data-username="${CSS.escape(u)}"]`);
							if (targetSlice) {
								targetSlice.classList.add('is-hovered');
								mostrarTooltip(targetSlice, e);
							}
						}
					}
				});

				chip.addEventListener('mousemove', e => {
					posicionarTooltip(e);
				});

				chip.addEventListener('mouseleave', () => {
					chip.classList.remove('is-active');
					slices.forEach(s => s.classList.remove('is-hovered'));
					ocultarTooltip();
				});

				chip.addEventListener('click', () => {
					if (chip.getAttribute('data-pending') === 'true') {
						filtrarTableroPendientes();
					} else {
						const u = chip.getAttribute('data-username');
						const tec = currentData?.tecnicos?.find(t => t.username === u);
						if (tec) {
							filtrarTableroPorTecnico(tec.username, tec.nombreVisible, tec.color, true);
						}
					}
				});
			});
		}

		function renderOrbitalSystem(items, centerIcon, orbitType) {
			if (!items || items.length === 0) {
				return `
					<div class="electoral-orbit-system" style="--orbit-size: 140px;">
						<div class="electoral-orbit-ring"></div>
						<div class="electoral-orbit-planet">
							<svg viewBox="0 0 36 36" class="electoral-donut-svg">
								<circle cx="18" cy="18" r="15.9155" fill="none" stroke="#cbd5e1" stroke-width="4" stroke-dasharray="100 0" stroke-dashoffset="25" opacity="0.3"></circle>
								<text x="18" y="21" class="electoral-donut-center" text-anchor="middle" font-size="7">${escapeHtml(centerIcon || '—')}</text>
							</svg>
							<span class="electoral-orbit-planet-label">${orbitType === 'fuero' ? '🏛️ Fueros' : '👤 Usuarios'}</span>
						</div>
						<span class="electoral-orbit-empty">Sin datos</span>
					</div>
				`;
			}

			const total = items.reduce((acc, it) => acc + it.cantidad, 0);

			// Planeta central (Donut SVG con colores)
			let circlesHtml = `<circle cx="18" cy="18" r="15.9155" fill="none" stroke="#e2e8f0" stroke-width="4.5" opacity="0.3"></circle>`;
			let currentOffset = 25;
			items.forEach(item => {
				const pct = Math.max(0, Math.min(100, item.porcentaje));
				if (pct <= 0) return;
				const dashArray = `${pct} ${100 - pct}`;
				circlesHtml += `
					<circle cx="18" cy="18" r="15.9155" fill="none"
							stroke="${item.color}"
							stroke-width="4.5"
							stroke-dasharray="${dashArray}"
							stroke-dashoffset="${currentOffset}"
							class="electoral-donut-slice">
						<title>${escapeHtml(item.etiqueta)}: ${item.cantidad} (${item.porcentaje}%)</title>
					</circle>
				`;
				currentOffset -= pct;
			});

			const planetSvg = `
				<svg viewBox="0 0 36 36" class="electoral-donut-svg">
					${circlesHtml}
					<text x="18" y="20.5" class="electoral-donut-center" text-anchor="middle" font-size="7.5">${total}</text>
				</svg>
			`;

			// Satélites que giran alrededor del planeta central
			const n = items.length;
			const radius = n <= 2 ? 58 : (n === 3 ? 66 : 74);

			const satellitesHtml = items.map((it, idx) => {
				const angleDeg = (360 / n) * idx - 90;
				const rad = (angleDeg * Math.PI) / 180;
				const x = Math.round(radius * Math.cos(rad));
				const y = Math.round(radius * Math.sin(rad));

				return `
					<div class="electoral-satellite-node"
					     style="--target-x: ${x}px; --target-y: ${y}px;"
					     title="${escapeHtml(it.etiqueta)}: ${it.cantidad} (${it.porcentaje}%)">
						<div class="electoral-satellite-pill" style="border-color: ${it.color};">
							<span class="electoral-satellite-dot" style="background-color: ${it.color};"></span>
							<span class="electoral-satellite-name">${escapeHtml(it.etiqueta)}</span>
							<strong class="electoral-satellite-qty">${it.cantidad} <small>(${it.porcentaje}%)</small></strong>
						</div>
					</div>
				`;
			}).join('');

			return `
				<div class="electoral-orbit-system" style="--orbit-size: ${(radius + 18) * 2}px;">
					<div class="electoral-orbit-ring"></div>
					<div class="electoral-orbit-planet">
						${planetSvg}
						<span class="electoral-orbit-planet-label">${orbitType === 'fuero' ? '🏛️ Fueros' : '👤 Usuarios'}</span>
					</div>
					<div class="electoral-orbit-track">
						${satellitesHtml}
					</div>
				</div>
			`;
		}

		function mostrarTooltip(slice, e) {
			const isPending = slice.getAttribute('data-pending') === 'true';
			const username = slice.getAttribute('data-username');
			const titleFallback = slice.getAttribute('data-title') || '';
			const subFallback = slice.getAttribute('data-sub') || '';

			if (isPending) {
				const pend = currentData?.pendientesDetalle;
				const fueros = pend?.fueros || [];
				const usuarios = pend?.usuarios || [];
				const count = currentData?.totalPendientes ?? 0;
				const pct = currentData?.porcentajePendientes ?? 0;
				const detalleSub = (pend && pend.enProceso > 0)
					? `${count} tareas activas (${pend.sinAsignar} nuevas + ${pend.enProceso} en proceso)`
					: `${count} tareas pendientes`;

				tooltip.innerHTML = `
					<div class="electoral-orbit-universe">
						${renderOrbitalSystem(fueros, '🏛️', 'fuero')}

						<div class="electoral-orbit-center-core">
							<div class="electoral-core-title-row">
								<span class="electoral-dot" style="background-color: #94a3b8;"></span>
								<span class="electoral-tooltip-title">Tareas Activas / Pendientes</span>
							</div>
							<span class="electoral-tooltip-sub">${detalleSub} (${pct}% del total)</span>
							<span class="electoral-orbit-hint">👆 Haz clic para ver las ${count} tareas en el tablero</span>
						</div>

						${renderOrbitalSystem(usuarios, '👤', 'usuario')}
					</div>
				`;
			} else {
				const tec = currentData?.tecnicos?.find(t => t.username === username);
				if (tec) {
					const fueros = tec.fueros || [];
					const usuarios = tec.usuarios || [];

					tooltip.innerHTML = `
						<div class="electoral-orbit-universe">
							${renderOrbitalSystem(fueros, '🏛️', 'fuero')}

							<div class="electoral-orbit-center-core">
								<div class="electoral-core-title-row">
									<span class="electoral-dot" style="background-color: ${tec.color};"></span>
									<span class="electoral-tooltip-title">${escapeHtml(tec.nombreVisible)}</span>
								</div>
								<span class="electoral-tooltip-sub">${tec.realizadas} tareas realizadas (${tec.porcentajeDelTotal}% del total)</span>
								<span class="electoral-orbit-hint">👆 Haz clic para filtrar en el tablero</span>
							</div>

							${renderOrbitalSystem(usuarios, '👤', 'usuario')}
						</div>
					`;
				} else {
					tooltip.innerHTML = `
						<div class="electoral-orbit-universe">
							<div class="electoral-orbit-center-core">
								<span class="electoral-tooltip-title">${escapeHtml(titleFallback)}</span>
								<span class="electoral-tooltip-sub">${escapeHtml(subFallback)}</span>
							</div>
						</div>
					`;
				}
			}

			tooltip.classList.add('is-visible');
			posicionarTooltip(e);
		}

		function posicionarTooltip(e) {
			const barRect = bar.getBoundingClientRect();
			const mouseX = e.clientX - barRect.left;
			const tooltipRect = tooltip.getBoundingClientRect();
			const half = (tooltipRect.width || 480) / 2;

			let clampedX;
			if (barRect.width <= (tooltipRect.width || 480)) {
				clampedX = barRect.width / 2;
			} else {
				clampedX = Math.max(half, Math.min(barRect.width - half, mouseX));
			}

			tooltip.style.left = `${clampedX}px`;
		}

		function ocultarTooltip() {
			tooltip.classList.remove('is-visible');
		}

		// Filtrado y resaltado interactivo en el tablero Kanban
		function filtrarTableroPorTecnico(username, nombreVisible, color, toggle = true) {
			const cards = document.querySelectorAll('.kcard');
			if (!cards.length) return;

			if (toggle && activeTechFilter === username) {
				limpiarFiltroTablero();
				return;
			}

			activeTechFilter = username;

			// Marcar slice y chips activos
			widget.querySelectorAll('.electoral-bar-slice').forEach(s => {
				if (s.getAttribute('data-username') === username) {
					s.classList.add('is-active-filter');
				} else {
					s.classList.remove('is-active-filter');
				}
			});

			widget.querySelectorAll('.electoral-chip').forEach(c => {
				if (c.getAttribute('data-username') === username) {
					c.classList.add('is-active-filter');
				} else {
					c.classList.remove('is-active-filter');
				}
			});

			let firstMatch = null;
			let matchCount = 0;

			cards.forEach(card => {
				const cardResp = (card.getAttribute('data-responsable') || '').trim().toLowerCase();
				const matches = cardResp === username.toLowerCase();

				if (matches) {
					card.classList.remove('is-filtered-out');
					card.classList.add('is-highlighted-tech');
					card.style.setProperty('--tech-highlight-color', color || '#2563eb');
					matchCount++;
					if (!firstMatch) firstMatch = card;
				} else {
					card.classList.add('is-filtered-out');
					card.classList.remove('is-highlighted-tech');
					card.style.removeProperty('--tech-highlight-color');
				}
			});

			actualizarBannerFiltro(nombreVisible, matchCount);

			if (firstMatch) {
				firstMatch.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
			}
		}

		function filtrarTableroPendientes() {
			const cards = document.querySelectorAll('.kcard');
			if (!cards.length) return;

			if (activeTechFilter === '__PENDING__') {
				limpiarFiltroTablero();
				return;
			}

			activeTechFilter = '__PENDING__';

			widget.querySelectorAll('.electoral-bar-slice').forEach(s => {
				if (s.getAttribute('data-pending') === 'true') {
					s.classList.add('is-active-filter');
				} else {
					s.classList.remove('is-active-filter');
				}
			});

			widget.querySelectorAll('.electoral-chip').forEach(c => {
				if (c.getAttribute('data-pending') === 'true') {
					c.classList.add('is-active-filter');
				} else {
					c.classList.remove('is-active-filter');
				}
			});

			let firstMatch = null;
			let matchCount = 0;

			cards.forEach(card => {
				const cardResp = (card.getAttribute('data-responsable') || '').trim();
				// Una tarea activa/pendiente incluye tanto las Nuevas (sin asignar) como las que están En Proceso
				const isPending = card.classList.contains('kcard--new')
					|| card.classList.contains('kcard--progress')
					|| !cardResp;

				if (isPending) {
					card.classList.remove('is-filtered-out');
					card.classList.add('is-highlighted-tech');
					card.style.setProperty('--tech-highlight-color', '#94a3b8');
					matchCount++;
					if (!firstMatch) firstMatch = card;
				} else {
					card.classList.add('is-filtered-out');
					card.classList.remove('is-highlighted-tech');
					card.style.removeProperty('--tech-highlight-color');
				}
			});

			actualizarBannerFiltro('Tareas Activas (Nuevas y En Proceso)', matchCount);

			if (firstMatch) {
				firstMatch.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
			}
		}

		function actualizarBannerFiltro(nombre, cantidad) {
			removerBannerFiltro();
			const banner = document.createElement('span');
			banner.className = 'electoral-filter-active-pill';
			banner.id = 'electoral-active-filter-badge';
			banner.innerHTML = `
				<span>🎯 Filtrando por: <strong>${escapeHtml(nombre)}</strong> (${cantidad} tareas)</span>
				<button type="button" class="electoral-filter-clear-btn" title="Quitar filtro">✕</button>
			`;
			banner.querySelector('.electoral-filter-clear-btn').addEventListener('click', e => {
				e.stopPropagation();
				limpiarFiltroTablero();
			});

			subCenter.parentNode.insertBefore(banner, subCenter.nextSibling);
		}

		function removerBannerFiltro() {
			const existing = document.getElementById('electoral-active-filter-badge');
			if (existing) {
				existing.remove();
			}
		}

		function limpiarFiltroTablero() {
			activeTechFilter = null;
			const cards = document.querySelectorAll('.kcard');
			cards.forEach(card => {
				card.classList.remove('is-filtered-out');
				card.classList.remove('is-highlighted-tech');
				card.style.removeProperty('--tech-highlight-color');
			});

			widget.querySelectorAll('.electoral-bar-slice').forEach(s => s.classList.remove('is-active-filter'));
			widget.querySelectorAll('.electoral-chip').forEach(c => c.classList.remove('is-active-filter'));

			removerBannerFiltro();
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

		// Carga inicial de datos enriquecidos
		cargarRendimiento(currentRange);
	});
})();
