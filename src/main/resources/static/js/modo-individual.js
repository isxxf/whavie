/* ===========================
   MODO-INDIVIDUAL.JS - DESCUBRIMIENTO DE PELÍCULAS EN SOLITARIO
   Permite a los usuarios descubrir películas individuales con drag&drop
   =========================== */

// ===========================
// VARIABLES GLOBALES
// ===========================
let stack;                  // Contenedor de las cartas
let isDragging = false;     // Indica si se está moviendo una carta
let startX = 0;             // Posición inicial X
let startY = 0;             // Posición inicial Y
let currentX = 0;           // Posición actual X
let currentCard = null;     // Carta actualmente
let guardadoTimeout = null; // Timeout para debounce de guardado
let hasMoved = false;       // Flag para saber si se movió la carta

document.addEventListener('DOMContentLoaded', () => {
    stack = document.querySelector('#cardStack');

    // Eventos globales de mouse/touch
    window.addEventListener('mousemove', drag);
    window.addEventListener('touchmove', drag);
    window.addEventListener('mouseup', stopDragging);
    window.addEventListener('touchend', stopDragging);

    cargarPreferencias();

    // ===========================
    // FILTROS - GÉNEROS, PLATAFORMAS, IDIOMAS, ETC
    // ===========================
    document.querySelectorAll('.tag').forEach(tag => {
        tag.addEventListener('click', () => {
            // Clasificación es exclusiva (solo una opción)
            if (tag.getAttribute('data-tipo') === 'clasificacion') {
                document.querySelectorAll('.tag[data-tipo="clasificacion"]').forEach(t => {
                    t.classList.remove('active');
                });
            }
            tag.classList.toggle('active');
            guardarPreferenciasDebounced();
        });
    });

    // Configura inputs de búsqueda para actores y directores
    configurarInputBusqueda('input-actor', 'actores-container', 'actor');
    configurarInputBusqueda('input-director', 'directores-container', 'director');

    // Botón para buscar películas
    document.getElementById('btn-descubrir').addEventListener('click', throttleClick(buscarPeliculas, 2000));

    // Botón para volver a preferencias
    document.getElementById('btn-volver-preferencias').addEventListener('click', () => {
        document.getElementById('cards-wrapper').style.display = 'none';
        document.getElementById('preferencias-panel').style.display = 'block';
    });

    // Guarda preferencias al salir de la página
    window.addEventListener('beforeunload', () => {
        clearTimeout(guardadoTimeout);
        guardarPreferencias();
    });
});

// ===========================
// BUSQUEDA DE ACTORES/DIRECTORES
// ===========================

/**
 * Configura el input de búsqueda con autocomplete
 * @param {string} inputId - ID del input
 * @param {string} containerId - ID del contenedor de tags removibles
 * @param {string} tipo - Tipo de búsqueda ('actor' o 'director')
 */
