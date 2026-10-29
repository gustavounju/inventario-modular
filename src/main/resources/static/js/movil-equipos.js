(() => {
    const base = (document.body.dataset.base || '/').replace(/\/?$/, '/');
    const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
    const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.content;
    const $ = (id) => document.getElementById(id);

    const state = {
        equipos: [],
        selectedDetail: null,
        session: null
    };

    const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[char]));

    const api = async (path, options = {}) => {
        const headers = { Accept: 'application/json', ...(options.headers || {}) };
        if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        const response = await fetch(base + path.replace(/^\//, ''), {
            credentials: 'same-origin',
            ...options,
            headers
        });
        if (response.status === 401) {
            window.location.href = base + 'movil/login';
            throw new Error('Sesión vencida');
        }
        if (!response.ok) {
            let message = 'No se pudo completar la operación.';
            try { message = (await response.json()).message || message; } catch (_) { /* ignore */ }
            throw new Error(message);
        }
        return response.status === 204 ? null : response.json();
    };

    const showFeedback = (text, error = false) => {
        const el = $('feedback');
        if (!el) return;
        el.hidden = !text;
        el.textContent = text || '';
        el.classList.toggle('error', Boolean(error));
    };

    const setConnection = (ok, text) => {
        const el = $('connection');
        if (!el) return;
        el.textContent = text;
        el.className = ok ? 'online' : 'offline';
    };

    const refreshIcons = () => {
        if (window.lucide) window.lucide.createIcons();
    };

    const formatRam = (ramMb) => {
        if (!ramMb || ramMb <= 0) return 'S/D';
        if (ramMb >= 1024) return (ramMb / 1024).toFixed(ramMb % 1024 === 0 ? 0 : 1) + ' GB';
        return ramMb + ' MB';
    };

    const renderList = () => {
        const list = $('equipos-list');
        const items = state.equipos || [];
        $('equipos-total-count').textContent = items.length;
        $('equipos-activos-count').textContent = items.filter((e) => e.ultimoReporteEn).length;
        $('equipos-taller-count').textContent = items.filter((e) => !e.ultimoReporteEn || e.estadoCiclo === 'EN_TALLER').length;

        if (!items.length) {
            list.innerHTML = '<p class="empty">No se encontraron equipos con ese criterio.</p>';
            return;
        }

        list.innerHTML = items.map((e) => `
            <article class="task-card" data-equipo-id="${e.id}" style="cursor:pointer;">
                <div class="task-top">
                    <span class="badge">${escapeHtml(e.fuero || 'Sin fuero')}</span>
                    <span class="badge ${e.activo ? 'priority-baja' : 'priority-alta'}">${escapeHtml(e.ip || 'Sin IP')}</span>
                </div>
                <h2>${escapeHtml(e.nombre)}</h2>
                <p class="meta">
                    <span>👤 ${escapeHtml(e.ultimoUsuario || 'Sin usuario reportado')}</span>
                    ${e.ubicacion ? `<span>📍 ${escapeHtml(e.ubicacion)}</span>` : ''}
                    ${e.sistemaOperativo ? `<span>💻 ${escapeHtml(e.sistemaOperativo)}</span>` : ''}
                    ${e.motherboardSerial ? `<span>🔢 SN: ${escapeHtml(e.motherboardSerial)}</span>` : ''}
                </p>
                <div class="actions">
                    <button type="button" class="secondary" data-view-equipo="${e.id}">
                        <i data-lucide="cpu"></i><span>Ver Gemelo Digital</span>
                    </button>
                    <button type="button" class="primary" data-task-equipo="${e.id}">
                        <i data-lucide="plus-circle"></i><span>Crear Tarea</span>
                    </button>
                </div>
            </article>
        `).join('');
        refreshIcons();
    };

    const loadEquipos = async (query = '') => {
        try {
            const q = encodeURIComponent((query || '').trim());
            state.equipos = await api(`api/v1/movil/equipos?q=${q}`);
            setConnection(true, 'En línea');
            renderList();
        } catch (err) {
            setConnection(false, 'Sin conexión');
            showFeedback(err.message, true);
        }
    };

    const openDetail = async (id) => {
        try {
            showFeedback('');
            const detail = await api(`api/v1/movil/equipos/${id}`);
            state.selectedDetail = detail;
            const eq = detail.equipo;
            $('detail-fuero').textContent = eq.fuero || 'Sin fuero';
            $('detail-nombre').textContent = eq.nombre;
            $('detail-meta').innerHTML = `
                <div><strong>IP:</strong> ${escapeHtml(eq.ip || 'S/D')}</div>
                <div><strong>MAC:</strong> ${escapeHtml(eq.mac || 'S/D')}</div>
                <div><strong>Usuario AD:</strong> ${escapeHtml(eq.ultimoUsuario || 'S/D')}</div>
                <div><strong>Ubicación:</strong> ${escapeHtml(eq.ubicacion || 'S/D')}</div>
                <div><strong>Impresora:</strong> ${escapeHtml(eq.impresora || eq.impresoraRedNombre || 'S/D')}</div>
                <div><strong>IP Impresora:</strong> ${escapeHtml(eq.impresoraIp || eq.impresoraRedIp || 'S/D')}</div>
            `;
            $('detail-hardware').innerHTML = `
                <div><strong>SO:</strong> ${escapeHtml(eq.sistemaOperativo || 'S/D')}</div>
                <div><strong>CPU:</strong> ${escapeHtml(eq.procesador || 'S/D')}</div>
                <div><strong>RAM:</strong> ${escapeHtml(formatRam(eq.ramMb))} ${eq.ramDetalles ? '(' + escapeHtml(eq.ramDetalles) + ')' : ''}</div>
                <div><strong>Discos:</strong> ${escapeHtml(eq.discosModelos || 'S/D')}</div>
                <div><strong>Motherboard:</strong> ${escapeHtml(eq.motherboardModelo || 'S/D')} ${eq.motherboardSerial ? '(SN: ' + escapeHtml(eq.motherboardSerial) + ')' : ''}</div>
            `;

            const gemeloEl = $('detail-gemelo');
            if (!detail.gemelo || !detail.gemelo.length) {
                gemeloEl.innerHTML = '<p class="muted" style="margin:0;">Sin componentes registrados en el gemelo digital.</p>';
            } else {
                gemeloEl.innerHTML = detail.gemelo.map((g) => {
                    const badgeColor = g.resultado === 'COINCIDE' ? '#16a34a'
                        : (g.resultado === 'FALTA' ? '#dc2626' : '#d97706');
                    return `
                        <div style="padding:0.45rem 0.6rem; border-radius:0.4rem; border:1px solid #e2e8f0; background:#f8fafc; font-size:0.82rem;">
                            <div style="display:flex; justify-content:space-between; align-items:center;">
                                <strong>${escapeHtml(g.tipo)}</strong>
                                <span style="color:#fff; background:${badgeColor}; padding:0.1rem 0.45rem; border-radius:999px; font-size:0.72rem; font-weight:600;">${escapeHtml(g.resultado)}</span>
                            </div>
                            <div>Esperado: ${escapeHtml(g.esperado || '-')}</div>
                            <div>Detectado: ${escapeHtml(g.detectado || '-')}</div>
                        </div>
                    `;
                }).join('');
            }

            const tareasEl = $('detail-tareas');
            if (!detail.tareas || !detail.tareas.length) {
                tareasEl.innerHTML = '<p class="muted" style="margin:0;">Sin tareas técnicas asociadas a este equipo.</p>';
            } else {
                tareasEl.innerHTML = detail.tareas.slice(0, 5).map((t) => `
                    <div style="padding:0.4rem 0.6rem; border-radius:0.4rem; border:1px solid #e2e8f0; font-size:0.82rem;">
                        <strong>#${t.id} · ${escapeHtml(t.titulo)}</strong>
                        <div class="muted">${escapeHtml(t.estado)} · Resp: ${escapeHtml(t.responsable || 'Sin asignar')}</div>
                    </div>
                `).join('');
            }

            const dialog = $('equipo-detail-dialog');
            if (dialog.showModal) dialog.showModal();
            else dialog.setAttribute('open', 'open');
            refreshIcons();
        } catch (err) {
            showFeedback(err.message, true);
        }
    };

    const openQuickTaskForEquipo = (equipo) => {
        if (!equipo) return;
        $('quick-task-equipo-id').value = equipo.id;
        $('quick-task-solicitante-username').value = equipo.ultimoUsuario || 'terreno';
        $('quick-task-solicitante-nombre').value = equipo.ultimoUsuario || ('Puesto ' + equipo.nombre);
        $('quick-task-solicitante-fuero').value = equipo.fuero || 'Juzgado';
        $('quick-task-equipo-label').textContent = `Equipo: ${equipo.nombre} (${equipo.ip || 'Sin IP'}) · ${equipo.fuero || 'Sin fuero'}`;
        $('quick-task-titulo').value = '';
        $('quick-task-descripcion').value = '';
        const dialog = $('quick-task-dialog');
        if (dialog.showModal) dialog.showModal();
        else dialog.setAttribute('open', 'open');
        $('quick-task-titulo').focus();
    };

    const applyScannedCode = (rawCode) => {
        const code = String(rawCode || '').trim();
        if (!code) return;
        const input = $('equipo-search');
        if (input) {
            input.value = code;
            loadEquipos(code);
        }
    };

    window.__inventarioStockApplyScan = applyScannedCode;
    window.addEventListener('inventario-stock-scan', (e) => applyScannedCode(e.detail));

    document.addEventListener('DOMContentLoaded', async () => {
        refreshIcons();
        try {
            state.session = await api('api/v1/movil/sesion');
            $('username').textContent = state.session?.usuario?.nombreVisible || state.session?.usuario?.username || 'Técnico';
        } catch (_) {
            $('username').textContent = 'Técnico en terreno';
        }

        await loadEquipos('');

        let debounceTimer = null;
        $('equipo-search')?.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => loadEquipos(e.target.value), 250);
        });

        $('refresh-equipos')?.addEventListener('click', () => loadEquipos($('equipo-search')?.value || ''));

        $('scan-equipo-qr')?.addEventListener('click', () => {
            if (window.TareasLan && typeof window.TareasLan.escanearCodigo === 'function') {
                window.TareasLan.escanearCodigo();
            } else {
                $('equipo-search')?.focus();
                showFeedback('Escáner de cámara nativo disponible desde la APK Tareas LAN. Puede escribir el código o serial manualmente.');
            }
        });

        $('equipos-list')?.addEventListener('click', (e) => {
            const taskBtn = e.target.closest('[data-task-equipo]');
            if (taskBtn) {
                e.stopPropagation();
                const id = Number(taskBtn.dataset.taskEquipo);
                const eq = state.equipos.find((item) => item.id === id);
                openQuickTaskForEquipo(eq);
                return;
            }
            const viewBtn = e.target.closest('[data-view-equipo]');
            const card = e.target.closest('[data-equipo-id]');
            const id = Number(viewBtn?.dataset.viewEquipo || card?.dataset.equipoId);
            if (id) openDetail(id);
        });

        const closeDetail = () => {
            const d = $('equipo-detail-dialog');
            if (d?.close) d.close();
            else d?.removeAttribute('open');
        };
        $('close-detail')?.addEventListener('click', closeDetail);
        $('close-detail-bottom')?.addEventListener('click', closeDetail);

        $('open-quick-task')?.addEventListener('click', () => {
            closeDetail();
            if (state.selectedDetail?.equipo) {
                openQuickTaskForEquipo(state.selectedDetail.equipo);
            }
        });

        const closeQuickTask = () => {
            const d = $('quick-task-dialog');
            if (d?.close) d.close();
            else d?.removeAttribute('open');
        };
        $('cancel-quick-task')?.addEventListener('click', closeQuickTask);

        $('quick-task-form')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const payload = {
                    titulo: $('quick-task-titulo').value.trim(),
                    descripcion: $('quick-task-descripcion').value.trim(),
                    prioridad: $('quick-task-prioridad').value,
                    tipo: $('quick-task-tipo').value,
                    equipoId: Number($('quick-task-equipo-id').value) || null,
                    solicitanteUsername: $('quick-task-solicitante-username').value || 'terreno',
                    solicitanteNombre: $('quick-task-solicitante-nombre').value || 'Relevamiento en terreno',
                    solicitanteFuero: $('quick-task-solicitante-fuero').value || 'Juzgado'
                };
                await api('api/v1/tareas-tecnicas', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                closeQuickTask();
                showFeedback('✅ Tarea técnica creada y vinculada al equipo.');
            } catch (err) {
                showFeedback(err.message, true);
            }
        });
    });
})();

