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
            const visible = rol.value !== 'PERSONA' ? '' : 'none';
            tarjeta.style.display = visible;
            label.style.display = visible;
        };
        rol.addEventListener('change', actualizar);
        actualizar();
    }

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