function configurarInputBusqueda(inputId, containerId, tipo) {
    const input = document.getElementById(inputId);
    const container = document.getElementById(containerId);
    if (!input || !container) return;

    const dropdown = document.createElement('div');
    dropdown.className = 'autocomplete-dropdown';
    dropdown.style.display = 'none';
    input.parentNode.insertBefore(dropdown, input.nextSibling);

    let debounceTimer;

    /**
     * Evento de input - busca personas con debounce
     */
    input.addEventListener('input', function () {
        clearTimeout(debounceTimer);
        const query = this.value.trim();

        if (query.length < 2) {
            dropdown.style.display = 'none';
            return;
        }

        // Debounce: espera 400ms antes de hacer la búsqueda
        debounceTimer = setTimeout(() => {
            fetch(`/api/preferencias/buscar-persona?query=${encodeURIComponent(query)}&tipo=${tipo}`)
                .then(res => res.json())
                .then(personas => {
                    dropdown.innerHTML = '';
                    if (personas.length === 0) {
                        dropdown.style.display = 'none';
                        return;
                    }
                    // Muestra los resultados
                    personas.forEach(persona => {
                        const item = document.createElement('div');
                        item.className = 'autocomplete-item';
                        item.textContent = persona.name;
                        item.addEventListener('click', () => {
                            agregarTagRemovible(container, persona.name, tipo, persona.id);
                            input.value = '';
                            dropdown.style.display = 'none';
                            guardarPreferenciasDebounced();
                        });
                        dropdown.appendChild(item);
                    });
                    dropdown.style.display = 'block';
                })
                .catch(err => console.error('Error buscando persona:', err));
        }, 400);
    });

    // Cierra dropdown cuando se hace click fuera
    document.addEventListener('click', (e) => {
        if (!input.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

/**
 * Agrega un tag removible (con botón X) para actor/director
 * @param {HTMLElement} container - Contenedor donde agregar el tag
 * @param {string} texto - Nombre de la persona
 * @param {string} tipo - Tipo ('actor' o 'director')
 * @param {number} id - ID de la persona en la BD
 */
function agregarTagRemovible(container, texto, tipo, id) {
    const tag = document.createElement('div');
    tag.className = 'tag-removable';
    tag.setAttribute('data-tipo', tipo);
    tag.setAttribute('data-id', id);

    const label = document.createElement('span');
    label.textContent = texto;

    const close = document.createElement('span');
    close.className = 'tag-removable-close';
    close.textContent = '×';
    close.addEventListener('click', () => {
        tag.remove();
        guardarPreferenciasDebounced();
    });

    tag.appendChild(label);
    tag.appendChild(close);
    container.appendChild(tag);
}

// ===========================
// PREFERENCIAS
// ===========================

/**
 * Carga las preferencias guardadas del usuario desde variables globales
 * Activa los tags correspondientes
 */
function cargarPreferencias() {
    if (!PREFERENCIAS) return;

    // Limpia tags activos anteriores
    document.querySelectorAll('.tag.active').forEach(tag => {
        tag.classList.remove('active');
    });

    // Géneros favoritos
    if (PREFERENCIAS.generosFavoritos) {
        PREFERENCIAS.generosFavoritos.forEach(id => {
            const tag = document.querySelector(`.tag[data-tipo="genero"][data-id="${id}"]`);
            if (tag) tag.classList.add('active');
        });
    }

    // Plataformas
    if (PREFERENCIAS.plataformasIds) {
        PREFERENCIAS.plataformasIds.forEach(id => {
            const tag = document.querySelector(`.tag[data-tipo="plataforma"][data-id="${id}"]`);
            if (tag) tag.classList.add('active');
        });
    }

    // Idiomas
    if (PREFERENCIAS.idiomasPreferidos) {
        PREFERENCIAS.idiomasPreferidos.forEach(id => {
            const tag = document.querySelector(`.tag[data-tipo="idioma"][data-id="${id}"]`);
            if (tag) tag.classList.add('active');
        });
    }

    // Clasificación de edad
    if (PREFERENCIAS.clasificacionEdad) {
        const tag = document.querySelector(`.tag[data-tipo="clasificacion"][data-id="${PREFERENCIAS.clasificacionEdad}"]`);
        if (tag) tag.classList.add('active');
    }

    // Época
    if (PREFERENCIAS.epocasIds && EPOCAS_DATA) {
        PREFERENCIAS.epocasIds.forEach(id => {
            const tag = document.querySelector(`.tag[data-tipo="epoca"][data-id="${id}"]`);
            if (tag) tag.classList.add('active');
        });
    } else if (PREFERENCIAS.fechaGte && EPOCAS_DATA) {
        EPOCAS_DATA.forEach(e => {
            if (e.gte === PREFERENCIAS.fechaGte) {
                const tag = document.querySelector(`.tag[data-tipo="epoca"][data-id="${e.id}"]`);
                if (tag) tag.classList.add('active');
            }
        });
    }
}

/**
 * Construye el DTO con las preferencias seleccionadas
 * @returns {Object} Objeto con las preferencias configuradas
 */
function construirDTO() {
    const dto = {
        generosFavoritos: [],
        plataformasIds: [],
        idiomasPreferidos: [],
        clasificacionEdad: null,
        fechaGte: null,
        fechaLte: null,
        epocasIds: [],
        actoresFavoritos: [],
        directoresFavoritos: []
    };

    // Recopila todos los tags activos
    document.querySelectorAll('.tag.active').forEach(tag => {
        const tipo = tag.getAttribute('data-tipo');
        const id = tag.getAttribute('data-id');

        if (tipo === 'genero') dto.generosFavoritos.push(parseInt(id));
        else if (tipo === 'plataforma') dto.plataformasIds.push(parseInt(id));
        else if (tipo === 'idioma') dto.idiomasPreferidos.push(id);
        else if (tipo === 'clasificacion') dto.clasificacionEdad = id;
        else if (tipo === 'epoca') {
            dto.epocasIds.push(id);
        }
    });

    if (dto.epocasIds.length === 1 && EPOCAS_DATA) {
        const epoca = EPOCAS_DATA.find(e => e.id === dto.epocasIds[0]);
        if (epoca) {
            dto.fechaGte = epoca.gte;
            dto.fechaLte = epoca.lte;
        }
    }

    // Actores y directores removibles
    document.querySelectorAll('#actores-container .tag-removable').forEach(tag => {
        dto.actoresFavoritos.push(parseInt(tag.getAttribute('data-id')));
    });
    document.querySelectorAll('#directores-container .tag-removable').forEach(tag => {
        dto.directoresFavoritos.push(parseInt(tag.getAttribute('data-id')));
    });

    return dto;
}

/**
 * Guarda las preferencias con debounce (espera 800ms sin cambios)
 * Evita hacer muchas peticiones al servidor
 */
function guardarPreferenciasDebounced() {
    clearTimeout(guardadoTimeout);
    guardadoTimeout = setTimeout(guardarPreferencias, 800);
}

/**
 * Envía las preferencias al servidor
 * Muestra badge de "Guardado" durante 2 segundos
 */
function guardarPreferencias() {
    const dto = construirDTO();
    const badge = document.getElementById('guardado-badge');

    fetch('/api/preferencias', {
        method: 'POST',
        headers: {
            ...getCsrfHeaders(),
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(dto)
    })
        .then(response => {
            if (response.ok && badge) {
                badge.classList.add('visible');
                setTimeout(() => badge.classList.remove('visible'), 2000);
            }
        })
        .catch(err => console.error('Error guardando preferencias:', err));
}

// ===========================
// BUSCAR PELICULAS
// ===========================

/**
 * Busca películas según las preferencias seleccionadas
 * Muestra loading mientras carga y luego muestra las cartas
 */
function buscarPeliculas() {
    const dto = construirDTO();

    // Oculta instrucciones de swipe
    const swipeInstructions = document.querySelector('.swipe-instructions');
    if (swipeInstructions) {
        swipeInstructions.style.display = 'none';
    }

    Swal.fire({
        title: '¡Preparando la cartelera!',
        html: 'Estamos eligiendo las mejores películas...',
        allowOutsideClick: false,
        showConfirmButton: false,
        customClass: {popup: 'rounded-popup'},
        didOpen: () => Swal.showLoading()
    });

    fetch('/api/preferencias/peliculas', {
        method: 'POST',
        headers: {
            ...getCsrfHeaders(),
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(dto)
    })
        .then(res => res.json())
        .then(peliculas => {
            Swal.close();
            document.getElementById('preferencias-panel').style.display = 'none';
            document.getElementById('cards-wrapper').style.display = 'flex';
            cargarCartas(peliculas);
        })
        .catch(err => {
            Swal.close();
            console.error('Error buscando películas:', err);
            Swal.fire({
                title: '¡Ups!',
                text: 'No pudimos encontrar películas. Intentá de nuevo.',
                icon: 'error',
                confirmButtonText: 'Entendido',
                customClass: {popup: 'rounded-popup', confirmButton: 'rounded-button'}
            });
        });
}

// ===========================
// CARTAS DE PELICULAS
// ===========================

/**
 * Carga las cartas de películas y las coloca en el stack
 * @param {Array} peliculas - Array de películas a mostrar
 */
function cargarCartas(peliculas) {
    document.querySelectorAll('.movie-card').forEach(c => c.remove());

    // Si no hay películas, vuelve a preferencias
    if (!peliculas || peliculas.length === 0) {
        document.getElementById('cards-wrapper').style.display = 'none';
        document.getElementById('preferencias-panel').style.display = 'block';
        Swal.fire({
            title: 'Sin resultados',
            text: 'No encontramos películas con esos filtros. Probá con otras preferencias.',
            icon: 'info',
            confirmButtonText: 'Entendido',
            customClass: {popup: 'rounded-popup', confirmButton: 'rounded-button'}
        });
        return;
    }

    // Crea las cartas en orden inverso para que el stack esté correcto
    peliculas.forEach((peli, index) => {
        const card = crearCarta(peli, peliculas.length - index);
        stack.insertBefore(card, stack.firstChild);
    });

    // Muestra instrucciones de swipe
    const swipeInstructions = document.querySelector('.swipe-instructions');
    if (swipeInstructions) {
        swipeInstructions.style.display = 'block';
    }

    prepararCartaTope();
}

/**
 * Crea el elemento HTML de una carta de película
 * @param {Object} peli - Datos de la película
 * @param {number} zIndex - Z-index de la carta
 * @returns {HTMLElement} Elemento de la carta
 */
function crearCarta(peli, zIndex) {
    const card = document.createElement('div');
    card.className = 'movie-card';
    card.setAttribute('data-id', peli.tmdbId);
    card.style.zIndex = zIndex;

    // Título
    const titulo = document.createElement('h1');
    titulo.className = 'movie-title';
    titulo.textContent = peli.titulo;

    // Contenedor del poster
    const posterContainer = document.createElement('div');
    posterContainer.className = 'poster-container';
    const posterWrapper = document.createElement('div');
    posterWrapper.className = 'poster-wrapper';

    const img = document.createElement('img');
    img.setAttribute('src', peli.posterPath);
    img.className = 'movie-poster';
    img.alt = 'Póster';
    img.addEventListener('error', () => {
        // Imagen por defecto si falla la carga
        img.src = 'https://media3.giphy.com/media/6uGhT1O4sxpi8/giphy.gif';
    });

    // Géneros (overlays en la imagen)
    const tagsOverlay = document.createElement('div');
    tagsOverlay.className = 'movie-tags-overlay';
    if (peli.generos) {
        peli.generos.forEach(g => {
            const pill = document.createElement('span');
            pill.className = 'genre-pill';
            pill.textContent = g;
            tagsOverlay.appendChild(pill);
        });
    }

    // Logos de plataformas
    if (peli.plataformasLogos && peli.plataformasLogos.length > 0) {
        const platformBadges = document.createElement('div');
        platformBadges.className = 'platform-badges';
        peli.plataformasLogos.forEach((logo, i) => {
            const logoImg = document.createElement('img');
            logoImg.setAttribute('src', `https://image.tmdb.org/t/p/w45${logo}`);
            logoImg.alt = peli.plataformas ? peli.plataformas[i] : '';
            logoImg.title = peli.plataformas ? peli.plataformas[i] : '';
            logoImg.className = 'platform-logo';
            logoImg.addEventListener('error', () => logoImg.style.display = 'none');
            platformBadges.appendChild(logoImg);
        });
        posterWrapper.appendChild(platformBadges);
    }

    posterWrapper.appendChild(img);
    posterWrapper.appendChild(tagsOverlay);
    posterContainer.appendChild(posterWrapper);

    // Contenido de información
    const infoContent = document.createElement('div');
    infoContent.className = 'movie-info-content';

    // Sinopsis
    const sinopsis = document.createElement('p');
    sinopsis.className = 'movie-sinopsis';
    if (peli.sinopsis) {
        sinopsis.textContent = peli.sinopsis;
    } else {
        sinopsis.style.fontStyle = 'italic';
        sinopsis.style.color = '#94a3b8';
        sinopsis.textContent = 'No se encontró un resumen.';
    }

    // Detalles (director, cast, idioma, etc)
    const detallesList = document.createElement('ul');
    detallesList.className = 'movie-details-list';

    const detalles = [
        {
            label: 'Director',
            valor: peli.directores && peli.directores.length ? peli.directores.join(', ') : 'No disponible'
        },
        {label: 'Cast', valor: peli.cast && peli.cast.length ? peli.cast.join(', ') : 'No disponible'},
        {label: 'Idioma original', valor: peli.idiomaOriginal || 'No disponible'},
        {label: 'Fecha de estreno', valor: peli.fechaEstreno || 'No disponible'}
    ];

    detalles.forEach(d => {
        const li = document.createElement('li');
        const strong = document.createElement('strong');
        strong.textContent = d.label + ': ';
        li.appendChild(strong);
        li.appendChild(document.createTextNode(d.valor));
        detallesList.appendChild(li);
    });

    infoContent.appendChild(sinopsis);
    infoContent.appendChild(detallesList);

    // Sellos de LIKE y NOPE (aparecen al mover)
    const likeStamp = document.createElement('div');
    likeStamp.className = 'stamp like-stamp';
    likeStamp.textContent = 'LIKE';

    const nopeStamp = document.createElement('div');
    nopeStamp.className = 'stamp nope-stamp';
    nopeStamp.textContent = 'NOPE';

    // Input escondido para guardar el ID
    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.className = 'movie-id';
    hiddenInput.value = peli.tmdbId;

    // Estructura de la carta
    card.appendChild(hiddenInput);
    card.appendChild(titulo);
    card.appendChild(posterContainer);
    card.appendChild(infoContent);
    card.appendChild(likeStamp);
    card.appendChild(nopeStamp);

    return card;
}

/**
 * Prepara la carta del tope del stack para ser dragged
 * Se ejecuta cuando carga nuevas cartas o después de remover una
 */
function prepararCartaTope() {
    const cards = document.querySelectorAll('.movie-card');
    if (cards.length === 0) {
        // No hay más cartas
        document.getElementById('cards-wrapper').style.display = 'none';
        document.getElementById('preferencias-panel').style.display = 'block';
        Swal.fire({
            title: '¡Terminaste!',
            text: 'Revisaste todas las películas. Buscá de nuevo para ver más.',
            icon: 'info',
            confirmButtonText: 'Buscar más',
            customClass: {popup: 'rounded-popup', confirmButton: 'rounded-button'}
        });
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
// DRAG & DROP
// ===========================

/**
 * Inicia el dragging de la carta
 * Guarda la posición inicial y prepara la animación
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
 * Maneja el movimiento de la carta
 * Aplica rotación y opacidad a los sellos (LIKE/NOPE)
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

    // Muestra sellos según dirección
    const likeStamp = currentCard.querySelector('.like-stamp');
    const nopeStamp = currentCard.querySelector('.nope-stamp');
    const opacity = Math.min(Math.abs(currentX) / 100, 1);

    if (currentX > 50) {
        // Derecha = LIKE
        likeStamp.style.opacity = opacity;
        nopeStamp.style.opacity = 0;
    } else if (currentX < -50) {
        // Izquierda = NOPE
        nopeStamp.style.opacity = opacity;
        likeStamp.style.opacity = 0;
    } else {
        likeStamp.style.opacity = 0;
        nopeStamp.style.opacity = 0;
    }
}

/**
 * Termina el dragging de la carta
 * Si fue dragged lo suficiente, la elimina y redirige
 * Si no, la devuelve a su posición original
 */
function stopDragging() {
    if (!isDragging || !currentCard) return;
    isDragging = false;

    // Threshold de 120px para confirmar la acción
    if (Math.abs(currentX) > 120) {
        const direction = currentX > 0 ? 1 : -1;  // 1 = derecha (LIKE), -1 = izquierda (NOPE)
        const finalX = direction * 1000;
        const peliculaId = currentCard.getAttribute('data-id');
        const cardToRemove = currentCard;
        currentCard = null;
        currentX = 0;

        // Anima la salida de la carta
        cardToRemove.style.transition = 'transform 0.5s ease-in, opacity 0.4s';
        cardToRemove.style.transform = `translateX(${finalX}px) rotate(${direction * 40}deg)`;
        cardToRemove.style.opacity = '0';

        // Oculta instrucciones
        const swipeInstructions = document.querySelector('.swipe-instructions');
        if (swipeInstructions) {
            swipeInstructions.style.display = 'none';
        }

        // Si es LIKE (derecha), oculta el stack y redirige al resultado
        if (direction > 0) {
            document.querySelectorAll('.movie-card').forEach(c => {
                if (c !== cardToRemove) c.style.visibility = 'hidden';
            });
            stack.style.visibility = 'hidden';
        }

        // Al terminar la animación
        cardToRemove.addEventListener('transitionend', () => {
            cardToRemove.remove();
            if (direction > 0) {
                // LIKE: redirige a página de resultado individual
                window.location.href = `/resultado/individual/${peliculaId}`;
            } else {
                // NOPE: prepara la siguiente carta
                prepararCartaTope();
            }
        }, {once: true});
    } else {
        // Devuelve la carta a su posición original
        currentCard.style.transition = 'transform 0.3s ease';
        currentCard.style.transform = 'translateX(0) rotate(0)';
        currentCard.querySelector('.like-stamp').style.opacity = 0;
        currentCard.querySelector('.nope-stamp').style.opacity = 0;
    }
}
