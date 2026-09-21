(() => {
    'use strict';
    
    let draggedCard = null;
    let draggedId = null;

    const READONLY_MESSAGE = 'El visor es de solo lectura. Solo los administradores pueden mover o modificar tareas.';

    document.addEventListener('dragstart', e => {
        // No arrastrar si el usuario hace clic o interactúa desde un botón, link, badges o formulario interno
        if (e.target.closest('button, input, select, textarea, a, .kcard-actions, .kcard-badges')) {
            return;
        }
        const card = e.target.closest('.kcard');
        if (!card) return;

        const isAdmin = document.body.dataset.isAdmin === 'true';
        if (!isAdmin) {
            e.preventDefault();
            alert(READONLY_MESSAGE);
            return;
        }

        draggedCard = card;
        draggedId = card.dataset.id;
        card.classList.add('is-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', draggedId);
    });

    document.addEventListener('dragend', () => {
        if (draggedCard) {
            draggedCard.classList.remove('is-dragging');
        }
        draggedCard = null;
        draggedId = null;
        document.querySelectorAll('.kanban-col').forEach(col => col.classList.remove('drag-over'));
    });

    document.addEventListener('dragover', e => {
        const col = e.target.closest('.kanban-col');
        if (col && draggedCard) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            col.classList.add('drag-over');
        }
    });

    document.addEventListener('dragleave', e => {
        const col = e.target.closest('.kanban-col');
        if (col && !col.contains(e.relatedTarget)) {
            col.classList.remove('drag-over');
        }
    });

    const getCsrfHeaders = () => {
        const token = document.querySelector('meta[name="csrf-token"]')?.content;
        const header = document.querySelector('meta[name="csrf-header"]')?.content;
        const headers = {};
        if (token && header) {
            headers[header] = token;
        }
        return headers;
    };

    document.addEventListener('drop', e => {
        const col = e.target.closest('.kanban-col');
        if (!col) return;

        const isAdmin = document.body.dataset.isAdmin === 'true';
        if (!isAdmin) {
            e.preventDefault();
            col.classList.remove('drag-over');
            alert(READONLY_MESSAGE);
            return;
        }

        if (!draggedId || !draggedCard) return;
        e.preventDefault();
        col.classList.remove('drag-over');
        
        const targetStatus = col.dataset.status;
        const sourceCol = draggedCard.closest('.kanban-col');
        const sourceStatus = sourceCol ? sourceCol.dataset.status : null;
        
        if (sourceStatus === targetStatus) return; // Misma columna
        
        const base = document.body.dataset.base || '/';
        const headers = getCsrfHeaders();
        const currentResponsable = draggedCard.dataset.responsable || '';

        if (targetStatus === 'EN_PROCESO') {
            // Mover a En Proceso: permitir elegir técnico al administrador
            const modal = document.getElementById('modal-dnd-asignar');
            const form = document.getElementById('form-dnd-asignar');
            if (modal && form) {
                form.action = `${base}admin/tareas/${draggedId}/tomar`;
                form.reset();
                if (currentResponsable) {
                    const select = form.querySelector('select[name="responsable"]');
                    if (select && Array.from(select.options).some(o => o.value === currentResponsable)) {
                        select.value = currentResponsable;
                    }
                }
                modal.showModal();
            } else {
                fetch(`${base}admin/tareas/${draggedId}/estado`, {
                    method: 'POST',
                    headers: headers,
                    body: new URLSearchParams({ estado: 'EN_PROCESO', origen: 'visor' })
                }).then(r => { if (r.ok) location.reload(); else alert('Error al cambiar estado.'); });
            }
        } else if (targetStatus === 'CERRADA') {
            // Mover a Terminadas: exigir solución y confirmar quién la resolvió
            const modal = document.getElementById('modal-dnd-resolver');
            const form = document.getElementById('form-dnd-resolver');
            if (modal && form) {
                form.action = `${base}admin/tareas/${draggedId}/estado`;
                form.reset();
                if (currentResponsable) {
                    const select = form.querySelector('select[name="responsable"]');
                    if (select && Array.from(select.options).some(o => o.value === currentResponsable)) {
                        select.value = currentResponsable;
                    }
                }
                modal.showModal();
            }
        } else if (targetStatus === 'CANCELADA') {
            // Mover a Canceladas: exigir motivo
            const modal = document.getElementById('modal-dnd-cancelar');
            const form = document.getElementById('form-dnd-cancelar');
            if (modal && form) {
                form.action = `${base}admin/tareas/${draggedId}/estado`;
                form.reset();
                modal.showModal();
            }
        } else if (targetStatus === 'PENDIENTE') {
            if (confirm('¿Deseas devolver esta tarea al estado Nueva (sin asignar)?')) {
                fetch(`${base}admin/tareas/${draggedId}/soltar`, {
                    method: 'POST',
                    headers: headers,
                    body: new URLSearchParams({ origen: 'visor' })
                }).then(r => { if (r.ok) location.reload(); else alert('Error al soltar la tarea.'); });
            }
        }
    });

    const setupAjaxForm = (formId) => {
        const form = document.getElementById(formId);
        if (!form) return;
        form.onsubmit = e => {
            e.preventDefault();
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Guardando...';
            }
            
            const headers = getCsrfHeaders();
            fetch(form.action, {
                method: 'POST',
                headers: headers,
                body: new URLSearchParams(new FormData(form))
            }).then(async r => {
                if (r.ok) {
                    location.reload();
                } else {
                    const text = await r.text();
                    alert('Error al guardar: ' + (text || ''));
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.textContent = 'Intentar de nuevo';
                    }
                }
            }).catch(() => {
                alert('Error de red al guardar.');
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Intentar de nuevo';
                }
            });
        };
    };

    setupAjaxForm('form-dnd-resolver');
    setupAjaxForm('form-dnd-cancelar');
    setupAjaxForm('form-dnd-asignar');

})();
