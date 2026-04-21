/* ===========================
   MAIN.JS - FUNCIONES GLOBALES COMPARTIDAS
   =========================== */

/**
 * Throttle para evitar múltiples clics en botones
 * Controla que una función solo se ejecute una vez en un periodo de tiempo
 * @param {Function} fn - Función a ejecutar
 * @param {number} delay - Tiempo de espera en milisegundos (default: 1000ms)
 * @returns {Function} Función throttled
 */
function throttleClick(fn, delay = 1000) {
    let isThrottled = false;
    return function (...args) {
        if (isThrottled) return;
        isThrottled = true;
        fn.apply(this, args);
        setTimeout(() => {
            isThrottled = false;
        }, delay);
    };
}

/**
 * Obtiene los headers CSRF necesarios para las peticiones POST
 * Lee los tokens del meta tags del HTML y los retorna en formato de headers
 * @returns {Object} Objeto con el header CSRF configurado o vacío si no se encuentra
 */
function getCsrfHeaders() {
    const tokenMeta = document.querySelector("meta[name='_csrf']");
    const headerMeta = document.querySelector("meta[name='_csrf_header']");

    if (tokenMeta && headerMeta) {
        return {
            [headerMeta.getAttribute("content")]: tokenMeta.getAttribute("content")
        };
    }
    return {};
}

/**
 * Muestra un alert personalizado usando SweetAlert2
 * Utiliza estilos custom de Whavie
 * @param {string} title - Título del alert
 * @param {string} text - Texto descriptivo
 * @param {string} icon - Tipo de icono (info, success, error, warning, question)
 * @returns {Promise} Promise de SweetAlert2
 */
function showWhavieAlert(title, text, icon = 'info') {
    return Swal.fire({
        title: title,
        text: text,
        icon: icon,
        customClass: {
            popup: "rounded-popup",
            confirmButton: 'rounded-button',
        }
    });
}

/**
 * Verifica si el usuario está autenticado
 * Lee el meta tag "is-authenticated" del HTML
 * @returns {boolean} True si el usuario está logueado, false en caso contrario
 */
function isUserLogueado() {
    return document.querySelector('meta[name="is-authenticated"]')?.content === 'true';
}

/**
 * Extrae el mensaje de error de una respuesta HTTP
 * Intenta parsear diferentes formatos de errores del backend
 * @param {Response} response - Respuesta HTTP del fetch
 * @param {string} fallback - Mensaje por defecto si no se encuentra error
 * @returns {Promise<Object>} Objeto con el texto del error y posibles fieldErrors
 */
async function extractErrorMessage(response, fallback = "Ocurrió un error. Intentá de nuevo.") {
    try {
        const data = await response.json();
        const result = {text: fallback};
        if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
            result.fieldErrors = data.fieldErrors;
            result.text = Object.values(data.fieldErrors)[0] || data.message || fallback;
        } else if (data.message) {
            result.text = data.message;
        } else if (data.error) {
            result.text = data.error;
        }
        return result;
    } catch (e) {
        return {text: fallback};
    }
}

