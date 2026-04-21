/* ===========================
   UNIRSE-SALA.JS - UNIRSE A UNA SALA EXISTENTE
   =========================== */

// ===========================
// CONFIGURACIÓN OTP INPUTS
// ===========================
const inputs = document.querySelectorAll('.otp-input');
const botonUnirse = document.querySelector('.btn-primary');

/**
 * Configura los inputs OTP para:
 * - Convertir entrada a mayúsculas
 * - Auto-avanzar al siguiente input
 * - Retroceder con backspace
 * - Enviar con Enter
 * - Pegar código completo
 */
inputs.forEach((input, index) => {
    // Evento de entrada: convierte a mayúsculas y auto-avanza
    input.addEventListener('input', (e) => {
        e.target.value = e.target.value.toUpperCase();

        if (e.target.value.length === 1 && index < inputs.length - 1) {
            inputs[index + 1].focus();
        }
    });

    // Evento de tecla: maneja backspace y Enter
    input.addEventListener('keydown', (e) => {
        // Backspace: retrocede al input anterior
        if (e.key === "Backspace" && e.target.value === "" && index > 0) {
            inputs[index - 1].focus();
        }
        // Enter: envía el formulario
        if (e.key === "Enter") {
            e.preventDefault();
            if (botonUnirse) {
                botonUnirse.click();
                botonUnirse.disabled = true;
                setTimeout(() => {
                    botonUnirse.disabled = false;
                }, 1500);
            }
        }
    });

    // Evento de pegar: permite pegar código completo
    input.addEventListener('paste', (e) => {
        e.preventDefault();
        const textoPegado = (e.clipboardData || window.clipboardData).getData('text');
        const limpio = textoPegado.trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
        // Distribuye caracteres en cada input
        for (let i = 0; i < inputs.length; i++) {
            inputs[i].value = limpio[i] || '';
        }
        // Coloca el foco en el último input completado
        const ultimoIndex = Math.min(limpio.length, inputs.length) - 1;
        if (ultimoIndex >= 0) {
            inputs[ultimoIndex].focus();
        }
    });
});

// ===========================
// CONTROL DE PASOS
// ===========================

/**
 * Muestra u oculta los pasos del formulario (step 1 o step 2)
 * Step 1: Ingreso del código OTP
 * Step 2: Ingreso de nombre para invitado
 * @param {number} stepNumber - Número del paso a mostrar (1 o 2)
 */
function showStep(stepNumber) {
    const step1 = document.getElementById('step-1');
    const step2 = document.getElementById('step-2');

    if (step1) step1.style.display = 'none';
    if (step2) step2.style.display = 'none';

    const target = document.getElementById('step-' + stepNumber);
    if (target) target.style.display = 'block';
}

// ===========================
// VERIFICACIÓN Y ENTRADA A SALA
// ===========================

/**
 * Verifica que el código OTP sea válido y entra a la sala
 * Si el usuario está logueado, entra directamente
 * Si no, redirige a step 2 para ingresar nombre de invitado
 */
async function verificarCodigoOTP() {
    let codigoCompleto = "";
    inputs.forEach(i => codigoCompleto += i.value);

    // Valida que el código esté completo
    if (codigoCompleto.length < 7) {
        showWhavieAlert("¡Hey!", "Por favor, completa el código de la sala.", "warning");
        return;
    }

    try {
        // Intenta obtener la sala
        const responseSala = await fetch(`/sala/obtener-sala/${codigoCompleto}`);
        if (!responseSala.ok) {
            showWhavieAlert("¡Ups!", "No se encontró la sala con ese código.", "error");
            return;
        }
        window.currentRoomCode = codigoCompleto;

        // Intenta entrar a la sala
        const resUnion = await fetch(`/sala/${codigoCompleto}`, {
            method: 'POST',
            headers: getCsrfHeaders(),
            credentials: 'include'
        });

        if (resUnion.ok) {
            // Entrada exitosa, redirige a la sala
            window.location.href = `/sala/${codigoCompleto}`;
        } else {
            // Maneja diferentes errores
            let errorData = {};
            try {
                errorData = await resUnion.json();
            } catch (e) {
                console.warn('No se pudo parsear respuesta de error:', e);
            }

            if (resUnion.status === 403 && errorData.error === "SALA_LLENA") {
                showWhavieAlert("¡Sala Llena!", errorData.message, "warning");
            } else if (resUnion.status === 400) {
                // Usuario no logueado: muestra step 2 para ingresar nombre
                if (isUserLogueado()) {
                    showWhavieAlert("¡Ups!", errorData.message, "error");
                } else {
                    showStep(2);
                }
            } else {
                showWhavieAlert("¡Ups!", "No pudimos procesar tu solicitud.", "error");
            }
        }
    } catch (err) {
        console.error(err);
        alert("Hubo un problema al verificar la sala. Intenta de nuevo en unos segundos.");
    }
}

