(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const base = document.body.dataset.base;
    const api = 'api/v1/tareas-tecnicas';
    const csrf = document.querySelector('meta[name="csrf-token"]').content;
    const csrfHeader = document.querySelector('meta[name="csrf-header"]').content;
    const native = navigator.userAgent.includes('InventarioLAN/');
    let session, tasks = [], selected, editing, filter = 'pending', limit = 40, stockAvailable = [];
    let cursor, cursorKey, busy = false, audio, sound = false, stopped = false;
    let applicantSearchTimer, assigneeSearchTimer;
    const applicantOptions = new Map();
    const assigneeOptions = new Map();
    // Los previews de comentarios se cargan aparte para no retrasar el listado principal de tareas.
    let renderToken = 0;
    const commentPreviewCache = new Map();
    const icons = () => window.lucide?.createIcons();
    const isOpen = task => ['PENDIENTE', 'EN_PROCESO'].includes(task.estado);
    const owns = task => task.responsable?.toLowerCase() === session.usuario.username.toLowerCase();
    const createdByMe = task => task.creadoPor?.toLowerCase() === session.usuario.username.toLowerCase();
    const mayEdit = task => session.administrador || createdByMe(task);
    const mayDelete = task => session.administrador;
    const mayStock = task => session.administrador || owns(task);
    const mayComment = task => session.administrador || owns(task) || createdByMe(task);
    const status = task => task.estado === 'PENDIENTE' ? 'Pendiente' : task.estado === 'EN_PROCESO' ? 'En Proceso' : task.estado === 'CERRADA' ? 'Finalizada' : 'Cancelada';
    const date = value => value ? new Date(value).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' }) : '-';
    function message(text, error = false, target = 'message') {
        $(target).textContent = text;
        $(target).classList.toggle('error', error);
        $(target).hidden = !text;
    }
    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text != null) node.textContent = text;
        if (className) node.className = className;
        return node;
    }
    async function request(path, method = 'GET', data) {
        const response = await fetch(base + path, {
            method, credentials: 'same-origin', cache: 'no-store',
            headers: { Accept: 'application/json', ...(method !== 'GET' ? { [csrfHeader]: csrf, 'Content-Type': 'application/json' } : {}) },
            ...(data !== undefined ? { body: JSON.stringify(data) } : {}), signal: AbortSignal.timeout(15000)
        });
        if (response.status === 401 || response.redirected) {
            stopped = true;
            location.assign(base + 'movil/login');
            throw new Error('La sesion vencio. Ingrese nuevamente.');
        }
        if (!response.ok) {
            const errors = { 403: 'No tiene permiso para esta operacion o la sesion cambio. Vuelva a ingresar.',
                404: 'La tarea ya no existe.', 409: 'La tarea ya fue tomada o esta finalizada. Actualice la lista.',
                400: 'Revise los campos obligatorios y su longitud.' };
            let detail = '';
            try {
                const body = await response.json();
                detail = body.detail || body.message || body.error || '';
            } catch {
                try { detail = await response.text(); } catch { detail = ''; }
            }
            const suffix = detail ? ' ' + detail : '';
            throw new Error((errors[response.status] || ('No se pudo guardar. Codigo ' + response.status + '.')) + suffix);
        }
        return response.status === 204 ? null : response.json();
    }
    function render() {
        const token = ++renderToken;
        const today = new Date().toLocaleDateString('en-CA');
        $('pending-count').textContent = tasks.filter(t => t.estado === 'PENDIENTE').length;
        $('mine-count').textContent = tasks.filter(t => isOpen(t) && owns(t)).length;
        $('done-count').textContent = tasks.filter(t => t.estado === 'CERRADA' && t.cerradoEn && new Date(t.cerradoEn).toLocaleDateString('en-CA') === today).length;
        if (!session.mostrarMisTareas && filter === 'mine') filter = 'pending';
        if (!session.mostrarFinalizadas && filter === 'done') filter = 'pending';
        const query = $('search').value.trim().toLocaleLowerCase();
        const found = tasks.filter(t => {
            const match = filter === 'all' || (filter === 'pending' && t.estado === 'PENDIENTE') || (filter === 'mine' && owns(t)) || (filter === 'done' && t.estado === 'CERRADA');
            if (!session.mostrarFinalizadas && !isOpen(t)) return false;
            const text = [t.id, t.titulo, t.descripcion, t.equipoNombre, t.solicitanteUsername, t.solicitanteNombre, t.solicitanteFuero, t.responsable].join(' ').toLocaleLowerCase();
            return match && text.includes(query);
        });
        $('task-list').replaceChildren();
        for (const t of found.slice(0, limit)) {
            const row = element('article', null, 'task-row');
            const title = element('button', '#' + t.id + '  ' + t.titulo, 'task-open');
            title.onclick = () => openDetail(t);
            const badge = element('span', status(t), 'badge' + (t.estado === 'CERRADA' ? ' done' : t.estado === 'CANCELADA' ? ' cancelled' : t.estado === 'EN_PROCESO' ? ' progress' : ''));
            row.append(title, badge);
            if (t.descripcion) row.append(element('p', t.descripcion, 'task-description'));
            const meta = element('div', null, 'task-meta');
            meta.append(element('span', t.responsable || 'Sin tomar'), element('span', t.solicitanteFuero || '-'),
                element('span', t.prioridad, ['ALTA', 'URGENTE'].includes(t.prioridad) ? 'priority-high' : ''), element('span', date(t.creadoEn)));
            row.append(meta);
            const preview = element('section', 'Cargando comentarios...', 'task-comments-preview muted');
            preview.id = 'comments-preview-' + t.id;
            preview.setAttribute('aria-label', 'Comentarios de la tarea ' + t.id);
            row.append(preview);
            $('task-list').append(row);
            loadCommentPreview(t.id, token);
        }
        $('result-count').textContent = found.length + ' tareas';
        $('empty').hidden = found.length !== 0;
        $('more').hidden = found.length <= limit;
        $('task-list').setAttribute('aria-busy', 'false');
    }
    async function refresh() {
        tasks = await request(api);
        render();
    }
    async function comments(id) {
        const list = await request(api + '/' + id + '/comentarios');
        commentPreviewCache.set(id, list);
        if (selected?.id !== id) return;
        $('comments').replaceChildren();
        if (!list.length) $('comments').append(element('p', 'Sin comentarios.', 'muted'));
        for (const c of list) {
            const item = element('article', null, 'comment');
            item.append(element('small', c.autor + ' | ' + date(c.creadoEn)), element('p', c.comentario));
            $('comments').append(item);
        }
    }
    async function stockUsed(id) {
        const list = await request(api + '/' + id + '/stock');
        $('stock-used').replaceChildren();
        if (!list.length) $('stock-used').append(element('p', 'Sin stock registrado.', 'muted'));
        for (const uso of list) {
            const item = element('article', null, 'comment');
            const serial = uso.serial ? ' | ' + uso.serial : '';
            const note = uso.observacion ? ' | ' + uso.observacion : '';
            item.append(element('small', uso.registradoPor + ' | ' + date(uso.creadoEn)),
                element('p', '#' + uso.stockComponenteId + ' ' + uso.tipo + ' - ' + uso.descripcion + serial + note));
            $('stock-used').append(item);
        }
    }
    function applicantLabel(user) {
        const name = user.nombreVisible || user.username || '';
        const username = user.username ? ' (' + user.username + ')' : '';
        const fuero = user.fuero ? ' - ' + user.fuero : '';
        return name + username + fuero;
    }
    function rememberApplicant(user) {
        const label = applicantLabel(user);
        applicantOptions.set(label.toLocaleLowerCase(), user);
        if (user.username) applicantOptions.set(user.username.toLocaleLowerCase(), user);
        if (user.nombreVisible) applicantOptions.set(user.nombreVisible.toLocaleLowerCase(), user);
        return label;
    }
    function setApplicant(user, label) {
        const form = $('task-form');
        form.elements.solicitanteUsername.value = (user.username || label || '').trim().slice(0, 120);
        form.elements.solicitanteNombre.value = (user.nombreVisible || label || user.username || '').trim().slice(0, 180);
        form.elements.solicitanteFuero.value = (user.fuero || session.usuario.fuero || 'Sin fuero informado').trim().slice(0, 120);
        $('solicitante-search').value = label || applicantLabel(user);
    }
    function assigneeLabel(user) {
        const name = user.nombreVisible || user.username || '';
        const username = user.username ? ' (' + user.username + ')' : '';
        const fuero = user.fuero ? ' - ' + user.fuero : '';
        const source = user.fuente ? ' · ' + user.fuente : '';
        return name + username + fuero + source;
    }
    function rememberAssignee(user) {
        const label = assigneeLabel(user);
        assigneeOptions.set(label.toLocaleLowerCase(), user);
        if (user.username) assigneeOptions.set(user.username.toLocaleLowerCase(), user);
        if (user.nombreVisible) assigneeOptions.set(user.nombreVisible.toLocaleLowerCase(), user);
        return label;
    }
    function setAssignee(user, label) {
        const form = $('task-form');
        form.elements.responsable.value = (user.username || label || '').trim().slice(0, 120);
        $('responsable-search').value = label || assigneeLabel(user);
    }
    function syncAssigneeFromInput() {
        const form = $('task-form');
        const input = $('responsable-search').value.trim();
        if (!input) {
            form.elements.responsable.value = '';
            $('responsable-help').textContent = 'Sin técnico asignado: sonará en todos los celulares de técnicos y administradores.';
            return true;
        }
        const match = assigneeOptions.get(input.toLocaleLowerCase());
        if (match) {
            setAssignee(match, assigneeLabel(match));
            $('responsable-help').textContent = 'Técnico seleccionado para aviso dirigido.';
            return true;
        }
        form.elements.responsable.value = input.slice(0, 120);
        $('responsable-help').textContent = 'Responsable cargado manualmente. Se enviará aviso dirigido a ese usuario.';
        return true;
    }
    function syncApplicantFromInput() {
        const input = $('solicitante-search').value.trim();
        const match = applicantOptions.get(input.toLocaleLowerCase());
        if (match) {
            setApplicant(match, applicantLabel(match));
            $('solicitante-help').textContent = 'Solicitante seleccionado desde Active Directory.';
            return true;
        }
        if (!input) return false;
        setApplicant({ username: input, nombreVisible: input, fuero: session.usuario.fuero || 'Sin fuero informado' }, input);
        $('solicitante-help').textContent = 'Solicitante cargado manualmente.';
        return true;
    }
    async function searchApplicants(query) {
        const options = $('solicitante-options');
        if (query.length < 2) {
            options.replaceChildren();
            $('solicitante-help').textContent = 'Escriba al menos 2 letras para buscar en AD, o cargue el nombre manualmente.';
            return;
        }
        try {
            const result = await request('api/v1/movil/usuarios-dominio?q=' + encodeURIComponent(query));
            options.replaceChildren();
            applicantOptions.clear();
            for (const user of result.usuarios || []) {
                const option = document.createElement('option');
                option.value = rememberApplicant(user);
                options.append(option);
            }
            if (result.disponible && result.usuarios?.length) {
                $('solicitante-help').textContent = result.usuarios.length + ' coincidencias de AD. Elija una o continue manualmente.';
            } else {
                $('solicitante-help').textContent = result.mensaje || 'Sin coincidencias de AD; puede cargar el solicitante manualmente.';
            }
        } catch {
            $('solicitante-help').textContent = 'No se pudo consultar AD; puede cargar el solicitante manualmente.';
        }
    }
    async function searchAssignees(query) {
        const options = $('responsable-options');
        try {
            const result = await request('api/v1/movil/tecnicos-asignables?q=' + encodeURIComponent(query || ''));
            options.replaceChildren();
            assigneeOptions.clear();
            for (const user of result.usuarios || []) {
                const option = document.createElement('option');
                option.value = rememberAssignee(user);
                options.append(option);
            }
            if (result.usuarios?.length) {
                $('responsable-help').textContent = result.usuarios.length + ' responsables disponibles. Deje vacío para aviso general.';
            } else {
                $('responsable-help').textContent = result.mensaje || 'Sin coincidencias; puede dejarlo vacío o escribir un usuario.';
            }
        } catch {
            $('responsable-help').textContent = 'No se pudo consultar responsables; puede dejarlo vacío o escribir un usuario.';
        }
    }
    function renderStockOptions() {
        const select = $('stock-form').elements.stockComponenteId;
        select.replaceChildren();
        const empty = document.createElement('option');
        empty.value = '';
        empty.textContent = stockAvailable.length ? 'Seleccionar stock' : 'Sin stock disponible';
        select.append(empty);
        for (const stock of stockAvailable) {
            const option = document.createElement('option');
            option.value = stock.id;
            option.textContent = '#' + stock.id + ' ' + stock.tipo + ' - ' + stock.descripcion + (stock.serial ? ' (' + stock.serial + ')' : '');
            select.append(option);
        }
    }
    function renderCommentPreview(id, list) {
        const preview = $('comments-preview-' + id);
        if (!preview) return;
        preview.replaceChildren();
        if (!list.length) {
            preview.textContent = 'Sin comentarios cargados.';
            return;
        }
        preview.classList.remove('muted');
        preview.append(element('strong', 'Comentarios'));
        for (const c of list.slice(0, 2)) {
            preview.append(element('span', c.autor + ': ' + c.comentario));
        }
    }
    function sameText(a, b) {
        return (a || '').trim().toLocaleLowerCase() === (b || '').trim().toLocaleLowerCase();
    }
    async function loadCommentPreview(id, token) {
        if (commentPreviewCache.has(id)) {
            renderCommentPreview(id, commentPreviewCache.get(id));
            return;
        }
        try {
            const list = await request(api + '/' + id + '/comentarios');
            commentPreviewCache.set(id, list);
            if (token === renderToken) renderCommentPreview(id, list);
        } catch {
            const preview = $('comments-preview-' + id);
            if (preview && token === renderToken) preview.textContent = 'No se pudieron cargar comentarios.';
        }
    }
    async function openDetail(task) {
        selected = task;
        $('detail-title').textContent = '#' + task.id + '  ' + task.titulo;
        $('detail-status').textContent = status(task) + ' | Prioridad ' + task.prioridad;
        $('detail-description').textContent = task.descripcion || 'Sin descripcion.';
        $('detail-meta').replaceChildren();
        const detailMeta = [
            ['Solicitante', task.solicitanteNombre || task.solicitanteUsername],
            ['Fuero solicitante', task.solicitanteFuero],
            ['Responsable', task.responsable || 'Sin tomar'],
            ['Equipo', task.equipoNombre],
            ['Creada', date(task.creadoEn)],
            ['Finalizada', date(task.cerradoEn)]
        ];
        if (task.solicitanteUsername && !sameText(task.solicitanteUsername, task.solicitanteNombre)) {
            detailMeta.splice(1, 0, ['Usuario AD solicitante', task.solicitanteUsername]);
        }
        for (const [key, value] of detailMeta) {
            $('detail-meta').append(element('dt', key), element('dd', value || '-'));
        }
        $('take-task').hidden = !session.puedeEditar || !!task.responsable || !isOpen(task);
        if ($('release-task')) $('release-task').hidden = !owns(task) || !isOpen(task);
        $('edit-task').hidden = !mayEdit(task);
        $('delete-task').hidden = !mayDelete(task);
        $('comment-form').hidden = !mayComment(task);
        $('state-form').hidden = !session.administrador || !isOpen(task);
        $('stock-form').hidden = !mayStock(task) || !isOpen(task) || !stockAvailable.length;
        $('stock-section').hidden = !mayStock(task) && !session.puedeEditar;
        const estadoSelect = $('state-form').elements.estado;
        const observacionesInput = $('state-form').elements.observacionesCierre;
        estadoSelect.value = isOpen(task) ? 'PENDIENTE' : task.estado;
        observacionesInput.value = task.observacionesCierre || '';
        
        const updateRequired = () => {
            observacionesInput.required = estadoSelect.value === 'CERRADA' || estadoSelect.value === 'CANCELADA';
        };
        estadoSelect.onchange = updateRequired;
        updateRequired();
        $('comment-form').reset();
        $('stock-form').reset();
        renderStockOptions();
        message('', false, 'detail-message');
        $('comments').textContent = 'Cargando comentarios...';
        $('stock-used').textContent = 'Cargando stock...';
        if (!$('detail-dialog').open) $('detail-dialog').showModal();
        try { await comments(task.id); } catch (error) { message(error.message, true, 'detail-message'); }
        try { await stockUsed(task.id); } catch (error) { message(error.message, true, 'detail-message'); }
    }
    async function act(action, target = 'detail-message') {
        if (busy) return;
        busy = true;
        document.querySelectorAll('dialog button').forEach(button => button.disabled = true);
        try { await action(); } catch (error) { message(error.message || 'Sin conexion con el servidor.', true, target); }
        finally {
            busy = false;
            document.querySelectorAll('dialog button').forEach(button => button.disabled = false);
        }
    }
    function openForm(task = null) {
        editing = task;
        const form = $('task-form');
        form.reset();
        $('form-title').textContent = task ? 'Editar tarea #' + task.id : 'Nueva tarea';
        const defaults = task || { solicitanteUsername: session.usuario.username, solicitanteNombre: session.usuario.nombreVisible, solicitanteFuero: session.usuario.fuero, prioridad: 'MEDIA' };
        for (const control of form.elements) if (control.name && defaults[control.name] != null) control.value = defaults[control.name];
        setApplicant({
            username: defaults.solicitanteUsername,
            nombreVisible: defaults.solicitanteNombre,
            fuero: defaults.solicitanteFuero
        }, defaults.solicitanteNombre || defaults.solicitanteUsername || '');
        $('responsable-field').hidden = !session.puedeAsignarResponsable;
        $('responsable-search').value = defaults.responsable || '';
        $('responsable-help').textContent = defaults.responsable
            ? 'Responsable asignado. Puede cambiarlo antes de guardar.'
            : 'Si lo deja vacío, sonará en todos los celulares de técnicos y administradores.';
        if (session.puedeAsignarResponsable) searchAssignees(defaults.responsable || '');
        const puedeDictar = task ? mayEdit(task) : session.puedeCrear;
        $('voice-problem').hidden = !native || !puedeDictar;
        message('', false, 'form-message');
        $('task-dialog').showModal();
    }
    function titleFromProblem(text) {
        const clean = (text || '').trim().replace(/\s+/g, ' ');
        let issue = clean || 'Tarea tecnica';
        const issueMatch = clean.match(/\b(?:problema|inconveniente|falla|fallo|error|porque|que|indica|dice)\b\s*(.+)$/i);
        if (issueMatch) issue = issueMatch[1].trim();
        return issue.length > 90 ? issue.slice(0, 87).trim() + '...' : issue;
    }
    function fillProblemDictation(text, note) {
        const form = $('task-form');
        form.elements.descripcion.value = (text || '').trim();
        form.elements.titulo.value = titleFromProblem(text);
        message(note || 'Problema dictado. Revise antes de guardar.', false, 'form-message');
    }
    function applyDictation(text) {
        if (!$('task-dialog').open) openForm();
        fillProblemDictation(text, 'Problema dictado. Revise antes de guardar.');
    }
    $('task-form').onsubmit = event => {
        event.preventDefault();
        act(async () => {
            const data = Object.fromEntries(new FormData(event.target));
            data.descripcion = data.descripcion?.trim() || '';
            if (!syncApplicantFromInput()) throw new Error('Indique quien solicito la ayuda.');
            data.solicitanteUsername = data.solicitanteUsername?.trim();
            data.solicitanteNombre = data.solicitanteNombre?.trim();
            data.solicitanteFuero = data.solicitanteFuero?.trim() || 'Sin fuero informado';
            // En movil el tecnico carga el problema; el titulo queda derivado para cumplir el contrato API.
            data.titulo = titleFromProblem(data.descripcion);
            data.equipoId = editing?.equipoId || null;
            syncAssigneeFromInput();
            data.responsable = event.target.elements.responsable.value?.trim() || null;
            const saved = await request(api + (editing ? '/' + editing.id : ''), editing ? 'PUT' : 'POST', data);
            $('task-dialog').close();
            commentPreviewCache.delete(saved.id);
            await refresh();
            await openDetail(tasks.find(t => t.id === saved.id) || saved);
            message('Tarea guardada.', false, 'detail-message');
        }, 'form-message');
    };
    $('take-task').onclick = () => act(async () => {
        const task = await request(api + '/' + selected.id + '/tomar', 'POST');
        await refresh(); await openDetail(task); message('La tarea quedo a su cargo.', false, 'detail-message');
    });
    if ($('release-task')) {
        $('release-task').onclick = () => act(async () => {
            const task = await request(api + '/' + selected.id + '/soltar', 'POST');
            await refresh(); await openDetail(task); message('Tarea liberada y devuelta a Nuevas.', false, 'detail-message');
        });
    }
    $('state-form').onsubmit = event => {
        event.preventDefault();
        act(async () => {
            const data = Object.fromEntries(new FormData(event.target));
            if (selected.estado === 'EN_PROCESO' && data.estado === 'PENDIENTE') data.estado = 'EN_PROCESO';
            const task = await request(api + '/' + selected.id + '/estado', 'PATCH', data);
            await refresh(); await openDetail(task); message('Estado guardado.', false, 'detail-message');
        });
    };
    $('comment-form').onsubmit = event => {
        event.preventDefault();
        act(async () => {
            await request(api + '/' + selected.id + '/comentarios', 'POST', Object.fromEntries(new FormData(event.target)));
            commentPreviewCache.delete(selected.id);
            event.target.reset();
            await comments(selected.id);
            await refresh();
            selected = tasks.find(t => t.id === selected.id) || selected;
            message('Comentario guardado.', false, 'detail-message');
        });
    };
    $('stock-form').onsubmit = event => {
        event.preventDefault();
        act(async () => {
            await request(api + '/' + selected.id + '/stock', 'POST', Object.fromEntries(new FormData(event.target)));
            stockAvailable = await request(api + '/stock-disponible');
            event.target.reset();
            renderStockOptions();
            await stockUsed(selected.id);
            await refresh();
            message('Stock registrado en la tarea.', false, 'detail-message');
        });
    };
    $('delete-task').onclick = () => {
        if (!confirm('Eliminar la tarea #' + selected.id + ' y sus comentarios?')) return;
        act(async () => { await request(api + '/' + selected.id, 'DELETE'); $('detail-dialog').close(); await refresh(); message('Tarea eliminada.'); });
    };
    $('edit-task').onclick = () => openForm(selected);
    $('new-task').onclick = () => openForm();
    $('solicitante-search').addEventListener('input', event => {
        clearTimeout(applicantSearchTimer);
        const value = event.target.value.trim();
        applicantSearchTimer = setTimeout(() => searchApplicants(value), 300);
    });
    $('solicitante-search').addEventListener('change', syncApplicantFromInput);
    $('responsable-search').addEventListener('input', event => {
        clearTimeout(assigneeSearchTimer);
        const value = event.target.value.trim();
        assigneeSearchTimer = setTimeout(() => searchAssignees(value), 300);
    });
    $('responsable-search').addEventListener('change', syncAssigneeFromInput);
    $('voice-task').onclick = () => {
        openForm();
        if (window.TareasLan?.dictarTarea) window.TareasLan.dictarTarea();
        else message('El dictado esta disponible desde la APK instalada.', true);
    };
    $('voice-problem').onclick = () => {
        if (window.TareasLan?.dictarTarea) window.TareasLan.dictarTarea();
        else message('El dictado esta disponible desde la APK instalada.', true, 'form-message');
    };
    window.addEventListener('tareas-lan-dictado', event => applyDictation(event.detail));
    $('search').oninput = () => { limit = 40; render(); };
    $('more').onclick = () => { limit += 40; render(); };
    $('refresh').onclick = () => (session ? refresh() : start()).then(() => message('Lista actualizada.')).catch(error => message(error.message, true));
    document.querySelectorAll('[data-close]').forEach(button => button.onclick = () => $(button.dataset.close).close());
    document.querySelectorAll('[data-filter]').forEach(button => button.onclick = () => {
        filter = button.dataset.filter; limit = 40; $('list-title').textContent = button.textContent;
        document.querySelectorAll('[data-filter]').forEach(b => b.setAttribute('aria-pressed', String(b === button))); render();
    });
    function beep() {
        if (!audio || audio.state !== 'running') return;
        const oscillator = audio.createOscillator(), gain = audio.createGain();
        oscillator.connect(gain); gain.connect(audio.destination); oscillator.frequency.value = 880;
        gain.gain.setValueAtTime(.12, audio.currentTime); gain.gain.exponentialRampToValueAtTime(.001, audio.currentTime + .45);
        oscillator.start(); oscillator.stop(audio.currentTime + .45);
    }
    $('sound').hidden = native;
    if (native && $('download-apk')) $('download-apk').hidden = true;
    $('sound').onclick = async () => {
        try {
            if (!audio) audio = new (window.AudioContext || window.webkitAudioContext)();
            await audio.resume(); sound = !sound;
            $('sound').setAttribute('aria-pressed', String(sound));
            const label = sound ? 'Silenciar esta pantalla' : 'Activar sonido en esta pantalla';
            $('sound').setAttribute('aria-label', label); $('sound').title = label;
            $('sound').replaceChildren();
            const icon = element('i'); icon.dataset.lucide = sound ? 'bell' : 'bell-off'; $('sound').append(icon); icons();
            if (sound) beep();
        } catch { message('El navegador no pudo activar el sonido.', true); }
    };
    async function poll() {
        if (stopped) return;
        try {
            const batch = await request('api/v1/movil/avisos' + (cursor != null ? '?despuesDe=' + cursor : ''));
            const incoming = batch.avisos.filter(a => a.autor?.toLowerCase() !== session.usuario.username.toLowerCase());
            if (incoming.length && !native) {
                const first = incoming[0];
                const prefix = first.tipo === 'COMENTARIO' ? 'Nuevo comentario: ' : 'Nueva tarea: ';
                message(incoming.length === 1 ? prefix + first.titulo : incoming.length + ' avisos nuevos.');
                if (sound) beep();
            }
            cursor = batch.siguiente;
            try { localStorage.setItem(cursorKey, String(cursor)); } catch { /* El seguimiento sigue en memoria. */ }
            if (!document.querySelector('dialog[open]') && !busy) await refresh();
            $('connection').textContent = 'Conectado'; $('connection').classList.remove('offline');
        } catch {
            $('connection').textContent = 'Sin conexion. Reintentando...'; $('connection').classList.add('offline');
        } finally { if (!stopped) setTimeout(poll, 10000); }
    }
    async function start() {
        try {
            session = await request('api/v1/movil/sesion');
            $('username').textContent = session.usuario.nombreVisible + ' | ' + session.usuario.username;
            $('mine-metric').hidden = !session.mostrarMisTareas;
            $('done-metric').hidden = !session.mostrarFinalizadas;
            $('filter-mine').hidden = !session.mostrarMisTareas;
            $('filter-done').hidden = !session.mostrarFinalizadas;
            $('new-task').hidden = !session.puedeCrear;
            $('voice-task').hidden = !native || !session.puedeCrear;
            stockAvailable = session.puedeEditar ? await request(api + '/stock-disponible') : [];
            cursorKey = 'tareas.cursor.' + base + '.' + session.usuario.username;
            try { const value = localStorage.getItem(cursorKey); if (value !== null && /^\d+$/.test(value)) cursor = Number(value); } catch { /* Almacenamiento opcional. */ }
            await refresh();
            const id = Number(new URLSearchParams(location.search).get('tarea'));
            if (id) { const task = tasks.find(t => t.id === id); if (task) await openDetail(task); else message('La tarea solicitada ya no existe.', true); }
            poll();
        } catch (error) { message(error.message || 'No se pudo conectar. Recargue la pantalla.', true); $('connection').textContent = 'No conectado'; }
    }
    icons(); start();
})();
