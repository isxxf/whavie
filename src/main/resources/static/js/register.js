/* ===========================
   REGISTER.JS - REGISTRO DE NUEVOS USUARIOS
   =========================== */

document.addEventListener('DOMContentLoaded', function () {
    const btnRegistrar = document.getElementById('btn-registrar');

    if (!btnRegistrar) return;

    // ===========================
    // FUNCIONES AUXILIARES
    // ===========================

    /**
     * Limpia todos los mensajes de error dinámicos mostrados anteriormente
     */
    function limpiarErrores() {
        document.querySelectorAll('.aviso-error-dinamico').forEach(e => e.remove());
    }

    /**
     * Muestra un mensaje de error dinámico en la página
     * Se inserta antes del formulario con icono y estilos
     * @param {string} mensaje - Texto del error a mostrar
     */
    function mostrarError(mensaje) {
        const div = document.createElement('div');
        div.className = 'aviso-auth aviso-error aviso-error-dinamico';

        const icon = document.createElement('i');
        icon.className = 'fas fa-exclamation-circle';

        div.appendChild(icon);
        div.appendChild(document.createTextNode(' ' + mensaje));

        // Inserta el error antes del contenedor del formulario
        document.querySelector('.auth-wrapper').insertBefore(div, document.querySelector('.auth-container'));
    }

    /**
     * Valida que el email tenga formato correcto
     * @param {string} email - Email a validar
     * @returns {boolean} True si es válido, false en caso contrario
     */
    function emailValido(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    // ===========================
    // EVENTO DE ENVÍO DEL FORMULARIO
    // ===========================

    btnRegistrar.addEventListener('click', function (e) {
        e.preventDefault();
        limpiarErrores();

        // Obtiene valores del formulario
        const email = document.getElementById('email').value.trim().toLowerCase();
        const username = document.getElementById('username').value.trim().toLowerCase();
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Validación 1: Las contraseñas deben coincidir
        if (password !== confirmPassword) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        // Validación 2: Email debe ser válido
        if (!emailValido(email)) {
            mostrarError("El email no es válido.");
            return;
        }

        const requestData = {email, username, password};

        // Envía la solicitud de registro al servidor
        fetch('/api/auth/register', {
            method: 'POST',
            headers: {...getCsrfHeaders(), 'Content-Type': 'application/json'},
            body: JSON.stringify(requestData)
        })
            .then(response => {
                if (response.ok) {
                    // Registro exitoso, redirige a login
                    window.location.href = '/login?registrado=true';
                } else {
                    // Registro fallido, extrae y muestra los errores
                    return extractErrorMessage(response).then(errorData => {
                        if (errorData.fieldErrors) {
                            // Muestra errores de validación por campo
                            Object.values(errorData.fieldErrors).forEach(error => mostrarError(error));
                        } else {
                            // Muestra error general
                            mostrarError(errorData.text);
                        }
                    });
                }
            })
            .catch(error => {
                // Error de red o de parsing
                mostrarError(error.message || "Error al registrar.");
            });
    });
});