/**
 * Permite que un invitado (usuario no autenticado) entre a la sala
 * Requiere ingresar un nombre de usuario
 */
async function entrarComoInvitado() {
    const nombre = document.getElementById('guestName').value.trim().toLowerCase();
    const codigo = window.currentRoomCode;

    // Validaciones
    if (!codigo) {
        showWhavieAlert("¡Hey!", "Verifica el código de la sala.", "warning");
        return;
    }

    if (nombre === "") {
        showWhavieAlert("¡Hey!", "Por favor, ingresa tu nombre.", "warning");
        return;
    }

    const url = `/sala/${codigo}?nombreInvitado=${encodeURIComponent(nombre)}`;

    try {
        const res = await fetch(url, {
            method: 'POST',
            headers: getCsrfHeaders(),
            credentials: 'include'
        });

        if (!res.ok) {
            let errorText = "No pudimos procesar tu solicitud";

            try {
                const errorJson = JSON.parse(await res.text());
                errorText = errorJson.message || errorJson.error || errorText;
            } catch (e) {
                console.warn('No se pudo parsear respuesta de error:', e);
            }

            // Maneja errores específicos
            if (res.status === 409) {
                showWhavieAlert("¡Aviso!", "Ya estás en esta sala. Redirigiendo...", "info");
                setTimeout(() => {
                    window.location.href = `/sala/${codigo}`;
                }, 1500);
                return;
            }

            if (res.status === 403) {
                showWhavieAlert("¡Sala Llena!", errorText, "warning");
                return;
            }

            showWhavieAlert("¡Ups!", errorText, "error");
            return;
        }

        // Guarda el token del invitado en sessionStorage
        const tokenInvitado = await res.text();
        if (tokenInvitado && tokenInvitado.trim() !== "") {
            sessionStorage.setItem('tokenInvitado', tokenInvitado);
        }

        // Redirige a la sala
        window.location.href = `/sala/${codigo}`;
    } catch (err) {
        console.error(err);
        showWhavieAlert("¡Ups!", "Código incorrecto o sala no disponible.", "error");
    }
}

// ===========================
// EVENT LISTENERS INICIALES
// ===========================
const inputNombreInvitado = document.getElementById('guestName');

if (inputNombreInvitado) {
    // Permite entrar como invitado presionando Enter
    const entrarComoInvitadoThrottled = throttleClick(entrarComoInvitado, 1500);
    inputNombreInvitado.addEventListener('keydown', (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            entrarComoInvitadoThrottled();
        }
    });
}

// Inicialización al cargar la página
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const codigoTraido = urlParams.get('codigo');

    // Si viene con código en URL, lo pre-rellena
    if (codigoTraido && codigoTraido.length > 0) {
        const limpio = codigoTraido.trim().toUpperCase().replace(/[^A-Z0-9]/g, '');

        // Distribuye el código en los inputs y los bloquea
        for (let i = 0; i < inputs.length; i++) {
            inputs[i].value = limpio[i] || '';
            inputs[i].readOnly = true;
        }

        window.currentRoomCode = limpio;

        // Si está logueado, verifica inmediatamente
        // Si no, muestra el step 2 para ingresar nombre
        if (isUserLogueado()) {
            verificarCodigoOTP();
        } else {
            showStep(2);
            if (inputNombreInvitado) inputNombreInvitado.focus();
        }
    }
});