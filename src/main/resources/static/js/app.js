document.addEventListener('DOMContentLoaded', () => {

    // confirmar acciones y evitar doble envio
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', e => {
            const msg = form.dataset.confirm;
            if (msg && !confirm(msg)) {
                e.preventDefault();
                return;
            }
            const btn = form.querySelector('button[type="submit"]');
            if (btn) setTimeout(() => { btn.disabled = true; }, 0);
        });
    });

    // chat: bajar al ultimo mensaje
    const chat = document.querySelector('.chat-box');
    if (chat) chat.scrollTop = chat.scrollHeight;

    // registro: mostrar tarjeta profesional solo para contadores
    const rol = document.getElementById('rol');
    const tarjeta = document.getElementById('tarjetaProfesional');
    const label = document.querySelector('label[for="tarjetaProfesional"]');
    if (rol && tarjeta && label) {
        const actualizar = () => {
            const esContador = rol.value !== 'PERSONA';
            const visible = esContador ? '' : 'none';
            tarjeta.style.display = visible;
            label.style.display = visible;
            // registro: soportes de verificacion (el de la tarjeta es obligatorio para contadores)
            document.querySelectorAll('[data-solo-contador]').forEach(el => { el.style.display = visible; });
            document.querySelectorAll('[data-requerido-contador]').forEach(el => { el.required = esContador; });
        };
        rol.addEventListener('change', actualizar);
        actualizar();
    }

    // modales de confirmacion (fragments/modal.html): ninguna accion importante se ejecuta con un clic
    document.querySelectorAll('[data-abrir-modal]').forEach(boton => {
        boton.addEventListener('click', () => {
            const dialogo = document.getElementById(boton.dataset.abrirModal);
            if (dialogo && typeof dialogo.showModal === 'function') {
                // un mismo modal sirve para varios elementos (p. ej. cada documento): el boton
                // indica la ruta del formulario y los datos que se muestran o preseleccionan
                const form = dialogo.querySelector('form');
                if (form && boton.dataset.accion && boton.dataset.accion.startsWith('/')) {
                    form.setAttribute('action', boton.dataset.accion);
                }
                dialogo.querySelectorAll('[data-modal-doc]').forEach(el => { el.textContent = boton.dataset.doc || ''; });
                // asignar o cambiar mentor: un solo modal, el boton "Seleccionar" indica el mentor elegido
                const mentor = form ? form.querySelector('input[name="mentorId"]') : null;
                if (mentor && boton.dataset.mentor) mentor.value = boton.dataset.mentor;
                dialogo.showModal();
                const selectSolicitud = form ? form.querySelector('select[name="solicitudId"]') : null;
                if (selectSolicitud && boton.dataset.solicitud !== undefined) {
                    selectSolicitud.value = boton.dataset.solicitud;
                    selectSolicitud.dispatchEvent(new Event('change', { bubbles: true }));
                }
                // lista de verificacion: "Subir" preselecciona el requisito o el documento pedido
                const requerimiento = form ? form.querySelector('input[name="requerimientoId"]') : null;
                if (requerimiento) {
                    requerimiento.value = boton.dataset.requerimiento || '';
                    dialogo.querySelectorAll('[data-aviso-requerimiento]').forEach(el => { el.hidden = !boton.dataset.requerimiento; });
                    const nombre = form.querySelector('input[name="nombre"]');
                    if (nombre && boton.dataset.requerimiento && boton.dataset.doc) nombre.value = boton.dataset.doc;
                }
                const selectRequisito = form ? form.querySelector('select[name="requisito"]') : null;
                if (selectRequisito && boton.dataset.requisito) {
                    selectRequisito.value = boton.dataset.requisito;
                    const nombre = form.querySelector('input[name="nombre"]');
                    if (nombre && !nombre.value) nombre.value = boton.dataset.requisito.substring(0, 120);
                }
                form && form.dispatchEvent(new Event('input'));
                const primero = dialogo.querySelector('select, textarea, input:not([type="hidden"])');
                if (primero) primero.focus();
            }
        });
    });

    document.querySelectorAll('dialog.modal').forEach(dialogo => {
        // "Cancelar" y la X cierran; Escape lo cierra de forma nativa
        dialogo.querySelectorAll('[data-cerrar-modal]').forEach(b => b.addEventListener('click', () => dialogo.close()));
        // clic fuera del cuadro (en el fondo oscuro)
        dialogo.addEventListener('click', e => { if (e.target === dialogo) dialogo.close(); });

        const form = dialogo.querySelector('form');
        if (!form) return;
        const confirmar = form.querySelectorAll('[data-confirmar]');
        const casillas = form.querySelectorAll('[data-confirmacion]');
        const selectResumen = form.querySelector('select[data-resumen]');
        const destino = form.querySelector('[data-resumen-destino]');

        // el boton de confirmar sigue deshabilitado hasta que los campos required (con su
        // longitud minima) esten completos y la casilla de confirmacion este marcada
        const validar = () => {
            const camposOk = Array.from(form.elements).every(el => {
                if (!el.willValidate) return true;
                if (el.required && !String(el.value).trim()) return false;
                const min = el.getAttribute('minlength');
                if (min && String(el.value).trim().length < Number(min)) return false;
                return el.checkValidity();
            });
            const casillasOk = Array.from(casillas).every(c => c.checked);
            confirmar.forEach(b => { b.disabled = !(camposOk && casillasOk); });
            if (selectResumen && destino) {
                const opcion = selectResumen.options[selectResumen.selectedIndex];
                destino.textContent = opcion && opcion.dataset.destino ? opcion.dataset.destino : 'RESUELTA o CERRADA';
            }
        };
        form.addEventListener('input', validar);
        form.addEventListener('change', validar);
        dialogo.addEventListener('close', () => { form.reset(); validar(); });
        validar();
    });

    // "Mis documentos": los requisitos dependen del tramite de la solicitud elegida
    document.querySelectorAll('select[data-filtra-requisitos]').forEach(selectSolicitud => {
        const form = selectSolicitud.form;
        const selectRequisito = form ? form.querySelector('select[name="requisito"]') : null;
        if (!selectRequisito) return;
        const filtrar = () => {
            const id = selectSolicitud.value;
            Array.from(selectRequisito.options).forEach(op => {
                if (!op.dataset.solicitud) return;
                const visible = id !== '' && op.dataset.solicitud === id;
                op.hidden = !visible;
                op.disabled = !visible;
            });
            const actual = selectRequisito.options[selectRequisito.selectedIndex];
            if (actual && actual.disabled) selectRequisito.value = '';
        };
        selectSolicitud.addEventListener('change', filtrar);
        form.addEventListener('reset', () => setTimeout(filtrar, 0));
        filtrar();
    });

    // nueva solicitud: mostrar los requisitos al elegir que declarar
    // (solo aplica a la declaracion de renta)
    const tipo = document.getElementById('titulo');
    const aviso = document.getElementById('aviso-renta');
    if (tipo && aviso) {
        const mostrar = () => {
            aviso.style.display = tipo.value.toLowerCase().includes('de renta') ? 'block' : 'none';
        };
        tipo.addEventListener('change', mostrar);
        mostrar();
    }
});