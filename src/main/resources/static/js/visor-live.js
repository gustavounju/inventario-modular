(() => {
    'use strict';
    const toggle = document.getElementById('visor-live');
    const status = document.getElementById('visor-live-status');
    let edited = false;

    document.addEventListener('input', event => {
        if (event.target.closest('form') && event.target !== toggle) {
            edited = true;
            status.textContent = 'Pausado: cambios sin guardar';
        }
    });

    async function update() {
        try {
            if (!toggle.checked || edited || document.activeElement?.closest('form')) return;
            const response = await fetch(location.href, { credentials: 'same-origin', cache: 'no-store', signal: AbortSignal.timeout(15000) });
            if (response.redirected || response.status === 401 || response.status === 403) {
                toggle.checked = false;
                status.textContent = 'Vuelva a ingresar';
                return;
            }
            if (!response.ok) throw new Error('Conexion');
            const next = new DOMParser().parseFromString(await response.text(), 'text/html');
            // El usuario pudo empezar a escribir mientras viajaba la respuesta; comprobar otra vez antes de reemplazar.
            if (edited || !toggle.checked || document.activeElement?.closest('form')) return;
            for (const selector of ['.task-metric-grid', '.task-board', '#apk-chips-list']) {
                const current = document.querySelector(selector), fresh = next.querySelector(selector);
                if (current && fresh) current.replaceWith(fresh);
            }
            status.textContent = 'Actualizado ' + new Date().toLocaleTimeString('es-AR');
        } catch {
            status.textContent = 'Sin conexion. Reintentando...';
        } finally { setTimeout(update, 30000); }
    }

    // ─── Polling en tiempo real de presencia APK cada 10s ─────────────────────
    async function updateApkPresencia() {
        try {
            const res = await fetch('/api/v1/movil/tecnicos-conectados', { credentials: 'same-origin', cache: 'no-store' });
            if (!res.ok) return;
            const list = await res.json();
            const container = document.getElementById('apk-chips-list');
            if (!container) return;

            if (!list.length) {
                container.innerHTML = `
                    <div class="apk-chip apk-chip--empty" title="Ningún técnico conectado a la APK actualmente">
                        <span class="apk-dot apk-dot--idle"></span>
                        <span class="apk-chip-text">Sin conexión</span>
                    </div>`;
                return;
            }

            // Preservar si había un técnico resaltado
            const activeHighlighted = document.querySelector('.apk-chip.is-highlighted')?.dataset?.username?.toLowerCase();

            container.innerHTML = list.map(tec => {
                const tiempoTexto = !tec.segundosInactivo || tec.segundosInactivo < 60
                    ? `${tec.segundosInactivo || 0}s`
                    : `${Math.floor(tec.segundosInactivo / 60)}m`;
                const title = `${escapeHtml(tec.nombreVisible || tec.username)} (@${escapeHtml(tec.username)})${tec.fuero ? ' · ' + escapeHtml(tec.fuero) : ''}${tec.esApk ? ' · APK Android' : ' · Móvil Web'}${tec.version ? ' v' + escapeHtml(tec.version) : ''} · activo hace ${tiempoTexto}`;
                const chipClass = tec.esApk ? 'apk-chip--apk' : 'apk-chip--web';
                const isHl = activeHighlighted && activeHighlighted === tec.username?.toLowerCase() ? ' is-highlighted' : '';
                return `<div class="apk-chip ${chipClass}${isHl}" data-username="${escapeHtml(tec.username)}" title="${title}">
                    <span class="apk-dot apk-dot--active"></span>
                    <span class="apk-chip-initials">${escapeHtml(tec.iniciales || 'TK')}</span>
                </div>`;
            }).join('');
        } catch {
            // Silencioso ante pérdidas temporales de red
        } finally {
            setTimeout(updateApkPresencia, 10000);
        }
    }

    // ─── Interacción: Clic en cuadradito resalta las cards del técnico ────────
    document.addEventListener('click', event => {
        const chip = event.target.closest('.apk-chip');
        if (!chip || chip.classList.contains('apk-chip--empty')) return;
        const username = chip.dataset.username;
        if (!username) return;

        const wasHighlighted = chip.classList.contains('is-highlighted');
        document.querySelectorAll('.apk-chip').forEach(c => c.classList.remove('is-highlighted'));
        document.querySelectorAll('.kcard').forEach(card => card.classList.remove('is-dimmed', 'is-highlighted'));

        if (!wasHighlighted) {
            chip.classList.add('is-highlighted');
            document.querySelectorAll('.kcard').forEach(card => {
                const cardResp = card.dataset.responsable || '';
                if (cardResp.toLowerCase() === username.toLowerCase()) {
                    card.classList.add('is-highlighted');
                } else {
                    card.classList.add('is-dimmed');
                }
            });
        }
    });

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/[&<>"']/g, m => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[m]));
    }
    window.escapeHtml = escapeHtml;

    setTimeout(update, 30000);
    setTimeout(updateApkPresencia, 10000);
})();
