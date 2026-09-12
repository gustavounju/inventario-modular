(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const base = document.body.dataset.base;
    const csrf = document.querySelector('meta[name="csrf-token"]').content;
    const csrfHeader = document.querySelector('meta[name="csrf-header"]').content;
    const native = navigator.userAgent.includes('InventarioLAN/');
    const installedVersion = window.TareasLan?.versionInstalada?.() || (navigator.userAgent.match(/InventarioLANVersion\/([^ ]+)/)?.[1] || '');
    let session, busy = false;
    let scanTarget = 'single';
    let quickCodes = [];
    const icons = () => window.lucide?.createIcons();
    const stateLabel = value => value === 'DISPONIBLE' ? 'Disponible' : value === 'RESERVADO' ? 'Reservado' : value === 'ASIGNADO' ? 'Asignado' : value;
    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text != null) node.textContent = text;
        if (className) node.className = className;
        return node;
    }
    function message(text, error = false) {
        $('message').textContent = text;
        $('message').classList.toggle('error', error);
        $('message').hidden = !text;
    }
    function renderQuickCodes() {
        $('quick-count').textContent = quickCodes.length + (quickCodes.length === 1 ? ' codigo' : ' codigos');
        $('quick-list').replaceChildren();
        for (const code of quickCodes) {
            const pill = element('button', code, 'quick-code-pill');
            pill.type = 'button';
            pill.title = 'Quitar ' + code;
            pill.onclick = () => {
                quickCodes = quickCodes.filter(item => item !== code);
                renderQuickCodes();
            };
            $('quick-list').append(pill);
        }
        $('send-quick-codes').disabled = !session?.puedeEditarStock || quickCodes.length === 0 || busy;
        $('clear-quick-codes').disabled = quickCodes.length === 0 || busy;
    }
    function addQuickCode(codeValue) {
        const code = String(codeValue || '').trim();
        if (!code) return;
        if (!quickCodes.includes(code)) quickCodes.push(code);
        $('quick-code').value = '';
        renderQuickCodes();
        message('Codigo agregado al lote: ' + code);
    }
    async function request(path, method = 'GET', data) {
        const response = await fetch(base + path, {
            method, credentials: 'same-origin', cache: 'no-store',
            headers: { Accept: 'application/json', ...(method !== 'GET' ? { [csrfHeader]: csrf, 'Content-Type': 'application/json' } : {}) },
            ...(data !== undefined ? { body: JSON.stringify(data) } : {}), signal: AbortSignal.timeout(15000)
        });
        if (response.status === 401 || response.redirected) {
            location.assign(base + 'movil/login');
            throw new Error('La sesion vencio. Ingrese nuevamente.');
        }
        if (!response.ok) {
            const errors = { 403: 'No tiene permiso para operar stock.', 400: 'Revise tipo, codigo y descripcion.', 409: 'El stock no se pudo guardar con esos datos.' };
            throw new Error(errors[response.status] || 'No se pudo completar la operacion.');
        }
        return response.status === 204 ? null : response.json();
    }
    function fillDescription() {
        const form = $('stock-entry-form');
        const serial = form.elements.serial.value.trim();
        const model = form.elements.modelo.value.trim();
        const capacity = form.elements.capacidad.value.trim();
        const parts = [form.elements.tipo.value, model, capacity].filter(Boolean);
        form.elements.descripcion.value = (parts.length ? parts.join(' ') : form.elements.tipo.value) + (serial ? ' - ' + serial : '');
    }
    function render(list) {
        $('available-count').textContent = list.filter(item => item.estado === 'DISPONIBLE').length;
        $('reserved-count').textContent = list.filter(item => item.estado === 'RESERVADO').length;
        $('assigned-count').textContent = list.filter(item => item.estado === 'ASIGNADO').length;
        $('stock-list').replaceChildren();
        for (const item of list.slice(0, 40)) {
            const row = element('article', null, 'task-row');
            const title = element('strong', '#' + item.id + ' ' + item.tipo + ' - ' + item.descripcion, 'task-open');
            const badge = element('span', stateLabel(item.estado), 'badge' + (item.estado === 'DISPONIBLE' ? ' done' : ''));
            row.append(title, badge);
            const meta = element('div', null, 'task-meta');
            meta.append(
                    element('span', item.serial || 'Sin codigo'),
                    element('span', item.marca || '-'),
                    element('span', item.modelo || '-'),
                    element('span', item.ingresadoPor ? 'Ingresado por ' + item.ingresadoPor : 'Sin usuario'));
            row.append(meta);
            if (item.observaciones) row.append(element('p', item.observaciones, 'task-description'));
            $('stock-list').append(row);
        }
        $('result-count').textContent = list.length + ' piezas';
        $('empty').hidden = list.length !== 0;
        $('stock-list').setAttribute('aria-busy', 'false');
    }
    async function refresh() {
        const list = await request('api/v1/stock/componentes');
        render(list);
        $('connection').textContent = 'Conectado';
        $('connection').classList.remove('offline');
    }
    async function start() {
        try {
            session = await request('api/v1/movil/stock/sesion');
            $('username').textContent = session.usuario.nombreVisible + ' | ' + session.usuario.username;
            $('scan-code').hidden = !native;
            $('scan-quick-code').hidden = !native;
            $('save-stock').disabled = !session.puedeEditarStock;
            renderQuickCodes();
            await checkApkUpdate();
            await refresh();
        } catch (error) {
            $('connection').textContent = 'No conectado';
            $('connection').classList.add('offline');
            message(error.message || 'No se pudo conectar.', true);
        }
    }
    $('stock-entry-form').onsubmit = event => {
        event.preventDefault();
        if (busy) return;
        busy = true;
        $('save-stock').disabled = true;
        message('');
        const form = event.target;
        fillDescription();
        const data = Object.fromEntries(new FormData(form));
        data.descripcion = data.descripcion || data.tipo + ' - ' + data.serial;
        data.estado = 'DISPONIBLE';
        data.activo = true;
        request('api/v1/stock/componentes', 'POST', data)
                .then(saved => {
                    form.reset();
                    message('Stock guardado: #' + saved.id + ' ' + saved.tipo + ' ' + (saved.serial || ''));
                    return refresh();
                })
                .catch(error => message(error.message || 'No se pudo guardar.', true))
                .finally(() => { busy = false; $('save-stock').disabled = !session?.puedeEditarStock; });
    };
    $('scan-code').onclick = () => {
        scanTarget = 'single';
        if (window.TareasLan?.escanearCodigo) window.TareasLan.escanearCodigo();
        else message('Escaneo disponible desde la APK 0.1.9-lan o superior.', true);
    };
    $('scan-quick-code').onclick = () => {
        scanTarget = 'quick';
        if (window.TareasLan?.escanearCodigo) window.TareasLan.escanearCodigo();
        else message('Escaneo disponible desde la APK 0.1.9-lan o superior.', true);
    };
    $('add-quick-code').onclick = () => addQuickCode($('quick-code').value);
    $('quick-code').addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            addQuickCode(event.target.value);
        }
    });
    $('clear-quick-codes').onclick = () => {
        quickCodes = [];
        renderQuickCodes();
        message('');
    };
    $('send-quick-codes').onclick = () => {
        if (busy || quickCodes.length === 0) return;
        busy = true;
        renderQuickCodes();
        request('api/v1/stock/componentes/lote-rapido', 'POST', { codigos: quickCodes })
                .then(saved => {
                    const total = saved.length;
                    quickCodes = [];
                    renderQuickCodes();
                    message(total + (total === 1 ? ' codigo enviado a stock pendiente.' : ' codigos enviados a stock pendientes.'));
                    return refresh();
                })
                .catch(error => message(error.message || 'No se pudo enviar el lote.', true))
                .finally(() => { busy = false; renderQuickCodes(); });
    };
    $('apk-update').onclick = event => {
        if (!window.TareasLan?.actualizarApk) return;
        event.preventDefault();
        window.TareasLan.actualizarApk();
    };
    async function checkApkUpdate() {
        try {
            const apk = await request('api/v1/movil/apk/info');
            if (!native || !apk.disponible || !apk.version || apk.version.toLowerCase() === installedVersion.toLowerCase()) return;
            $('apk-update-text').textContent = 'Instalada ' + (installedVersion || 'sin dato') + ' | publicada ' + apk.version;
            $('apk-update-panel').hidden = false;
        } catch { /* La pantalla de stock sigue operativa aunque falle el chequeo de APK. */ }
    }
    $('stock-entry-form').elements.serial.addEventListener('change', fillDescription);
    $('stock-entry-form').elements.tipo.addEventListener('change', fillDescription);
    $('stock-entry-form').elements.modelo.addEventListener('change', fillDescription);
    $('stock-entry-form').elements.capacidad.addEventListener('change', fillDescription);
    function applyScan(codeValue) {
        const code = String(codeValue || '').trim();
        if (!code) return;
        if (scanTarget === 'quick') {
            addQuickCode(code);
            return;
        }
        const form = $('stock-entry-form');
        form.elements.serial.value = code;
        fillDescription();
        form.elements.tipo.focus();
        message('Codigo escaneado: ' + code);
    }
    window.__inventarioStockApplyScan = applyScan;
    window.addEventListener('inventario-stock-scan', event => {
        const code = String(event.detail || '').trim();
        applyScan(code);
    });
    if (native && $('download-apk')) $('download-apk').hidden = true;
    icons(); start();
})();
