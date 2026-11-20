(function () {
	'use strict';

	const STORAGE_VIEW_KEY = 'inventario_equipos_vista_modo';
	const STORAGE_GROUP_KEY = 'inventario_equipos_plano_agrupacion';
	const STORAGE_COMPACT_KEY = 'inventario_equipos_plano_compacto';
	const STORAGE_CONN_KEY = 'inventario_equipos_plano_conexiones';

	const FUERO_PALETTE = [
		'#0284c7', // Azul judicial
		'#7c3aed', // Violeta
		'#059669', // Esmeralda
		'#d97706', // Ámbar
		'#e11d48', // Carmesí
		'#0891b2', // Cian
		'#4f46e5', // Índigo
		'#65a30d', // Lima oscuro
		'#c026d3', // Fucsia
		'#ea580c'  // Naranja
	];

	function colorForFuero(fuero) {
		const str = (fuero || 'Sin fuero').trim().toLowerCase();
		let hash = 0;
		for (let i = 0; i < str.length; i++) {
			hash = ((hash << 5) - hash) + str.charCodeAt(i);
			hash |= 0;
		}
		return FUERO_PALETTE[Math.abs(hash) % FUERO_PALETTE.length];
	}

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

		const btnGroupFuero = document.getElementById('btn-group-fuero');
		const btnGroupPiso = document.getElementById('btn-group-piso');
		const btnSyncAd = document.getElementById('btn-sync-ad-estructura');
		const btnConn = document.getElementById('btn-plano-connections');
		const syncBanner = document.getElementById('plano-sync-banner');
		const floorTabsEl = document.getElementById('plano-floor-tabs');
		const fueroBarEl = document.getElementById('plano-fuero-bar');
		const stageEl = document.getElementById('plano-stage');
		const svgEl = document.getElementById('plano-connections-svg');
		const canvasEl = document.getElementById('plano-canvas');
		const quickSearchEl = document.getElementById('plano-quick-search');
		const btnCompact = document.getElementById('btn-plano-compact');

		let groupBy = localStorage.getItem(STORAGE_GROUP_KEY) || 'fuero';
		let activeFloor = 'ALL';
		let activeFuero = 'ALL';
		let quickFilter = '';
		let isCompact = localStorage.getItem(STORAGE_COMPACT_KEY) === 'true';
		let showAllConnections = localStorage.getItem(STORAGE_CONN_KEY) === 'true';

		// Registro de elementos visibles en el DOM para trazar líneas SVG
		let deskElementsById = new Map();
		let printerElementsByIp = new Map();
		let currentFilteredItems = [];

		// Tooltip flotante único
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

		function setGroupBy(mode) {
			groupBy = mode === 'piso' ? 'piso' : 'fuero';
			localStorage.setItem(STORAGE_GROUP_KEY, groupBy);
			if (btnGroupFuero) btnGroupFuero.classList.toggle('is-active', groupBy === 'fuero');
			if (btnGroupPiso) btnGroupPiso.classList.toggle('is-active', groupBy === 'piso');
			renderTopSelectors();
			renderCanvas();
		}

		function applyCompactState() {
			containerPlano.classList.toggle('is-compact', isCompact);
			if (btnCompact) {
				btnCompact.textContent = isCompact ? '🔍 Vista Detallada' : '📐 Modo Compacto';
			}
			requestAnimationFrame(drawAmbientConnections);
		}

		function applyConnButtonState() {
			if (!btnConn) return;
			btnConn.classList.toggle('is-active', showAllConnections);
			btnConn.textContent = showAllConnections ? '🔗 Conexiones: ON' : '🔗 Conexiones';
		}

		function getFilteredItems() {
			const q = quickFilter.trim().toLowerCase();
			return data.filter(function (item) {
				if (activeFloor !== 'ALL' && floorKey(item) !== activeFloor) {
					return false;
				}
				if (activeFuero !== 'ALL' && fueroPadreOf(item) !== activeFuero) {
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
			if (!showAllConnections) return;

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
			if (!svgEl) return;
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

		function renderTopSelectors() {
			if (floorTabsEl) {
				const counts = new Map();
				data.forEach(function (item) {
					const k = floorKey(item);
					if (!counts.has(k)) {
						counts.set(k, {
							key: k,
							edificio: item.edificio || 'Distribución General',
							piso: item.piso || 'Oficinas por Fuero',
							count: 0
						});
					}
					counts.get(k).count++;
				});

				const floors = Array.from(counts.values()).sort(function (a, b) {
					if (a.edificio === 'Distribución General' && b.edificio !== 'Distribución General') return 1;
					if (b.edificio === 'Distribución General' && a.edificio !== 'Distribución General') return -1;
					const ed = a.edificio.localeCompare(b.edificio, 'es', { sensitivity: 'base' });
					if (ed !== 0) return ed;
					return a.piso.localeCompare(b.piso, 'es', { sensitivity: 'base', numeric: true });
				});

				if (floors.length <= 1) {
					floorTabsEl.innerHTML = '';
				} else {
					let html = '<button type="button" class="plano-floor-tab' + (activeFloor === 'ALL' ? ' is-active' : '') + '" data-floor="ALL">'
						+ '🗺️ Todos los Pisos <span class="plano-floor-count">' + data.length + '</span></button>';

					floors.forEach(function (f) {
						html += '<button type="button" class="plano-floor-tab' + (activeFloor === f.key ? ' is-active' : '') + '" data-floor="' + esc(f.key) + '">'
							+ esc(floorShortLabel(f.edificio, f.piso))
							+ ' <span class="plano-floor-count">' + f.count + '</span></button>';
					});

					floorTabsEl.innerHTML = html;
					floorTabsEl.querySelectorAll('.plano-floor-tab').forEach(function (btn) {
						btn.addEventListener('click', function () {
							activeFloor = btn.getAttribute('data-floor') || 'ALL';
							renderTopSelectors();
							renderCanvas();
						});
					});
				}
			}

			if (fueroBarEl) {
				const fueroCounts = new Map();
				data.forEach(function (item) {
					const f = fueroPadreOf(item);
					fueroCounts.set(f, (fueroCounts.get(f) || 0) + 1);
				});

				const fueros = Array.from(fueroCounts.entries()).sort(function (a, b) {
					return a[0].localeCompare(b[0], 'es', { sensitivity: 'base', numeric: true });
				});

				if (fueros.length <= 1) {
					fueroBarEl.style.display = 'none';
					return;
				}
				fueroBarEl.style.display = 'flex';

				let html = '<button type="button" class="plano-fuero-chip' + (activeFuero === 'ALL' ? ' is-active' : '') + '" data-fuero="ALL">🏛️ Todos los Fueros (' + data.length + ')</button>';
				fueros.forEach(function (entry) {
					const name = entry[0];
					const count = entry[1];
					const color = colorForFuero(name);
					html += '<button type="button" class="plano-fuero-chip' + (activeFuero === name ? ' is-active' : '') + '" data-fuero="' + esc(name) + '">'
						+ '<span class="plano-fuero-dot" style="background:' + color + ';"></span>'
						+ esc(name) + ' <span style="opacity:0.72;">(' + count + ')</span></button>';
				});

				fueroBarEl.innerHTML = html;
				fueroBarEl.querySelectorAll('.plano-fuero-chip').forEach(function (btn) {
					btn.addEventListener('click', function () {
						activeFuero = btn.getAttribute('data-fuero') || 'ALL';
						renderTopSelectors();
						renderCanvas();
					});
				});
			}
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
			return '';
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

			desk.innerHTML = ''
				+ '<div class="plano-desk-top">'
				+ '  <div class="plano-desk-name-wrap">'
				+ '    <span class="plano-desk-icon">' + iconFor(item) + '</span>'
				+ '    <span class="plano-desk-name">' + esc(item.nombre) + '</span>'
				+ '  </div>'
				+ '  <span class="plano-led ' + ledClassFor(item) + '" title="' + esc(item.estadoLabel) + '"></span>'
				+ '</div>'
				+ '<div class="' + userClass + '">' + esc(userText) + '</div>'
				+ buildPrinterBadgeHtml(item)
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

		function renderByFuero(filtered) {
			const fuerosMap = new Map();
			filtered.forEach(function (item) {
				const fp = fueroPadreOf(item);
				if (!fuerosMap.has(fp)) {
					fuerosMap.set(fp, {
						fueroPadre: fp,
						rooms: new Map(),
						totalEquipos: 0,
						pcs: 0,
						impresoras: 0
					});
				}
				const fObj = fuerosMap.get(fp);
				fObj.totalEquipos++;
				if (item.esImpresora) fObj.impresoras++; else fObj.pcs++;

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

			const sortedFueros = Array.from(fuerosMap.values()).sort(function (a, b) {
				if (a.fueroPadre.toLowerCase() === 'sin fuero') return 1;
				if (b.fueroPadre.toLowerCase() === 'sin fuero') return -1;
				return a.fueroPadre.localeCompare(b.fueroPadre, 'es', { sensitivity: 'base', numeric: true });
			});

			const frag = document.createDocumentFragment();

			sortedFueros.forEach(function (fObj) {
				const accent = colorForFuero(fObj.fueroPadre);
				const section = document.createElement('section');
				section.className = 'plano-floor-section';
				section.style.borderColor = accent + '55';

				const roomCount = fObj.rooms.size;
				section.innerHTML = ''
					+ '<div class="plano-floor-header" style="border-bottom-color:' + accent + '44;">'
					+ '  <div class="plano-floor-title" style="color:' + accent + ';">'
					+ '    <span style="width:12px;height:12px;border-radius:3px;background:' + accent + ';display:inline-block;"></span>'
					+ '    🏛️ ' + esc(fObj.fueroPadre)
					+ '  </div>'
					+ '  <div class="plano-floor-badge" style="border-color:' + accent + '44;color:' + accent + ';">'
					+      roomCount + (roomCount === 1 ? ' dependencia' : ' dependencias')
					+ '    · ' + fObj.totalEquipos + (fObj.totalEquipos === 1 ? ' equipo' : ' equipos')
					+ '  </div>'
					+ '</div>';

				const roomsGrid = document.createElement('div');
				roomsGrid.className = 'plano-rooms-grid';

				const sortedRooms = Array.from(fObj.rooms.values()).sort(function (a, b) {
					return a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base', numeric: true });
				});

				sortedRooms.forEach(function (room) {
					const roomEl = document.createElement('article');
					roomEl.className = 'plano-room';
					roomEl.style.setProperty('--room-accent', accent);

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
				section.innerHTML = ''
					+ '<div class="plano-floor-header">'
					+ '  <div class="plano-floor-title">' + esc(floorLabel(floor.edificio, floor.piso)) + '</div>'
					+ '  <div class="plano-floor-badge">' + roomCount + (roomCount === 1 ? ' oficina' : ' oficinas') + ' · ' + floor.totalEquipos + (floor.totalEquipos === 1 ? ' equipo' : ' equipos') + '</div>'
					+ '</div>';

				const roomsGrid = document.createElement('div');
				roomsGrid.className = 'plano-rooms-grid';

				const sortedRooms = Array.from(floor.rooms.values()).sort(function (a, b) {
					const fc = a.fuero.localeCompare(b.fuero, 'es', { sensitivity: 'base', numeric: true });
					if (fc !== 0) return fc;
					return a.oficina.localeCompare(b.oficina, 'es', { sensitivity: 'base', numeric: true });
				});

				sortedRooms.forEach(function (room) {
					const accent = colorForFuero(room.fuero);
					const roomEl = document.createElement('article');
					roomEl.className = 'plano-room';
					roomEl.style.setProperty('--room-accent', accent);

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
				frag.appendChild(section);
			});

			return frag;
		}

		function renderCanvas() {
			if (!canvasEl) return;
			deskElementsById = new Map();
			printerElementsByIp = new Map();
			const filtered = getFilteredItems();
			currentFilteredItems = filtered;

			if (filtered.length === 0) {
				canvasEl.innerHTML = '<div class="plano-empty">🏛️ No hay equipos que coincidan con el filtro aplicado en el plano.</div>';
				if (svgEl) svgEl.innerHTML = '';
				return;
			}

			const frag = groupBy === 'piso' ? renderByPiso(filtered) : renderByFuero(filtered);
			canvasEl.innerHTML = '';
			canvasEl.appendChild(frag);
			requestAnimationFrame(drawAmbientConnections);
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
				renderCanvas();
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

		// Inicialización
		applyCompactState();
		applyConnButtonState();
		setGroupBy(groupBy);

		const savedMode = localStorage.getItem(STORAGE_VIEW_KEY) || 'plano';
		setViewMode(savedMode);
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', initPlanoEquipos);
	} else {
		initPlanoEquipos();
	}
})();
