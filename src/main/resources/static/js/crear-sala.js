/* ===========================
   CREAR-SALA.JS - CREAR NUEVA SALA
   =========================== */

// Variables globales
let count = 2; // Contador de participantes (mínimo 2)
const countDisplay = document.getElementById('participant-count');

/**
 * Cambia el valor del contador de participantes
 * Mantiene el rango entre 2 y 10 participantes
 * @param {number} delta - Cantidad a sumar o restar (generalmente +1 o -1)
 */
function changeValue(delta) {
    count += delta;
    if (count < 2) count = 2;     // Mínimo 2 participantes
    if (count > 10) count = 10;   // Máximo 10 participantes
    countDisplay.innerText = count;
}

/**
 * Crea una nueva sala enviando petición POST al servidor
 * El servidor retorna el código de la sala y redirige a ella
 */
function confirmarCreacion() {
    const max = countDisplay.innerText;
    const url = `/sala?maxParticipantes=${max}`;

    fetch(url, {
        method: 'POST',
        headers: getCsrfHeaders()
    })
        .then(res => {
            if (!res.ok) {
                return res.text().then(text => {
                    throw new Error(text)
                });
            }
            return res.json();
        })
        .then(data => {
            // Redirige a la sala creada
            window.location.href = "/sala/" + data.codigo;
        })
        .catch(err => {
            console.error("Error creando sala:", err);
            showWhavieAlert('¡Error!', 'No se pudo crear la sala.', 'error');
        });
}