/* ===========================
   VOTACION.JS - VOTACIÓN EN GRUPO (MODO MULTIPLAYER)
   Maneja el swipe&vote colaborativo entre múltiples usuarios
   =========================== */

// ===========================
// VARIABLES GLOBALES
// ===========================
const salaCodigo = SALA_CODIGO;
let stompClient = null;

// Variables de control del drag&drop
let stack;
let isDragging = false;
let startX = 0;
let startY = 0;
let currentX = 0;
let currentCard = null;
let cartaEsperandoRemover = false;  // Flag para evitar enviar múltiples votos
let hasMoved = false;

// ===========================
// INICIALIZACIÓN
// ===========================
document.addEventListener('DOMContentLoaded', () => {
    conectarWebSocket();

    stack = document.querySelector('.card-stack');

    // Eventos de drag&drop
    window.addEventListener('mousemove', drag);
    window.addEventListener('touchmove', drag);
    window.addEventListener('mouseup', stopDragging);
    window.addEventListener('touchend', stopDragging);

    prepararCartaTope();
});

// ===========================
// PREPARACIÓN DE CARTAS
// ===========================

/**
 * Prepara la carta del tope para ser interactuada
 * Si no hay más cartas, muestra mensaje de espera
 */
function prepararCartaTope() {
    const cards = document.querySelectorAll('.movie-card');
    if (cards.length === 0) {
        // Sin más películas para votar
        stack.innerHTML = "<div class='no-more'><h2>¡No hay más películas!</h2><p>Esperando a que el resto termine de votar...</p></div>";
        return;
    }

    // Muestra instrucciones de swipe
    const swipeInstructions = document.querySelector('.swipe-instructions');
    if (swipeInstructions) {
        swipeInstructions.style.display = 'block';
    }

    currentCard = cards[0];
    currentCard.addEventListener('mousedown', startDragging);
    currentCard.addEventListener('touchstart', startDragging);
}

// ===========================
// DRAG & DROP - VOTACIÓN
// ===========================

/**
 * Inicia el movimiento de la carta
 */
function startDragging(e) {
    isDragging = true;
    hasMoved = false;
    startX = e.type === 'touchstart' ? e.touches[0].clientX : e.clientX;
    startY = e.type === 'touchstart' ? e.touches[0].clientY : e.clientY;
    currentCard.style.transition = 'none';
    currentCard.style.cursor = 'grabbing';
}

/**
 * Maneja el movimiento de la carta durante el drag
 * Muestra los sellos de LIKE/NOPE según dirección
 */
function drag(e) {
    if (!isDragging || !currentCard) return;

    const x = e.type === 'touchmove' ? e.touches[0].clientX : e.clientX;
    const y = e.type === 'touchmove' ? e.touches[0].clientY : e.clientY;
    const deltaX = x - startX;
    const deltaY = y - startY;
    const absDeltaX = Math.abs(deltaX);
    const absDeltaY = Math.abs(deltaY);

    // Si el movimiento es vertical, permite scroll normal
    if (absDeltaY > absDeltaX && absDeltaY > 10) {
        isDragging = false;
        currentCard.style.cursor = 'grab';
        return;
    }

    // No hace dragging hasta que se mueva lo suficiente
    if (absDeltaX < 10 && !hasMoved) {
        return;
    }

    hasMoved = true;
    currentX = deltaX;
    const rotation = currentX / 15;

    currentCard.style.transform = `translateX(${currentX}px) rotate(${rotation}deg)`;

    // Muestra sellos (LIKE a la derecha, NOPE a la izquierda)
    const likeStamp = currentCard.querySelector('.like-stamp');
    const nopeStamp = currentCard.querySelector('.nope-stamp');
    const opacity = Math.min(Math.abs(currentX) / 100, 1);

    if (currentX > 50) {
        likeStamp.style.opacity = opacity;
        nopeStamp.style.opacity = 0;
    } else if (currentX < -50) {
        nopeStamp.style.opacity = opacity;
        likeStamp.style.opacity = 0;
    } else {
        likeStamp.style.opacity = 0;
        nopeStamp.style.opacity = 0;
    }
}

/**
 * Termina el drag y registra el voto si fue suficiente
 */
function stopDragging() {
    if (!isDragging || !currentCard) return;
    isDragging = false;

    // Threshold de 120px para confirmar el voto
    if (Math.abs(currentX) > 120) {
        const direction = currentX > 0 ? 1 : -1;  // 1 = LIKE, -1 = NOPE
        const finalX = direction * 1000;
        const peliculaId = currentCard.getAttribute('data-id');
        const cardToRemove = currentCard;
        currentCard = null;
        currentX = 0;

        // Anima la salida de la carta
        cardToRemove.style.transition = 'transform 0.5s ease-in, opacity 0.4s';
        cardToRemove.style.transform = `translateX(${finalX}px) rotate(${direction * 40}deg)`;
        cardToRemove.style.opacity = '0';

        // Al terminar la animación
        cardToRemove.addEventListener('transitionend', () => {
            cardToRemove.remove();
            cartaEsperandoRemover = false;
        }, {once: true});
        cartaEsperandoRemover = true;

        // Envía el voto al servidor
        enviarVoto(peliculaId, direction > 0 ? "LIKE" : "DISLIKE");

        // Oculta otras cartas y el stack
        document.querySelectorAll('.movie-card').forEach(c => {
            if (c !== cardToRemove) c.style.visibility = 'hidden';
        });
        stack.style.visibility = 'hidden';

        // Oculta instrucciones
        const swipeInstructions = document.querySelector('.swipe-instructions');
        if (swipeInstructions) {
            swipeInstructions.style.display = 'none';
        }

        // Muestra modal de espera
        Swal.fire({
            title: '¡Voto registrado!',
            html: 'Esperando a los demás jugadores...',
            allowOutsideClick: false,
            allowEscapeKey: false,
            showConfirmButton: false,
            customClass: {popup: "rounded-popup"},
            didOpen: () => {
                Swal.showLoading();
            }
        });

    } else {
        // Devuelve la carta a su posición
        currentCard.style.transition = 'transform 0.3s ease';
        currentCard.style.transform = 'translateX(0) rotate(0)';
        currentCard.querySelector('.like-stamp').style.opacity = 0;
        currentCard.querySelector('.nope-stamp').style.opacity = 0;
    }
}

