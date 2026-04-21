/* ===========================
   RESET-PASSWORD.JS - RECUPERAR CONTRASENA
   =========================== */

document.addEventListener('DOMContentLoaded', function () {
    const btnReset = document.getElementById('btn-reset');

    if (!btnReset) return;

    function limpiarMensajes() {
        document.querySelectorAll('.aviso-error-dinamico, .aviso-success-dinamico').forEach(e => e.remove());
    }

    function mostrarMensaje(mensaje, esError = true) {
        const div = document.createElement('div');
        div.className = 'aviso-auth ' + (esError ? 'aviso-error' : 'aviso-success') + ' ' + (esError ? 'aviso-error-dinamico' : 'aviso-success-dinamico');

        const icon = document.createElement('i');
        icon.className = esError ? 'fas fa-exclamation-circle' : 'fas fa-check-circle';

        div.appendChild(icon);
        div.appendChild(document.createTextNode(' ' + mensaje));

        document.querySelector('.auth-wrapper').insertBefore(div, document.querySelector('.auth-container'));
    }

    const token = new URLSearchParams(window.location.search).get('token');
    const tokenInput = document.getElementById('resetToken');

    if (tokenInput) {
        tokenInput.value = token || '';
    }

    if (!token) {
        mostrarMensaje('El link de recuperación no es válido o está incompleto.');
        btnReset.disabled = true;
        return;
    }

    btnReset.addEventListener('click', function (e) {
        e.preventDefault();
        limpiarMensajes();

        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (!password || !confirmPassword) {
            mostrarMensaje('Completá ambos campos de contraseña.');
            return;
        }

        if (password.trim().length < 5) {
            mostrarMensaje('La contraseña debe tener mínimo 5 caracteres.');
            return;
        }

        if (password !== confirmPassword) {
            mostrarMensaje('Las contraseñas no coinciden.');
            return;
        }

        const body = new URLSearchParams({
            token: token,
            nuevaPassword: password
        }).toString();

        fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: {...getCsrfHeaders(), 'Content-Type': 'application/x-www-form-urlencoded'},
            body: body
        })
            .then(response => {
                if (response.ok) {
                    showWhavieAlert('Contraseña actualizada', 'Ya podés iniciar sesión.', 'success')
                        .then(() => window.location.href = '/login');
                    return;
                }
                return extractErrorMessage(response).then(errorData => {
                    mostrarMensaje(errorData.text || 'No se pudo actualizar la contraseña.');
                });
            })
            .catch(error => {
                mostrarMensaje(error.message || 'Error al actualizar la contraseña.');
            });
    });
});
