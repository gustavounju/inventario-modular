(function () {
	'use strict';

	const STORAGE_VIEW_KEY = 'inventario_equipos_vista_modo';
	const STORAGE_GROUP_KEY = 'inventario_equipos_plano_agrupacion';
	const STORAGE_SUBMODE_KEY = 'inventario_equipos_taller_submodo';
	const STORAGE_COMPACT_KEY = 'inventario_equipos_plano_compacto';
	const STORAGE_CONN_KEY = 'inventario_equipos_plano_conexiones';

	// Color institucional único y sobrio (sin paleta arcoíris)
	const INSTITUTIONAL_ACCENT = '#0284c7';

	function esc(str) {
		if (str == null) return '';
		return String(str).replace(/[&<>"']/g, function (m) {
			return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m];
		});
	}

	function floorKey(item) {
		return (item.edificio || 'Distribución General') + '||' + (item.piso || 'Oficinas por Fuero');
	}

	function floorLabel(edificio, piso) {
		if (edificio === 'Distribución General') {
			return '📍 Sin Edificio / Piso Asignado';
		}
		return '🏢 ' + edificio + ' — ' + piso;
	}

	function floorShortLabel(edificio, piso) {
		if (edificio === 'Distribución General') {
			return '📍 Sin Piso';
		}
		return edificio + ' · ' + piso;
	}

	function fueroPadreOf(item) {
		return (item.fueroPadre || item.fuero || 'Sin fuero').trim();
	}

	function subDependenciaOf(item) {
		return (item.subDependencia || item.oficina || 'Sede Principal').trim();
	}

	function initPlanoEquipos() {
		const data = Array.isArray(window.PLANO_EQUIPOS_DATA) ? window.PLANO_EQUIPOS_DATA : [];
		const btnPlano = document.getElementById('btn-vista-plano');
		const btnTabla = document.getElementById('btn-vista-tabla');
		const containerPlano = document.getElementById('vista-plano-container');
		const containerTabla = document.getElementById('vista-tabla-container');

		if (!btnPlano || !btnTabla || !containerPlano || !containerTabla) {
			return;
		}

		const btnVolverMosaico = document.getElementById('btn-volver-mosaico');
		const btnSubMatriz = document.getElementById('btn-submodo-matriz');
		const btnSubCroquis = document.getElementById('btn-submodo-croquis');
		const btnGroupFuero = document.getElementById('btn-group-fuero');
		const btnGroupPiso = document.getElementById('btn-group-piso');
		const btnSyncAd = document.getElementById('btn-sync-ad-estructura');
		const btnConn = document.getElementById('btn-plano-connections');
		const syncBanner = document.getElementById('plano-sync-banner');
		const floorTabsEl = document.getElementById('plano-floor-tabs');
		const mosaicoContainer = document.getElementById('plano-mosaico-container');
		const explorerContainer = document.getElementById('plano-explorer-container');
		const sidebarFuerosEl = document.getElementById('plano-sidebar-fueros');
		const stageEl = document.getElementById('plano-stage');
		const svgEl = document.getElementById('plano-connections-svg');
		const canvasEl = document.getElementById('plano-canvas');
		const quickSearchEl = document.getElementById('plano-quick-search');
		const btnCompact = document.getElementById('btn-plano-compact');

		let groupBy = localStorage.getItem(STORAGE_GROUP_KEY) || 'fuero';
		let subMode = localStorage.getItem(STORAGE_SUBMODE_KEY) || 'matriz'; // 'matriz' | 'croquis'
		let showMosaico = true; // Nivel 1 por defecto: Mosaico general de tarjetas de fueros
		let activeFloor = 'ALL';
		let activeFuero = 'ALL';
		let quickFilter = '';
		let isCompact = localStorage.getItem(STORAGE_COMPACT_KEY) === 'true';
		let showAllConnections = localStorage.getItem(STORAGE_CONN_KEY) === 'true';

		let deskElementsById = new Map();
		let printerElementsByIp = new Map();
		let currentFilteredItems = [];

		let tooltipEl = document.getElementById('plano-floating-tooltip');
		if (!tooltipEl) {
			tooltipEl = document.createElement('div');
			tooltipEl.id = 'plano-floating-tooltip';
			tooltipEl.className = 'plano-tooltip';
			document.body.appendChild(tooltipEl);
		}

		function setViewMode(mode) {
			const isPlano = mode === 'plano';
			containerPlano.style.display = isPlano ? 'flex' : 'none';
			containerTabla.style.display = isPlano ? 'none' : 'block';
			btnPlano.classList.toggle('is-active', isPlano);
			btnTabla.classList.toggle('is-active', !isPlano);
			localStorage.setItem(STORAGE_VIEW_KEY, mode);
			if (isPlano) {
				requestAnimationFrame(drawAmbientConnections);
			}
		}

		function setSubMode(mode) {
			subMode = mode === 'croquis' ? 'croquis' : 'matriz';
			localStorage.setItem(STORAGE_SUBMODE_KEY, subMode);
			if (btnSubMatriz) btnSubMatriz.classList.toggle('is-active', subMode === 'matriz');
			if (btnSubCroquis) btnSubCroquis.classList.toggle('is-active', subMode === 'croquis');
			if (btnConn) btnConn.style.display = subMode === 'croquis' ? 'inline-flex' : 'none';
			if (btnCompact) btnCompact.style.display = subMode === 'croquis' ? 'inline-flex' : 'none';
			renderWorkspace();
		}

		function setGroupBy(mode) {
			groupBy = mode === 'piso' ? 'piso' : 'fuero';
			localStorage.setItem(STORAGE_GROUP_KEY, groupBy);
			if (btnGroupFuero) btnGroupFuero.classList.toggle('is-active', groupBy === 'fuero');
			if (btnGroupPiso) btnGroupPiso.classList.toggle('is-active', groupBy === 'piso');
			if (groupBy === 'piso') {
				showMosaico = false;
			}
			renderWorkspace();
		}

		function openMosaicoView() {
			showMosaico = true;
			activeFuero = 'ALL';
			groupBy = 'fuero';
			if (btnGroupFuero) btnGroupFuero.classList.add('is-active');
			if (btnGroupPiso) btnGroupPiso.classList.remove('is-active');
			if (quickSearchEl && quickFilter) {
				quickFilter = '';
				quickSearchEl.value = '';
			}
			renderWorkspace();
		}

		function selectFueroInExplorer(fueroName) {
			activeFuero = fueroName || 'ALL';
			showMosaico = false;
			renderWorkspace();
		}

		function applyCompactState() {
			containerPlano.classList.toggle('is-compact', isCompact);
			if (btnCompact) {
				btnCompact.textContent = isCompact ? '🔍 Detallado' : '📐 Compacto';
			}
			requestAnimationFrame(drawAmbientConnections);
		}

		function applyConnButtonState() {
			if (!btnConn) return;
			btnConn.classList.toggle('is-active', showAllConnections);
			btnConn.textContent = showAllConnections ? '🔗 Conexiones: ON' : '🔗 Conexiones';
		}

		function getFilteredItems(ignoreActiveFuero) {
			const q = quickFilter.trim().toLowerCase();
			return data.filter(function (item) {
				if (activeFloor !== 'ALL' && floorKey(item) !== activeFloor) {
					return false;
				}
				if (!ignoreActiveFuero && activeFuero !== 'ALL' && fueroPadreOf(item) !== activeFuero) {
					return false;
				}
				if (q) {
					const haystack = [
						item.nombre,
						item.usuario,
						item.usuarioRaw,
						item.fuero,
						item.fueroPadre,
						item.subDependencia,
						item.oficina,
						item.ip,
						item.mac,
						item.sn,
						item.so,
						item.edificio,
						item.piso,
						item.impresoraNombre,
						item.impresoraIp,
						item.impresoraTipo
					].join(' ').toLowerCase();
					if (!haystack.includes(q)) {
						return false;
					}
				}
				return true;
			});
		}

		function findTargetPrinterElement(pcItem) {
			if (!pcItem || pcItem.esImpresora || pcItem.impresoraTipo !== 'RED') {
				return null;
			}
			if (pcItem.impresoraRedId != null && deskElementsById.has(String(pcItem.impresoraRedId))) {
				return deskElementsById.get(String(pcItem.impresoraRedId));
			}
			if (pcItem.impresoraIp && printerElementsByIp.has(pcItem.impresoraIp.trim())) {
				return printerElementsByIp.get(pcItem.impresoraIp.trim());
			}
			return null;
		}

		function findConnectedPcElements(printerItem) {
			if (!printerItem || !printerItem.esImpresora) return [];
			const printerIdStr = String(printerItem.id);
			const printerIp = (printerItem.ip || '').trim();
			const matched = [];

			currentFilteredItems.forEach(function (it) {
				if (it.esImpresora || it.impresoraTipo !== 'RED') return;
				const matchById = it.impresoraRedId != null && String(it.impresoraRedId) === printerIdStr;
				const matchByIp = printerIp && it.impresoraIp && it.impresoraIp.trim() === printerIp;
				if (matchById || matchByIp) {
					const el = deskElementsById.get(String(it.id));
					if (el) matched.push(el);
				}
			});
			return matched;
		}

		function createCurveSvg(fromEl, toEl, isAmbient) {
			if (!stageEl || !fromEl || !toEl) return null;
			const stageRect = stageEl.getBoundingClientRect();
			const r1 = fromEl.getBoundingClientRect();
			const r2 = toEl.getBoundingClientRect();

			const x1 = r1.left + r1.width / 2 - stageRect.left;
			const y1 = r1.top + r1.height / 2 - stageRect.top;
			const x2 = r2.left + r2.width / 2 - stageRect.left;
			const y2 = r2.top + r2.height / 2 - stageRect.top;

			const dx = x2 - x1;
			const dy = y2 - y1;
			const dist = Math.hypot(dx, dy);
			const curveOffset = Math.min(80, Math.max(24, dist * 0.22));
			const cx = (x1 + x2) / 2;
			const cy = (y1 + y2) / 2 - curveOffset;

			const ns = 'http://www.w3.org/2000/svg';
			const g = document.createElementNS(ns, 'g');

			const path = document.createElementNS(ns, 'path');
			path.setAttribute('d', 'M ' + x1.toFixed(1) + ' ' + y1.toFixed(1) + ' Q ' + cx.toFixed(1) + ' ' + cy.toFixed(1) + ' ' + x2.toFixed(1) + ' ' + y2.toFixed(1));
			path.setAttribute('class', 'plano-conn-path' + (isAmbient ? ' is-ambient' : ''));
			g.appendChild(path);

			if (!isAmbient) {
				const c1 = document.createElementNS(ns, 'circle');
				c1.setAttribute('cx', x1.toFixed(1));
				c1.setAttribute('cy', y1.toFixed(1));
				c1.setAttribute('r', '4');
				c1.setAttribute('class', 'plano-conn-dot');
				g.appendChild(c1);

				const c2 = document.createElementNS(ns, 'circle');
				c2.setAttribute('cx', x2.toFixed(1));
				c2.setAttribute('cy', y2.toFixed(1));
				c2.setAttribute('r', '5');
				c2.setAttribute('class', 'plano-conn-dot');
				g.appendChild(c2);
			}
			return g;
		}

		function clearHighlights() {
			if (!canvasEl) return;
			canvasEl.querySelectorAll('.plano-desk.is-conn-highlight').forEach(function (el) {
				el.classList.remove('is-conn-highlight');
			});
		}

		function drawAmbientConnections() {
			if (!svgEl) return;
			svgEl.innerHTML = '';
			if (!showAllConnections || showMosaico || subMode !== 'croquis') return;

			currentFilteredItems.forEach(function (item) {
				if (item.esImpresora || item.impresoraTipo !== 'RED') return;
				const pcEl = deskElementsById.get(String(item.id));
				const prEl = findTargetPrinterElement(item);
				if (pcEl && prEl) {
					const g = createCurveSvg(pcEl, prEl, true);
					if (g) svgEl.appendChild(g);
				}
			});
		}

		function highlightConnectionsFor(item, sourceEl) {
			if (!svgEl || subMode !== 'croquis') return;
			clearHighlights();
			drawAmbientConnections();

			if (item.esImpresora) {
				const pcs = findConnectedPcElements(item);
				if (pcs.length > 0) {
					sourceEl.classList.add('is-conn-highlight');
					pcs.forEach(function (pcEl) {
						pcEl.classList.add('is-conn-highlight');
						const g = createCurveSvg(pcEl, sourceEl, false);
						if (g) svgEl.appendChild(g);
					});
				}
			} else if (item.impresoraTipo === 'RED') {
				const prEl = findTargetPrinterElement(item);
				if (prEl) {
					sourceEl.classList.add('is-conn-highlight');
					prEl.classList.add('is-conn-highlight');
					const g = createCurveSvg(sourceEl, prEl, false);
					if (g) svgEl.appendChild(g);
				}
			}
		}

		function buildFuerosSummaryMap(itemsList) {
			const fuerosMap = new Map();
			itemsList.forEach(function (item) {
				const fp = fueroPadreOf(item);
				if (!fuerosMap.has(fp)) {
					fuerosMap.set(fp, {
						fueroPadre: fp,
						rooms: new Map(),
						totalEquipos: 0,
						pcs: 0,
						impresoras: 0,
						sincronizados: 0,
						pendientes: 0,
						taller: 0
					});
				}
				const fObj = fuerosMap.get(fp);
				fObj.totalEquipos++;
				if (item.esImpresora) {
					fObj.impresoras++;
				} else {
					fObj.pcs++;
					if (item.estadoCodigo === 'SINCRONIZADO' || item.estadoCodigo === 'DISPONIBLE') {
						fObj.sincronizados++;
					} else if (item.estadoCodigo === 'PENDIENTE') {
						fObj.pendientes++;
					} else if (item.estadoCodigo === 'TALLER') {
						fObj.taller++;
					}
				}

				const sub = subDependenciaOf(item);
				const rk = sub.toLowerCase();
				if (!fObj.rooms.has(rk)) {
					fObj.rooms.set(rk, {
						titulo: sub,
						ubicacionFisica: item.ubicacionFisica || '',
						responsable: item.responsableOficina || '',
						items: [],
						pcs: 0,
						impresoras: 0
					});
				}
				const rObj = fObj.rooms.get(rk);
				if (!rObj.ubicacionFisica && item.ubicacionFisica) rObj.ubicacionFisica = item.ubicacionFisica;
				if (!rObj.responsable && item.responsableOficina) rObj.responsable = item.responsableOficina;
				rObj.items.push(item);
				if (item.esImpresora) rObj.impresoras++; else rObj.pcs++;
			});

			return Array.from(fuerosMap.values()).sort(function (a, b) {
				if (a.fueroPadre.toLowerCase() === 'sin fuero') return 1;
				if (b.fueroPadre.toLowerCase() === 'sin fuero') return -1;
				return a.fueroPadre.localeCompare(b.fueroPadre, 'es', { sensitivity: 'base', numeric: true });
			});
		}

		// =========================================================================
		// NIVEL 1: RENDERIZADO DEL MOSAICO DE FUEROS (Propuesta B)
		// =========================================================================
		function renderMosaicoFueros() {
			if (!mosaicoContainer) return;
			const allFueros = buildFuerosSummaryMap(data);

			if (allFueros.length === 0) {
				mosaicoContainer.innerHTML = '<div class="plano-empty">🏛️ No hay equipos registrados para mostrar en el mosaico de fueros.</div>';
				return;
			}

			let html = ''
				+ '<div class="fuero-mosaico-header-bar">'
				+ '  <div class="fuero-mosaico-header-title">🏛️ Mapa General por Fueros (' + allFueros.length + ' fueros · ' + data.length + ' activos)</div>'
				+ '  <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">'
				+ '    <span class="fuero-mosaico-header-hint">👆 Haga clic en cualquier tarjeta de Fuero para abrir su consola técnica de taller</span>'
				+ '    <button type="button" id="btn-ver-todos-explorador" class="plano-action-all-btn" style="padding:5px 11px; border-radius:7px; border:1px solid var(--border-color); background:var(--bg-subtle); color:var(--text-primary); font-size:0.77rem; font-weight:700; cursor:pointer;">📋 Ver todos los fueros juntos</button>'
				+ '  </div>'
				+ '</div>'
				+ '<div class="fuero-mosaico-grid">';

			allFueros.forEach(function (fObj) {
				const roomList = Array.from(fObj.rooms.values()).sort(function (a, b) {
					return a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base', numeric: true });
				});
				const maxChips = 4;
				let chipsHtml = '';
				roomList.slice(0, maxChips).forEach(function (r) {
					chipsHtml += '<span class="fuero-dep-chip">' + esc(r.titulo) + ' (' + r.items.length + ')</span>';
				});
				if (roomList.length > maxChips) {
					chipsHtml += '<span class="fuero-dep-chip">+' + (roomList.length - maxChips) + ' más</span>';
				}

				const hasPending = fObj.pendientes > 0;
				html += ''
					+ '<button type="button" class="fuero-mosaico-card' + (hasPending ? ' has-pending' : '') + '" data-fuero-target="' + esc(fObj.fueroPadre) + '">'
					+ '  <div>'
					+ '    <div class="fuero-mosaico-card-top">'
					+ '      <div>'
					+ '        <h3 class="fuero-mosaico-name">🏛️ ' + esc(fObj.fueroPadre) + '</h3>'
					+ '        <div class="fuero-mosaico-sub">' + roomList.length + (roomList.length === 1 ? ' dependencia' : ' dependencias') + '</div>'
					+ '      </div>'
					+ '      <span class="fuero-mosaico-total-badge">' + fObj.totalEquipos + (fObj.totalEquipos === 1 ? ' activo' : ' activos') + '</span>'
					+ '    </div>'
					+ '  </div>'
					+ '  <div class="fuero-mosaico-stats">'
					+ '    <div class="fuero-stat-item">🖥️ <span>' + fObj.pcs + ' PC' + (fObj.pcs === 1 ? '' : 's') + '</span></div>'
					+ '    <div class="fuero-stat-item">🖨️ <span>' + fObj.impresoras + ' Impresora' + (fObj.impresoras === 1 ? '' : 's') + '</span></div>'
					+ '    <div class="fuero-stat-item"><span class="plano-led is-sincronizado"></span> <span>' + fObj.sincronizados + ' Sincr.</span></div>'
					+ '    <div class="fuero-stat-item">'
					+        (hasPending
								? '<span class="plano-led is-pendiente"></span> <span style="color:#d97706;font-weight:800;">' + fObj.pendientes + ' Pend. Gemelo</span>'
								: '<span class="plano-led is-esperando"></span> <span>0 Pendientes</span>')
					+ '    </div>'
					+ '  </div>'
					+ '  <div class="fuero-mosaico-deps">' + chipsHtml + '</div>'
					+ '</button>';
			});

			html += '</div>';
			mosaicoContainer.innerHTML = html;

			mosaicoContainer.querySelectorAll('.fuero-mosaico-card').forEach(function (card) {
				card.addEventListener('click', function () {
					const target = card.getAttribute('data-fuero-target');
					selectFueroInExplorer(target);
				});
			});

			const btnAllExp = document.getElementById('btn-ver-todos-explorador');
			if (btnAllExp) {
				btnAllExp.addEventListener('click', function () {
					selectFueroInExplorer('ALL');
				});
			}
		}

		// =========================================================================
		// NIVEL 2: DIRECTORIO LATERAL DE FUEROS (Propuesta A)
		// =========================================================================
		function renderSidebarFueros() {
			if (!sidebarFuerosEl) return;
			const baseItems = getFilteredItems(true);
			const fuerosList = buildFuerosSummaryMap(baseItems);

			let html = ''
				+ '<div class="plano-sidebar-title">🏛️ Directorio de Fueros</div>'
				+ '<button type="button" class="plano-sidebar-item' + (activeFuero === 'ALL' ? ' is-active' : '') + '" data-sidebar-fuero="ALL">'
				+ '  <span class="plano-sidebar-item-name">📋 Todos los Fueros</span>'
				+ '  <span class="plano-sidebar-item-badges"><span class="plano-sidebar-count">' + baseItems.length + '</span></span>'
				+ '</button>';

			fuerosList.forEach(function (fObj) {
				const isAct = activeFuero === fObj.fueroPadre;
				const pendingDot = fObj.pendientes > 0
					? '<span class="plano-led is-pendiente" title="' + fObj.pendientes + ' equipos pendientes de gemelo"></span>'
					: '';
				html += ''
					+ '<button type="button" class="plano-sidebar-item' + (isAct ? ' is-active' : '') + '" data-sidebar-fuero="' + esc(fObj.fueroPadre) + '" title="' + esc(fObj.fueroPadre) + '">'
					+ '  <span class="plano-sidebar-item-name">' + esc(fObj.fueroPadre) + '</span>'
					+ '  <span class="plano-sidebar-item-badges">' + pendingDot + '<span class="plano-sidebar-count">' + fObj.totalEquipos + '</span></span>'
					+ '</button>';
			});

			sidebarFuerosEl.innerHTML = html;
			sidebarFuerosEl.querySelectorAll('.plano-sidebar-item').forEach(function (btn) {
				btn.addEventListener('click', function () {
					activeFuero = btn.getAttribute('data-sidebar-fuero') || 'ALL';
					renderSidebarFueros();
					renderDetailCanvas();
				});
			});
		}

		function ledClassFor(item) {
			switch (item.estadoCodigo) {
				case 'SINCRONIZADO': return 'is-sincronizado';
				case 'DISPONIBLE': return 'is-disponible';
				case 'PENDIENTE': return 'is-pendiente';
				case 'IMPRESORA': return 'is-impresora';
				case 'TALLER': return 'is-taller';
				default: return 'is-esperando';
			}
		}

		function iconFor(item) {
			if (item.esImpresora) return '🖨️';
			if (item.estadoCodigo === 'TALLER') return '🛠️';
			return '🖥️';
		}

		function formatPrinterTooltip(item) {
			if (item.esImpresora) {
				const pcs = findConnectedPcElements(item);
				return pcs.length + (pcs.length === 1 ? ' PC conectada en plano' : ' PCs conectadas en plano');
			}
			if (item.impresoraTipo === 'RED') {
				const label = item.impresoraNombre || 'Impresora en Red';
				return '🌐 Red → ' + label + (item.impresoraIp ? ' (' + item.impresoraIp + ')' : '');
			}
			if (item.impresoraTipo === 'LOCAL') {
				return '🔌 Local (USB) — ' + (item.impresoraNombre || 'Conectada');
			}
			return '';
		}

		function showTooltip(item, event) {
			const estadoDot = '<span class="plano-led ' + ledClassFor(item) + '"></span>';
			const hint = item.esImpresora
				? 'Clic para ver equipos vinculados a esta impresora'
				: (item.estadoCodigo === 'PENDIENTE'
					? 'Clic para abrir ficha y registrar Gemelo Digital'
					: 'Clic para abrir ficha técnica completa');
			const printerInfo = formatPrinterTooltip(item);

			tooltipEl.innerHTML = ''
				+ '<div class="plano-tooltip-title"><span>' + iconFor(item) + ' ' + esc(item.nombre) + '</span>' + estadoDot + '</div>'
				+ '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Estado:</span><span class="plano-tooltip-val">' + esc(item.estadoLabel) + '</span></div>'
				+ '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Usuario:</span><span class="plano-tooltip-val">' + esc(item.esImpresora ? 'Compartida en Red' : (item.usuario || 'Sin asignar')) + '</span></div>'
				+ '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Fuero:</span><span class="plano-tooltip-val">' + esc(fueroPadreOf(item)) + '</span></div>'
				+ '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Dependencia:</span><span class="plano-tooltip-val">' + esc(subDependenciaOf(item)) + '</span></div>'
				+ (item.ubicacionFisica ? '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Ubic. Física:</span><span class="plano-tooltip-val">' + esc(item.ubicacionFisica) + '</span></div>' : '')
				+ (printerInfo ? '<div class="plano-tooltip-row"><span class="plano-tooltip-label">' + (item.esImpresora ? 'Conexiones:' : 'Impresora:') + '</span><span class="plano-tooltip-val" style="color:#7dd3fc;">' + esc(printerInfo) + '</span></div>' : '')
				+ (item.ip ? '<div class="plano-tooltip-row"><span class="plano-tooltip-label">IP:</span><span class="plano-tooltip-val" style="font-family:monospace;">' + esc(item.ip) + '</span></div>' : '')
				+ (item.mac ? '<div class="plano-tooltip-row"><span class="plano-tooltip-label">MAC:</span><span class="plano-tooltip-val" style="font-family:monospace;">' + esc(item.mac) + '</span></div>' : '')
				+ '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Sistema:</span><span class="plano-tooltip-val">' + esc(item.so) + '</span></div>'
				+ (item.ultimoReporte ? '<div class="plano-tooltip-row"><span class="plano-tooltip-label">Últ. reporte:</span><span class="plano-tooltip-val">' + esc(item.ultimoReporte) + '</span></div>' : '')
				+ '<div class="plano-tooltip-hint">👆 ' + esc(hint) + '</div>';

			tooltipEl.classList.add('is-visible');
			positionTooltip(event);
		}

		function positionTooltip(event) {
			const pad = 14;
			const rect = tooltipEl.getBoundingClientRect();
			let left = event.clientX + pad;
			let top = event.clientY + pad;

			if (left + rect.width > window.innerWidth - 10) {
				left = Math.max(10, event.clientX - rect.width - pad);
			}
			if (top + rect.height > window.innerHeight - 10) {
				top = Math.max(10, event.clientY - rect.height - pad);
			}
			tooltipEl.style.left = left + 'px';
			tooltipEl.style.top = top + 'px';
		}

		function hideTooltip() {
			tooltipEl.classList.remove('is-visible');
		}

		function buildPrinterBadgeHtml(item) {
			if (item.esImpresora) return '';
			if (item.impresoraTipo === 'RED') {
				const target = item.impresoraNombre || item.impresoraIp || 'En Red';
				return '<div class="plano-desk-printer is-red" title="Imprime en Red: ' + esc(target) + (item.impresoraIp ? ' (' + esc(item.impresoraIp) + ')' : '') + '">🌐 <span>' + esc(target) + '</span></div>';
			}
			if (item.impresoraTipo === 'LOCAL') {
				const target = item.impresoraNombre || 'USB Local';
				return '<div class="plano-desk-printer is-local" title="Impresora Local (USB): ' + esc(target) + '">🔌 <span>USB · ' + esc(target) + '</span></div>';
			}
			return '<span style="font-size:0.72rem; color:var(--text-muted);">—</span>';
		}

		function buildDeskTile(item) {
			const desk = document.createElement(item.esImpresora ? 'button' : 'a');
			desk.className = 'plano-desk' + (item.esImpresora ? ' is-impresora' : '');
			desk.setAttribute('data-equipo-id', String(item.id));
			if (item.esImpresora) {
				desk.type = 'button';
				desk.style.textAlign = 'left';
				desk.addEventListener('click', function () {
					hideTooltip();
					if (typeof window.abrirModalVinculados === 'function') {
						window.abrirModalVinculados(item.id);
					}
				});
			} else {
				desk.href = item.estadoCodigo === 'PENDIENTE'
					? ('/admin/equipos/' + item.id + '#tab-gemelo')
					: ('/admin/equipos/' + item.id);
			}

			const userText = item.esImpresora
				? 'Impresora compartida'
				: (item.usuario ? item.usuario : '⚠️ Sin asignar');
			const userClass = (!item.esImpresora && !item.usuario) ? 'plano-desk-user is-unassigned' : 'plano-desk-user';

			let specsHtml = '<div class="plano-desk-specs">';
			if (!item.esImpresora && item.so && item.so !== 'Sin dato') {
				specsHtml += '<span class="plano-desk-spec-pill" title="Sistema Operativo">💿 ' + esc(item.so) + '</span>';
			}
			if (item.mac) {
				specsHtml += '<span class="plano-desk-spec-pill" title="Dirección MAC">🔌 ' + esc(item.mac) + '</span>';
			} else if (item.sn) {
				specsHtml += '<span class="plano-desk-spec-pill" title="Serie Motherboard / Activo">🔢 ' + esc(item.sn) + '</span>';
			}
			if (item.ultimoReporte) {
				specsHtml += '<span class="plano-desk-spec-pill" title="Último reporte">🕒 ' + esc(item.ultimoReporte) + '</span>';
			}
			specsHtml += '</div>';

			const prBadge = item.esImpresora ? '' : (item.impresoraTipo ? buildPrinterBadgeHtml(item) : '');

			desk.innerHTML = ''
				+ '<div class="plano-desk-top">'
				+ '  <div class="plano-desk-name-wrap">'
				+ '    <span class="plano-desk-icon">' + iconFor(item) + '</span>'
				+ '    <span class="plano-desk-name">' + esc(item.nombre) + '</span>'
				+ '  </div>'
				+ '  <span class="plano-led ' + ledClassFor(item) + '" title="' + esc(item.estadoLabel) + '"></span>'
				+ '</div>'
				+ '<div class="' + userClass + '">' + esc(userText) + '</div>'
				+ specsHtml
				+ prBadge
				+ '<div class="plano-desk-footer">'
				+ '  <span class="plano-desk-ip">' + esc(item.ip || 'Sin IP') + '</span>'
				+ (item.estadoCodigo === 'PENDIENTE' ? '<span class="plano-desk-tag">Gemelo</span>' : '')
				+ '</div>';

			deskElementsById.set(String(item.id), desk);
			if (item.esImpresora && item.ip && item.ip.trim()) {
				printerElementsByIp.set(item.ip.trim(), desk);
			}

			desk.addEventListener('mouseenter', function (ev) {
				showTooltip(item, ev);
				highlightConnectionsFor(item, desk);
			});
			desk.addEventListener('mousemove', function (ev) { positionTooltip(ev); });
			desk.addEventListener('mouseleave', function () {
				hideTooltip();
				clearHighlights();
				drawAmbientConnections();
			});
			return desk;
		}

		// =========================================================================
		// NIVEL 3: MATRIZ TÉCNICA DE TALLER POR VOCALÍA / OFICINA (Propuesta C)
		// =========================================================================
		function buildRoomMatrixElement(roomTitle, ubicacionFisica, responsable, items, pcsCount, impCount) {
			const box = document.createElement('article');
			box.className = 'taller-room-box';

			let metaHtml = '';
			if (ubicacionFisica) {
				metaHtml += '<span class="plano-floor-badge">📍 ' + esc(ubicacionFisica) + '</span>';
			}
			if (responsable) {
				metaHtml += '<span class="plano-floor-badge">👤 ' + esc(responsable) + '</span>';
			}
			if (pcsCount > 0) {
				metaHtml += '<span class="plano-floor-badge">🖥️ ' + pcsCount + ' PC' + (pcsCount === 1 ? '' : 's') + '</span>';
			}
			if (impCount > 0) {
				metaHtml += '<span class="plano-floor-badge">🖨️ ' + impCount + ' Imp.</span>';
			}

			let rowsHtml = '';
			items.forEach(function (item) {
				const href = item.estadoCodigo === 'PENDIENTE'
					? ('/admin/equipos/' + item.id + '#tab-gemelo')
					: ('/admin/equipos/' + item.id);

				const hostCell = item.esImpresora
					? '<button type="button" class="taller-host-link" style="background:none;border:none;padding:0;cursor:pointer;" onclick="abrirModalVinculados(' + Number(item.id) + ')">'
					  + '<span>' + iconFor(item) + '</span><span>' + esc(item.nombre) + '</span></button>'
					: '<a class="taller-host-link" href="' + esc(href) + '"><span>' + iconFor(item) + '</span><span>' + esc(item.nombre) + '</span></a>';

				const userCell = item.esImpresora
					? '<span style="color:var(--text-muted); font-size:0.78rem;">Impresora en Red</span>'
					: (item.usuario
						? '<strong>' + esc(item.usuario) + '</strong>' + (item.usuarioRaw && item.usuarioRaw !== item.usuario ? '<span class="taller-sub-mono">@' + esc(item.usuarioRaw) + '</span>' : '')
						: '<span style="color:#d97706; font-weight:700; font-size:0.78rem;">⚠️ Sin asignar</span>');

				const netCell = '<span class="taller-mono">' + esc(item.ip || 'Sin IP') + '</span>'
					+ (item.mac ? '<span class="taller-sub-mono">MAC: ' + esc(item.mac) + '</span>' : '');

				const sysCell = '<span>' + esc(item.so || 'Sin dato') + '</span>'
					+ (item.sn ? '<span class="taller-sub-mono">S/N: ' + esc(item.sn) + '</span>' : '');

				const prCell = item.esImpresora
					? '<button type="button" class="taller-action-btn" onclick="abrirModalVinculados(' + Number(item.id) + ')">🔗 Ver PCs vinculadas</button>'
					: buildPrinterBadgeHtml(item);

				const stateCell = '<div style="display:inline-flex; align-items:center; gap:6px; font-weight:700; font-size:0.77rem;">'
					+ '<span class="plano-led ' + ledClassFor(item) + '"></span>'
					+ '<span>' + esc(item.estadoLabel) + '</span>'
					+ '</div>'
					+ (item.ultimoReporte ? '<span class="taller-sub-mono">🕒 ' + esc(item.ultimoReporte) + '</span>' : '');

				let actionCell = '';
				if (item.esImpresora) {
					actionCell = '<button type="button" class="taller-action-btn" onclick="abrirModalVinculados(' + Number(item.id) + ')">🔗 Vinculados</button>';
				} else if (item.estadoCodigo === 'PENDIENTE') {
					actionCell = '<a class="taller-action-btn is-gemelo" href="/admin/equipos/' + Number(item.id) + '#tab-gemelo">🤖 Registrar Gemelo</a>';
				} else {
					actionCell = '<div style="display:flex; gap:5px; flex-wrap:wrap;">'
						+ '<a class="taller-action-btn" href="/admin/equipos/' + Number(item.id) + '">🖥️ Ficha</a>'
						+ '<a class="taller-action-btn" href="/admin/equipos/' + Number(item.id) + '/acta" target="_blank">🖨️ Acta</a>'
						+ '</div>';
				}

				rowsHtml += ''
					+ '<tr>'
					+ '  <td>' + hostCell + '</td>'
					+ '  <td>' + userCell + '</td>'
					+ '  <td>' + netCell + '</td>'
					+ '  <td>' + sysCell + '</td>'
					+ '  <td>' + prCell + '</td>'
					+ '  <td>' + stateCell + '</td>'
					+ '  <td>' + actionCell + '</td>'
					+ '</tr>';
			});

			box.innerHTML = ''
				+ '<div class="taller-room-bar">'
				+ '  <h3 class="taller-room-title">🏢 ' + esc(roomTitle) + '</h3>'
				+ '  <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">' + metaHtml + '</div>'
				+ '</div>'
				+ '<div style="overflow-x:auto;">'
				+ '  <table class="taller-matrix-table">'
				+ '    <thead>'
				+ '      <tr>'
				+ '        <th>Equipo / Host</th>'
				+ '        <th>Usuario Asignado</th>'
				+ '        <th>Red (IP / MAC)</th>'
				+ '        <th>Sistema / Serie</th>'
				+ '        <th>Impresora</th>'
				+ '        <th>Estado Gemelo</th>'
				+ '        <th>Acción Taller</th>'
				+ '      </tr>'
				+ '    </thead>'
				+ '    <tbody>' + rowsHtml + '</tbody>'
				+ '  </table>'
				+ '</div>';

			return box;
		}

		function renderByFuero(filtered) {
			const sortedFueros = buildFuerosSummaryMap(filtered);
			const frag = document.createDocumentFragment();

			sortedFueros.forEach(function (fObj) {
				const section = document.createElement('section');
				section.className = 'plano-floor-section';

				const roomCount = fObj.rooms.size;
				const headerEl = document.createElement('div');
				headerEl.className = 'plano-floor-header';
				headerEl.innerHTML = ''
					+ '  <div class="plano-floor-title">'
					+ '    🏛️ ' + esc(fObj.fueroPadre)
					+ '  </div>'
					+ '  <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">'
					+ (fObj.pcs > 0 ? '<span class="plano-floor-badge">🖥️ ' + fObj.pcs + ' PC' + (fObj.pcs === 1 ? '' : 's') + '</span>' : '')
					+ (fObj.impresoras > 0 ? '<span class="plano-floor-badge">🖨️ ' + fObj.impresoras + ' Imp.</span>' : '')
					+ (fObj.pendientes > 0 ? '<span class="plano-floor-badge" style="color:#d97706 !important; border-color:#f59e0b !important;">🟡 ' + fObj.pendientes + ' Pend. Gemelo</span>' : '')
					+ '    <span class="plano-floor-badge">'
					+        roomCount + (roomCount === 1 ? ' dependencia' : ' dependencias')
					+ '      · ' + fObj.totalEquipos + (fObj.totalEquipos === 1 ? ' activo' : ' activos')
					+ '    </span>'
					+ '  </div>';

				section.appendChild(headerEl);

				const sortedRooms = Array.from(fObj.rooms.values()).sort(function (a, b) {
					return a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base', numeric: true });
				});

				if (subMode === 'matriz') {
					const matrixWrap = document.createElement('div');
					sortedRooms.forEach(function (room) {
						matrixWrap.appendChild(buildRoomMatrixElement(
							room.titulo,
							room.ubicacionFisica,
							room.responsable,
							room.items,
							room.pcs,
							room.impresoras
						));
					});
					section.appendChild(matrixWrap);
				} else {
					const roomsGrid = document.createElement('div');
					roomsGrid.className = 'plano-rooms-grid';
					sortedRooms.forEach(function (room) {
						const roomEl = document.createElement('article');
						roomEl.className = 'plano-room';
						roomEl.style.setProperty('--room-accent', INSTITUTIONAL_ACCENT);

						let countsHtml = '';
						if (room.pcs > 0) countsHtml += '<span title="Computadoras">🖥️ ' + room.pcs + '</span>';
						if (room.impresoras > 0) countsHtml += '<span title="Impresoras de Red">🖨️ ' + room.impresoras + '</span>';

						roomEl.innerHTML = ''
							+ '<div class="plano-room-header">'
							+ '  <div>'
							+ '    <h3 class="plano-room-name">' + esc(room.titulo) + '</h3>'
							+ '    <div class="plano-room-meta">'
							+ (room.ubicacionFisica
								? '<span class="plano-room-fuero">📍 ' + esc(room.ubicacionFisica) + '</span>'
								: '<span class="plano-room-resp">📍 Ubicación física pendiente</span>')
							+ (room.responsable ? '<span class="plano-room-resp">👤 ' + esc(room.responsable) + '</span>' : '')
							+ '    </div>'
							+ '  </div>'
							+ '  <div class="plano-room-counts">' + countsHtml + '</div>'
							+ '</div>';

						const desksGrid = document.createElement('div');
						desksGrid.className = 'plano-desks-grid';
						room.items.forEach(function (item) {
							desksGrid.appendChild(buildDeskTile(item));
						});

						roomEl.appendChild(desksGrid);
						roomsGrid.appendChild(roomEl);
					});
					section.appendChild(roomsGrid);
				}

				frag.appendChild(section);
			});

			return frag;
		}

		function renderByPiso(filtered) {
			const floorsMap = new Map();
			filtered.forEach(function (item) {
				const fk = floorKey(item);
				if (!floorsMap.has(fk)) {
					floorsMap.set(fk, {
						key: fk,
						edificio: item.edificio || 'Distribución General',
						piso: item.piso || 'Oficinas por Fuero',
						rooms: new Map(),
						totalEquipos: 0
					});
				}
				const floorObj = floorsMap.get(fk);
				floorObj.totalEquipos++;

				const roomKey = (item.oficina || item.fuero || 'Sin ubicación') + '||' + fueroPadreOf(item);
				if (!floorObj.rooms.has(roomKey)) {
					floorObj.rooms.set(roomKey, {
						oficina: item.oficina || item.fuero || 'Sin ubicación',
						fuero: fueroPadreOf(item),
						responsable: item.responsableOficina || '',
						items: [],
						pcs: 0,
						impresoras: 0
					});
				}
				const roomObj = floorObj.rooms.get(roomKey);
				roomObj.items.push(item);
				if (item.esImpresora) roomObj.impresoras++; else roomObj.pcs++;
			});

			const sortedFloors = Array.from(floorsMap.values()).sort(function (a, b) {
				if (a.edificio === 'Distribución General' && b.edificio !== 'Distribución General') return 1;
				if (b.edificio === 'Distribución General' && a.edificio !== 'Distribución General') return -1;
				const ed = a.edificio.localeCompare(b.edificio, 'es', { sensitivity: 'base' });
				if (ed !== 0) return ed;
				return a.piso.localeCompare(b.piso, 'es', { sensitivity: 'base', numeric: true });
			});

			const frag = document.createDocumentFragment();

			sortedFloors.forEach(function (floor) {
				const section = document.createElement('section');
				section.className = 'plano-floor-section';

				const roomCount = floor.rooms.size;
				const headerEl = document.createElement('div');
				headerEl.className = 'plano-floor-header';
				headerEl.innerHTML = ''
					+ '  <div class="plano-floor-title">' + esc(floorLabel(floor.edificio, floor.piso)) + '</div>'
					+ '  <div class="plano-floor-badge">' + roomCount + (roomCount === 1 ? ' oficina' : ' oficinas') + ' · ' + floor.totalEquipos + (floor.totalEquipos === 1 ? ' equipo' : ' equipos') + '</div>';

				section.appendChild(headerEl);

				const sortedRooms = Array.from(floor.rooms.values()).sort(function (a, b) {
					const fc = a.fuero.localeCompare(b.fuero, 'es', { sensitivity: 'base', numeric: true });
					if (fc !== 0) return fc;
					return a.oficina.localeCompare(b.oficina, 'es', { sensitivity: 'base', numeric: true });
				});

				if (subMode === 'matriz') {
					const matrixWrap = document.createElement('div');
					sortedRooms.forEach(function (room) {
						matrixWrap.appendChild(buildRoomMatrixElement(
							room.oficina + ' (' + room.fuero + ')',
							'',
							room.responsable,
							room.items,
							room.pcs,
							room.impresoras
						));
					});
					section.appendChild(matrixWrap);
				} else {
					const roomsGrid = document.createElement('div');
					roomsGrid.className = 'plano-rooms-grid';
					sortedRooms.forEach(function (room) {
						const roomEl = document.createElement('article');
						roomEl.className = 'plano-room';
						roomEl.style.setProperty('--room-accent', INSTITUTIONAL_ACCENT);

						let countsHtml = '';
						if (room.pcs > 0) countsHtml += '<span title="Computadoras">🖥️ ' + room.pcs + '</span>';
						if (room.impresoras > 0) countsHtml += '<span title="Impresoras de Red">🖨️ ' + room.impresoras + '</span>';

						roomEl.innerHTML = ''
							+ '<div class="plano-room-header">'
							+ '  <div>'
							+ '    <h3 class="plano-room-name">' + esc(room.oficina) + '</h3>'
							+ '    <div class="plano-room-meta">'
							+ '      <span class="plano-room-fuero">🏛️ ' + esc(room.fuero) + '</span>'
							+ (room.responsable ? '<span class="plano-room-resp">👤 ' + esc(room.responsable) + '</span>' : '')
							+ '    </div>'
							+ '  </div>'
							+ '  <div class="plano-room-counts">' + countsHtml + '</div>'
							+ '</div>';

						const desksGrid = document.createElement('div');
						desksGrid.className = 'plano-desks-grid';
						room.items.forEach(function (item) {
							desksGrid.appendChild(buildDeskTile(item));
						});

						roomEl.appendChild(desksGrid);
						roomsGrid.appendChild(roomEl);
					});
					section.appendChild(roomsGrid);
				}

				frag.appendChild(section);
			});

			return frag;
		}

		function renderDetailCanvas() {
			if (!canvasEl) return;
			deskElementsById = new Map();
			printerElementsByIp = new Map();
			const filtered = getFilteredItems(false);
			currentFilteredItems = filtered;

			if (filtered.length === 0) {
				canvasEl.innerHTML = '<div class="plano-empty">🏛️ No hay equipos que coincidan con el filtro aplicado.</div>';
				if (svgEl) svgEl.innerHTML = '';
				return;
			}

			const frag = groupBy === 'piso' ? renderByPiso(filtered) : renderByFuero(filtered);
			canvasEl.innerHTML = '';
			canvasEl.appendChild(frag);
			requestAnimationFrame(drawAmbientConnections);
		}

		function renderWorkspace() {
			const hasSearch = quickFilter.trim().length > 0;
			const shouldShowMosaico = showMosaico && !hasSearch && groupBy === 'fuero';

			if (btnVolverMosaico) {
				btnVolverMosaico.classList.toggle('is-active', shouldShowMosaico);
			}

			if (shouldShowMosaico) {
				if (mosaicoContainer) mosaicoContainer.style.display = 'flex';
				if (explorerContainer) explorerContainer.style.display = 'none';
				if (svgEl) svgEl.innerHTML = '';
				renderMosaicoFueros();
			} else {
				if (mosaicoContainer) mosaicoContainer.style.display = 'none';
				if (explorerContainer) explorerContainer.style.display = 'grid';
				renderSidebarFueros();
				renderDetailCanvas();
			}
		}

		// Sincronización manual de estructura AD
		if (btnSyncAd) {
			btnSyncAd.addEventListener('click', async function () {
				const originalText = btnSyncAd.textContent;
				btnSyncAd.disabled = true;
				btnSyncAd.textContent = '⏳ Sincronizando AD...';

				const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
				const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
				const headers = { 'Accept': 'application/json' };
				if (csrfToken) headers[csrfHeader] = csrfToken;

				try {
					const resp = await fetch('/admin/equipos/sincronizar-estructura-ad', {
						method: 'POST',
						headers: headers
					});
					const json = await resp.json();
					if (syncBanner) {
						syncBanner.style.display = 'block';
						syncBanner.style.background = json.ousEncontradas > 0 ? 'rgba(34, 197, 94, 0.14)' : 'rgba(234, 179, 8, 0.14)';
						syncBanner.style.color = json.ousEncontradas > 0 ? '#15803d' : '#b45309';
						syncBanner.style.border = '1px solid ' + (json.ousEncontradas > 0 ? 'rgba(34, 197, 94, 0.35)' : 'rgba(234, 179, 8, 0.35)');
						syncBanner.textContent = (json.ousEncontradas > 0 ? '✅ ' : 'ℹ️ ') + (json.mensaje || 'Sincronización finalizada.');
					}
					if (json.ousEncontradas > 0) {
						setTimeout(function () { window.location.reload(); }, 1100);
					}
				} catch (e) {
					if (syncBanner) {
						syncBanner.style.display = 'block';
						syncBanner.style.background = 'rgba(239, 68, 68, 0.14)';
						syncBanner.style.color = '#b91c1c';
						syncBanner.style.border = '1px solid rgba(239, 68, 68, 0.35)';
						syncBanner.textContent = '❌ No se pudo sincronizar la estructura con Active Directory.';
					}
				} finally {
					btnSyncAd.disabled = false;
					btnSyncAd.textContent = originalText;
				}
			});
		}

		// Eventos de controles
		btnPlano.addEventListener('click', function () { setViewMode('plano'); });
		btnTabla.addEventListener('click', function () { setViewMode('tabla'); });
		if (btnVolverMosaico) btnVolverMosaico.addEventListener('click', openMosaicoView);
		if (btnSubMatriz) btnSubMatriz.addEventListener('click', function () {
			showMosaico = false;
			setSubMode('matriz');
		});
		if (btnSubCroquis) btnSubCroquis.addEventListener('click', function () {
			showMosaico = false;
			setSubMode('croquis');
		});
		if (btnGroupFuero) btnGroupFuero.addEventListener('click', function () { setGroupBy('fuero'); });
		if (btnGroupPiso) btnGroupPiso.addEventListener('click', function () { setGroupBy('piso'); });

		if (btnConn) {
			btnConn.addEventListener('click', function () {
				showAllConnections = !showAllConnections;
				localStorage.setItem(STORAGE_CONN_KEY, String(showAllConnections));
				applyConnButtonState();
				drawAmbientConnections();
			});
		}

		if (quickSearchEl) {
			quickSearchEl.addEventListener('input', function () {
				quickFilter = quickSearchEl.value || '';
				if (quickFilter.trim().length > 0) {
					showMosaico = false;
				}
				renderWorkspace();
			});
		}

		if (btnCompact) {
			btnCompact.addEventListener('click', function () {
				isCompact = !isCompact;
				localStorage.setItem(STORAGE_COMPACT_KEY, String(isCompact));
				applyCompactState();
			});
		}

		window.addEventListener('resize', function () {
			if (containerPlano.style.display !== 'none') {
				drawAmbientConnections();
			}
		});

		// SPEC-001: Control del Modal a Pantalla Completa para Equipos
		const btnToggleFs = document.getElementById('btn-toggle-fullscreen-equipos');
		function setFullscreenEquipos(enable) {
			document.body.classList.toggle('equipos-fullscreen-active', enable);
			if (btnToggleFs) {
				btnToggleFs.classList.toggle('is-active', enable);
				btnToggleFs.innerHTML = enable ? '🗗 Salir de Pantalla Completa' : '⛶ Pantalla Completa';
			}
			setTimeout(function () {
				requestAnimationFrame(drawAmbientConnections);
			}, 60);
		}

		if (btnToggleFs) {
			btnToggleFs.addEventListener('click', function () {
				const isCurrentlyFs = document.body.classList.contains('equipos-fullscreen-active');
				setFullscreenEquipos(!isCurrentlyFs);
			});
		}

		document.addEventListener('keydown', function (ev) {
			if (ev.key === 'Escape') {
				const anyDialogOpen = document.querySelector('dialog[open]');
				if (!anyDialogOpen && document.body.classList.contains('equipos-fullscreen-active')) {
					const sb = document.getElementById('app-sidebar');
					if (sb && sb.classList.contains('is-open')) {
						if (typeof window.toggleSidebar === 'function') window.toggleSidebar();
					} else {
						setFullscreenEquipos(false);
					}
				}
			}
		});

		// Inicialización
		setFullscreenEquipos(true);
		applyCompactState();
		applyConnButtonState();
		setSubMode(subMode);

		const savedMode = localStorage.getItem(STORAGE_VIEW_KEY) || 'plano';
		setViewMode(savedMode);
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initPlanoEquipos);
	} else {
		initPlanoEquipos();
	}
})();