// ===========================
// EFECTOS Y CELEBRACIÓN
// ===========================

/**
 * Lanza efecto de confeti para celebrar un match
 */
function lanzarCelebracion() {
    confetti({
        particleCount: 150,
        spread: 70,
        origin: {x: 0, y: 0.6}
    });
    confetti({
        particleCount: 150,
        spread: 70,
        origin: {x: 1, y: 0.6}
    });
}

// ===========================
// WEBSOCKET - COMUNICACIÓN EN TIEMPO REAL
// ===========================

/**
 * Conecta al WebSocket y se subscribe a los eventos de la sala
 * Maneja:
 * - Match: todos votaron SI en la misma película
 * - Mayoría: ganadora por voto mayoritario
 * - Nueva ronda: siguiente película
 * - Sala cerrada: fin de la votación
 */
function conectarWebSocket() {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({'X-CSRF-TOKEN': csrfToken}, function (frame) {
        /**
         * Se subscribe a los mensajes de la sala
         */
        stompClient.subscribe('/topic/sala/' + salaCodigo, function (mensajeRecibido) {
            const datos = JSON.parse(mensajeRecibido.body);
            if (datos.accion === 'SALA_CERRADA') {
                // La sala fue cerrada, fin de la votación
                Swal.fire({
                    title: "Sala finalizada",
                    text: datos.mensaje,
                    icon: "warning",
                    confirmButtonText: "Volver al inicio",
                    customClass: {
                        popup: "rounded-popup",
                        confirmButton: 'rounded-button',
                    },
                    allowOutsideClick: false
                }).then(() => {
                    window.location.href = '/';
                });

            } else if (datos.accion === 'HAY_MATCH') {
                // ¡MATCH! Todos votaron SI en la misma película
                stack.style.visibility = 'hidden';

                // Oculta instrucciones
                const swipeInstructions = document.querySelector('.swipe-instructions');
                if (swipeInstructions) {
                    swipeInstructions.style.display = 'none';
                }
                
                Swal.close();
                lanzarCelebracion();
                Swal.fire({
                    title: "¡TENEMOS MATCH!",
                    text: "¡A todos les gustó esta película!",
                    icon: "success",
                    confirmButtonText: "Ver pelicula",
                    allowOutsideClick: true,
                    customClass: {
                        popup: "rounded-popup",
                        confirmButton: 'rounded-button',
                    }
                }).then(() => {
                    // Redirige a página de resultado con tipo 'match'
                    window.location.href = `/resultado/sala/${salaCodigo}/${datos.tmdbId}/match`;
                });

            } else if (datos.accion === 'NUEVA_RONDA') {
                // Nueva ronda: siguiente película a votar
                const intentarPrepararCarta = () => {
                    const cartaPendiente = document.querySelector('.movie-card[style*="opacity: 0"], .movie-card[style*="opacity:0"]');
                    if (cartaPendiente) {
                        setTimeout(intentarPrepararCarta, 100);
                    } else {
                        Swal.close();
                        document.querySelectorAll('.movie-card').forEach(c => {
                            c.style.visibility = 'visible';
                        });
                        stack.style.visibility = 'visible';
                        prepararCartaTope();
                    }
                };
                setTimeout(intentarPrepararCarta, 100);
            } else if (datos.accion === 'GANADORA_MAYORIA') {
                // Fin de la votación - ganadora por mayoría
                stack.style.visibility = 'hidden';

                // Oculta instrucciones
                const swipeInstructions = document.querySelector('.swipe-instructions');
                if (swipeInstructions) {
                    swipeInstructions.style.display = 'none';
                }
                
                Swal.close();
                Swal.fire({
                    title: "Fin del juego",
                    text: "No hubo match. Pero la mayoría votó por esta película.",
                    icon: "info",
                    confirmButtonText: "Ver película",
                    allowOutsideClick: false,
                    customClass: {
                        popup: "rounded-popup",
                        confirmButton: 'rounded-button',
                    }
                }).then(() => {
                    // Redirige a página de resultado con tipo 'mayoria'
                    window.location.href = `/resultado/sala/${salaCodigo}/${datos.tmdbId}/mayoria`;
                });
            }
        });

        // Notifica presencia en la sala
        stompClient.send(`/app/sala/${salaCodigo}/presencia`, {}, JSON.stringify({
            nombre: USUARIO_NOMBRE
        }));
    });
}

/**
 * Envía el voto del usuario al servidor a través de WebSocket
 * @param {number} peliculaId - ID de TMDB de la película votada
 * @param {string} tipoVoto - Tipo de voto: "LIKE" o "DISLIKE"
 */
function enviarVoto(peliculaId, tipoVoto) {
    if (stompClient && stompClient.connected) {
        const payload = {
            peliculaId: peliculaId,
            voto: tipoVoto
        };
        stompClient.send(`/app/sala/${salaCodigo}/votar`, {}, JSON.stringify(payload));
    }
}