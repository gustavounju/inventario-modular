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
		let activeSegmentFilter = null;
		let hideTooltipTimer = null;

		function cancelarOcultarTooltip() {
			if (hideTooltipTimer) {
				clearTimeout(hideTooltipTimer);
				hideTooltipTimer = null;
			}
		}

		function programarOcultarTooltip() {
			cancelarOcultarTooltip();
			hideTooltipTimer = setTimeout(() => {
				ocultarTooltip();
			}, 220);
		}

		if (tooltip) {
			tooltip.addEventListener('mouseenter', () => {
				cancelarOcultarTooltip();
			});
			tooltip.addEventListener('mouseleave', () => {
				programarOcultarTooltip();
			});
		}

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
				limpiarFiltroTablero();
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

			// Si había un filtro activo (por segmento de anillo, por pendientes o por técnico), mantenerlo sin cerrarlo en el polling
			if (activeSegmentFilter) {
				const { ownerKey, ownerName, orbitType, etiqueta, itemObj } = activeSegmentFilter;
				let updatedItem = null;
				if (ownerKey === '__PENDING__') {
					const pend = data.pendientesDetalle;
					const list = orbitType === 'fuero' ? pend?.fueros : pend?.usuarios;
					updatedItem = list?.find(it => (it.etiqueta || '').trim().toLowerCase() === etiqueta.toLowerCase());
				} else {
					const tec = tecnicos.find(t => t.username === ownerKey);
					const list = orbitType === 'fuero' ? tec?.fueros : tec?.usuarios;
					updatedItem = list?.find(it => (it.etiqueta || '').trim().toLowerCase() === etiqueta.toLowerCase());
				}
				filtrarYMostrarTareasPorSegmento(ownerKey, ownerName, orbitType, updatedItem || itemObj, false);
			} else if (activeTechFilter === '__PENDING__') {
				filtrarTableroPendientes(false);
			} else if (activeTechFilter) {
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
					cancelarOcultarTooltip();
					mostrarTooltip(slice, e);
				});

				slice.addEventListener('mouseleave', () => {
					programarOcultarTooltip();
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
					cancelarOcultarTooltip();
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

				chip.addEventListener('mouseleave', () => {
					chip.classList.remove('is-active');
					slices.forEach(s => s.classList.remove('is-hovered'));
					programarOcultarTooltip();
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

		function renderOrbitalSystem(items, centerIcon, orbitType, ownerKey = '', ownerName = '') {
			const labelTitle = orbitType === 'fuero' ? '🏛️ Fueros' : '👤 Usuarios';

			if (!items || items.length === 0) {
				return `
					<div class="electoral-orbit-system" data-orbit-type="${orbitType}">
						<div class="electoral-orbit-planet">
							<svg viewBox="0 0 40 40" class="electoral-donut-svg">
								<circle cx="20" cy="20" r="15.9155" fill="none" stroke="#cbd5e1" stroke-width="5" stroke-dasharray="100 0" stroke-dashoffset="25" opacity="0.3"></circle>
								<text x="20" y="22.5" class="electoral-donut-center" text-anchor="middle" font-size="7.5">0</text>
							</svg>
							<span class="electoral-orbit-planet-label">${labelTitle}</span>
						</div>
						<div class="electoral-donut-hover-badge">Sin datos</div>
					</div>
				`;
			}

			const total = items.reduce((acc, it) => acc + it.cantidad, 0);

			// Anillo circular SVG con segmentos interactivos por porcentaje exacto
			let circlesHtml = `<circle cx="20" cy="20" r="15.9155" fill="none" stroke="#e2e8f0" stroke-width="5.2" opacity="0.25"></circle>`;
			let currentOffset = 25;
			items.forEach((item, idx) => {
				const exactPct = total > 0 ? (item.cantidad * 100.0) / total : 0;
				if (exactPct <= 0) return;
				const dashArray = `${exactPct} ${Math.max(0, 100 - exactPct)}`;
				circlesHtml += `
					<circle cx="20" cy="20" r="15.9155" fill="none"
							stroke="${item.color}"
							stroke-width="5.2"
							stroke-dasharray="${dashArray}"
							stroke-dashoffset="${currentOffset}"
							class="electoral-donut-slice"
							data-idx="${idx}"
							data-orbit-type="${orbitType}"
							data-owner="${escapeHtml(ownerKey)}"
							data-owner-name="${escapeHtml(ownerName)}"
							data-etiqueta="${escapeHtml(item.etiqueta)}"
							data-cantidad="${item.cantidad}"
							data-porcentaje="${item.porcentaje}"
							data-color="${item.color}">
						<title>${escapeHtml(item.etiqueta)}: ${item.cantidad} (${item.porcentaje}%) — Clic para ver tareas</title>
					</circle>
				`;
				currentOffset -= exactPct;
			});

			const planetSvg = `
				<svg viewBox="0 0 40 40" class="electoral-donut-svg">
					${circlesHtml}
					<text x="20" y="22.5" class="electoral-donut-center" data-total="${total}" text-anchor="middle" font-size="7.5">${total}</text>
				</svg>
			`;

			return `
				<div class="electoral-orbit-system" data-orbit-type="${orbitType}">
					<div class="electoral-orbit-planet">
						${planetSvg}
						<span class="electoral-orbit-planet-label">${labelTitle}</span>
					</div>
					<div class="electoral-donut-hover-badge" aria-live="polite"></div>
				</div>
			`;
		}

		function vincularInteractividadDonuts() {
			const systems = tooltip.querySelectorAll('.electoral-orbit-system');
			systems.forEach(sys => {
				const badge = sys.querySelector('.electoral-donut-hover-badge');
				const centerText = sys.querySelector('.electoral-donut-center');
				const total = centerText ? centerText.getAttribute('data-total') : '';
				const donutSlices = sys.querySelectorAll('.electoral-donut-slice');

				donutSlices.forEach(seg => {
					seg.addEventListener('mouseenter', () => {
						cancelarOcultarTooltip();
						const etiqueta = seg.getAttribute('data-etiqueta') || '';
						const cantidad = seg.getAttribute('data-cantidad') || '0';
						const porcentaje = seg.getAttribute('data-porcentaje') || '0';
						const color = seg.getAttribute('data-color') || '#3b82f6';

						donutSlices.forEach(s => {
							if (s === seg) {
								s.classList.add('is-hovered');
								s.classList.remove('is-dimmed');
							} else {
								s.classList.remove('is-hovered');
								s.classList.add('is-dimmed');
							}
						});

						if (centerText) {
							centerText.textContent = cantidad;
							centerText.style.fill = color;
						}

						if (badge) {
							badge.innerHTML = `
								<span class="electoral-satellite-dot" style="background-color: ${color};"></span>
								<span class="electoral-donut-badge-name">${escapeHtml(etiqueta)}</span>
								<strong class="electoral-donut-badge-qty">${cantidad} <small>(${porcentaje}%)</small></strong>
							`;
							badge.style.borderColor = color;
							badge.classList.add('is-visible');
						}
					});

					seg.addEventListener('mouseleave', () => {
						donutSlices.forEach(s => {
							s.classList.remove('is-hovered', 'is-dimmed');
						});
						if (centerText && total !== null) {
							centerText.textContent = total;
							centerText.style.fill = '';
						}
						if (badge) {
							badge.classList.remove('is-visible');
						}
					});

					seg.addEventListener('click', e => {
						e.stopPropagation();
						const idx = parseInt(seg.getAttribute('data-idx') || '-1', 10);
						const orbitType = seg.getAttribute('data-orbit-type') || 'fuero';
						const ownerKey = seg.getAttribute('data-owner') || '';
						const ownerName = seg.getAttribute('data-owner-name') || '';

						let itemObj = null;
						if (ownerKey === '__PENDING__') {
							const pend = currentData?.pendientesDetalle;
							const list = orbitType === 'fuero' ? pend?.fueros : pend?.usuarios;
							if (list && idx >= 0 && idx < list.length) {
								itemObj = list[idx];
							}
						} else {
							const tec = currentData?.tecnicos?.find(t => t.username === ownerKey);
							const list = orbitType === 'fuero' ? tec?.fueros : tec?.usuarios;
							if (list && idx >= 0 && idx < list.length) {
								itemObj = list[idx];
							}
						}

						if (itemObj) {
							ocultarTooltip();
							filtrarYMostrarTareasPorSegmento(ownerKey, ownerName, orbitType, itemObj);
						}
					});
				});
			});
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
						${renderOrbitalSystem(fueros, '🏛️', 'fuero', '__PENDING__', 'Tareas Activas / Pendientes')}

						<div class="electoral-orbit-center-core">
							<div class="electoral-core-title-row">
								<span class="electoral-dot" style="background-color: #94a3b8;"></span>
								<span class="electoral-tooltip-title">Tareas Activas / Pendientes</span>
							</div>
							<span class="electoral-tooltip-sub">${detalleSub} (${pct}% del total)</span>
							<span class="electoral-orbit-hint">👆 Clic en un color del anillo para ver sus tareas</span>
						</div>

						${renderOrbitalSystem(usuarios, '👤', 'usuario', '__PENDING__', 'Tareas Activas / Pendientes')}
					</div>
				`;
			} else {
				const tec = currentData?.tecnicos?.find(t => t.username === username);
				if (tec) {
					const fueros = tec.fueros || [];
					const usuarios = tec.usuarios || [];

					tooltip.innerHTML = `
						<div class="electoral-orbit-universe">
							${renderOrbitalSystem(fueros, '🏛️', 'fuero', tec.username, tec.nombreVisible)}

							<div class="electoral-orbit-center-core">
								<div class="electoral-core-title-row">
									<span class="electoral-dot" style="background-color: ${tec.color};"></span>
									<span class="electoral-tooltip-title">${escapeHtml(tec.nombreVisible)}</span>
								</div>
								<span class="electoral-tooltip-sub">${tec.realizadas} tareas realizadas (${tec.porcentajeDelTotal}% del total)</span>
								<span class="electoral-orbit-hint">👆 Clic en un color del anillo para ver sus tareas</span>
							</div>

							${renderOrbitalSystem(usuarios, '👤', 'usuario', tec.username, tec.nombreVisible)}
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

			vincularInteractividadDonuts();
			tooltip.classList.add('is-visible');
			posicionarTooltip(e, slice);
		}

		function posicionarTooltip(e, anchorEl) {
			const barRect = bar.getBoundingClientRect();
			let targetX = e ? (e.clientX - barRect.left) : (barRect.width / 2);
			if (anchorEl) {
				const anchorRect = anchorEl.getBoundingClientRect();
				targetX = (anchorRect.left + anchorRect.width / 2) - barRect.left;
			}
			const tooltipRect = tooltip.getBoundingClientRect();
			const half = (tooltipRect.width || 420) / 2;

			let clampedX;
			if (barRect.width <= (tooltipRect.width || 420)) {
				clampedX = barRect.width / 2;
			} else {
				clampedX = Math.max(half, Math.min(barRect.width - half, targetX));
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

			activeSegmentFilter = null;
			removerPanelSegmento();
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

		function filtrarTableroPendientes(toggle = true) {
			const cards = document.querySelectorAll('.kcard');
			if (!cards.length) return;

			if (toggle && activeTechFilter === '__PENDING__') {
				limpiarFiltroTablero();
				return;
			}

			activeSegmentFilter = null;
			removerPanelSegmento();
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

		function filtrarYMostrarTareasPorSegmento(ownerKey, ownerName, orbitType, itemObj, doScroll = true) {
			const etiqueta = (itemObj.etiqueta || '').trim();
			const color = itemObj.color || '#2563eb';
			const tareasLista = itemObj.tareas || [];
			const tipoLabel = orbitType === 'fuero' ? 'Fuero' : 'Usuario';

			activeTechFilter = `${ownerKey}:${orbitType}:${etiqueta}`;
			activeSegmentFilter = { ownerKey, ownerName, orbitType, etiqueta, itemObj };

			// Marcar slice y chip del técnico activo
			widget.querySelectorAll('.electoral-bar-slice').forEach(s => {
				const matchSlice = ownerKey === '__PENDING__'
					? s.getAttribute('data-pending') === 'true'
					: s.getAttribute('data-username') === ownerKey;
				s.classList.toggle('is-active-filter', matchSlice);
			});

			widget.querySelectorAll('.electoral-chip').forEach(c => {
				const matchChip = ownerKey === '__PENDING__'
					? c.getAttribute('data-pending') === 'true'
					: c.getAttribute('data-username') === ownerKey;
				c.classList.toggle('is-active-filter', matchChip);
			});

			// 1. Filtrar y resaltar en el tablero Kanban las tarjetas que coincidan
			const idsSegmento = new Set(tareasLista.map(t => String(t.id)));
			const cards = document.querySelectorAll('.kcard');
			let firstMatch = null;
			let matchCount = 0;

			cards.forEach(card => {
				const cardId = (card.getAttribute('data-id') || '').trim();
				const cardResp = (card.getAttribute('data-responsable') || '').trim().toLowerCase();
				const cardVal = (card.getAttribute(orbitType === 'fuero' ? 'data-fuero' : 'data-usuario') || '').trim().toLowerCase();

				let matches = false;
				if (idsSegmento.size > 0 && idsSegmento.has(cardId)) {
					matches = true;
				} else if (ownerKey === '__PENDING__') {
					const isPending = card.classList.contains('kcard--new') || card.classList.contains('kcard--progress') || !cardResp;
					matches = isPending && cardVal === etiqueta.toLowerCase();
				} else {
					matches = cardResp === ownerKey.toLowerCase() && cardVal === etiqueta.toLowerCase();
				}

				if (matches) {
					card.classList.remove('is-filtered-out');
					card.classList.add('is-highlighted-tech');
					card.style.setProperty('--tech-highlight-color', color);
					matchCount++;
					if (!firstMatch) firstMatch = card;
				} else {
					card.classList.add('is-filtered-out');
					card.classList.remove('is-highlighted-tech');
					card.style.removeProperty('--tech-highlight-color');
				}
			});

			actualizarBannerFiltro(`${ownerName} · ${tipoLabel}: ${etiqueta}`, itemObj.cantidad);
			mostrarPanelTareasSegmento(ownerName, orbitType, itemObj, doScroll);
		}

		function mostrarPanelTareasSegmento(ownerName, orbitType, itemObj, doScroll = true) {
			const existingPanel = document.getElementById('electoral-segment-tasks-panel');
			const prevScrollTop = existingPanel ? (existingPanel.querySelector('.electoral-segment-list')?.scrollTop || 0) : 0;
			removerPanelSegmento();
			const tareas = itemObj.tareas || [];
			const color = itemObj.color || '#2563eb';
			const iconoTipo = orbitType === 'fuero' ? '🏛️ Fuero' : '👤 Usuario';

			const panel = document.createElement('div');
			panel.className = 'electoral-segment-panel';
			panel.id = 'electoral-segment-tasks-panel';
			panel.style.setProperty('--segment-accent', color);

			let rowsHtml = '';
			if (tareas.length === 0) {
				rowsHtml = `<p class="electoral-segment-empty">No se encontraron tareas detalladas para este segmento.</p>`;
			} else {
				rowsHtml = `
					<div class="electoral-segment-list">
						${tareas.map(t => `
							<div class="electoral-segment-item" data-task-id="${t.id}">
								<div class="electoral-segment-item-top">
									<span class="electoral-segment-id">#${t.id}</span>
									<strong class="electoral-segment-title">${escapeHtml(t.titulo || 'Sin título')}</strong>
									<span class="electoral-segment-date">${escapeHtml(t.fecha || '')}</span>
								</div>
								<div class="electoral-segment-item-meta">
									<span>💻 ${escapeHtml(t.equipo || 'PC genérica')}</span>
									<span>👤 ${escapeHtml(t.solicitante || '—')}</span>
									<span>🏛️ ${escapeHtml(t.fuero || '—')}</span>
									${t.observacionesCierre ? `<span class="electoral-segment-note">✔ ${escapeHtml(t.observacionesCierre)}</span>` : ''}
								</div>
							</div>
						`).join('')}
					</div>
				`;
			}

			panel.innerHTML = `
				<div class="electoral-segment-header">
					<div class="electoral-segment-header-info">
						<span class="electoral-dot" style="background-color: ${color};"></span>
						<span class="electoral-segment-tech">${escapeHtml(ownerName)}</span>
						<span class="electoral-segment-sep">→</span>
						<span class="electoral-segment-badge" style="border-color: ${color};">
							${iconoTipo}: <strong>${escapeHtml(itemObj.etiqueta)}</strong>
						</span>
						<span class="electoral-segment-count">${itemObj.cantidad} ${itemObj.cantidad === 1 ? 'tarea' : 'tareas'} (${itemObj.porcentaje}%)</span>
					</div>
					<button type="button" class="electoral-segment-close-btn" title="Cerrar listado y quitar filtro">✕ Cerrar</button>
				</div>
				${rowsHtml}
			`;

			panel.querySelector('.electoral-segment-close-btn').addEventListener('click', () => {
				limpiarFiltroTablero();
			});

			// Al hacer clic en una tarea del listado, si está en el tablero actual, hacer scroll hacia ella
			panel.querySelectorAll('.electoral-segment-item').forEach(row => {
				row.addEventListener('click', () => {
					const tid = row.getAttribute('data-task-id');
					const cardEl = document.getElementById(`kcard-${tid}`);
					if (cardEl) {
						cardEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
					}
				});
			});

			widget.appendChild(panel);
			const newList = panel.querySelector('.electoral-segment-list');
			if (newList && prevScrollTop > 0) {
				newList.scrollTop = prevScrollTop;
			}
			if (doScroll) {
				panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
			}
		}

		function removerPanelSegmento() {
			const existing = document.getElementById('electoral-segment-tasks-panel');
			if (existing) {
				existing.remove();
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
			activeSegmentFilter = null;
			const cards = document.querySelectorAll('.kcard');
			cards.forEach(card => {
				card.classList.remove('is-filtered-out');
				card.classList.remove('is-highlighted-tech');
				card.style.removeProperty('--tech-highlight-color');
			});

			widget.querySelectorAll('.electoral-bar-slice').forEach(s => s.classList.remove('is-active-filter'));
			widget.querySelectorAll('.electoral-chip').forEach(c => c.classList.remove('is-active-filter'));

			removerBannerFiltro();
			removerPanelSegmento();
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
