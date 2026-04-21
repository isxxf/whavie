/* ===========================
   SALA.JS - SALA DE ESPERA Y SELECCIÓN DE FILTROS
   =========================== */

// ===========================
// INICIALIZACIÓN
// ===========================
document.addEventListener('DOMContentLoaded', () => {

    // ===========================
    // FILTROS - SOLO DISPONIBLES PARA ANFITRIÓN
    // ===========================
    const tags = document.querySelectorAll('.tag');

    /**
     * Evento de clic en tags de filtro
     * Solo el anfitrión puede seleccionar filtros
     * Se envían los filtros seleccionados a través de WebSocket a otros participantes
     */
    tags.forEach(tag => {
        tag.addEventListener('click', function () {
            if (!ES_ANFITRION) return;  // Solo anfitrión puede filtrar
            this.classList.toggle('active');
            const filtrosSeleccionados = [];
            document.querySelectorAll('.tag.active').forEach(t => {
                filtrosSeleccionados.push({
                    tipo: t.getAttribute('data-tipo'),
                    id: t.getAttribute('data-id')
                });
            });
            if (stompClient && stompClient.connected) {
                stompClient.send(`/app/sala/${SALA_CODIGO}/filtros`, {}, JSON.stringify({filtros: filtrosSeleccionados}));
            }
        });
    });

    // ===========================
    // BOTÓN EMPEZAR - SOLO ANFITRIÓN
    // ===========================
    const btnEmpezar = document.getElementById('btn-empezar');
    if (btnEmpezar && ES_ANFITRION) {
        /**
         * Evento del botón "Empezar votación"
         * Valida que haya mínimo 2 participantes
         * Recopila filtros seleccionados y envía to inicio de votación
         */
        btnEmpezar.addEventListener('click', throttleClick((e) => {
            e.preventDefault();

            const numeroParticipantes = document.querySelectorAll('.participant-item').length;

            // Valida cantidad de participantes
            if (numeroParticipantes < 2) {
                Swal.fire({
                    title: "¡Faltan participantes!",
                    text: "Se necesitan al menos 2 personas en la sala para iniciar la votación.",
                    icon: "warning",
                    confirmButtonText: "Entendido",
                    customClass: {
                        popup: "rounded-popup",
                        confirmButton: 'rounded-button',
                    }
                });
                return;
            }

            // Muestra modal de carga
            Swal.fire({
                title: '¡Preparando la cartelera!',
                html: 'Estamos eligiendo las mejores películas...',
                allowOutsideClick: false,
                customClass: {
                    popup: "rounded-popup",
                    confirmButton: 'rounded-button',
                },
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Recopila filtros elegidos
            const filtrosElegidos = [];
            document.querySelectorAll('.tag.active').forEach(t => {
                filtrosElegidos.push({
                    tipo: t.getAttribute('data-tipo'),
                    id: t.getAttribute('data-id')
                });
            });

            if (stompClient && stompClient.connected) {
                // Avisa que va a empezar
                stompClient.send(`/app/sala/${SALA_CODIGO}/avisar-inicio`, {}, JSON.stringify({}));
                // Envía los filtros e inicia la votación
                stompClient.send(`/app/sala/${SALA_CODIGO}/iniciar`, {}, JSON.stringify({filtros: filtrosElegidos}));
            }
        }, 2000));
    }
});

// ===========================
// FUNCIONES GENERALES
// ===========================

/**
 * Copia el código de la sala al portapapeles
 * Cambia el icono de copiar a un check de confirmación
 */
function copiarCodigo() {
    const codigo = document.getElementById('roomCodeText').innerText;

    navigator.clipboard.writeText(codigo).then(() => {
        const icono = document.getElementById('copyIcon');

        // Cambia icono a check
        icono.classList.remove('fa-copy');
        icono.classList.add('fa-check');
        icono.style.color = '#10b981';

        // Vuelve a cambiar el icono después de 2 segundos
        setTimeout(() => {
            icono.classList.remove('fa-check');
            icono.classList.add('fa-copy');
            icono.style.color = '';
        }, 2000);

    }).catch(err => {
        console.error('Error al copiar: ', err);
        showWhavieAlert("¡Ups!", "No pudimos copiar el código.", "error");
    });
}

// ===========================
// WEBSOCKET - COMUNICACIÓN EN TIEMPO REAL
// ===========================

let stompClient = null;
const salaCodigo = SALA_CODIGO;
const nombreParticipante = USUARIO_NOMBRE;
const avatarParticipante = USUARIO_AVATAR;

/**
 * Conecta al servidor WebSocket para recibir actualizaciones en tiempo real
 * Maneja eventos como:
 * - Nuevos usuarios se unen
 * - Usuarios abandonan la sala
 * - Filtros se aplican
 * - Se inicia la votación
 * - Se cierra la sala
 */
function conectarWebSocket() {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({ 'X-CSRF-TOKEN': csrfToken }, function (frame) {

        /**
         * Se subscribe a los mensajes de la sala
         * Procesa diferentes tipos de acciones según la respuesta del servidor
         */
        stompClient.subscribe('/topic/sala/' + salaCodigo, function (mensajeRecibido) {
            const datos = JSON.parse(mensajeRecibido.body);

            if (datos.accion === 'SALA_CERRADA') {
                // La sala fue cerrada por el anfitrión
                Swal.fire({
                    title: "¡Sala Cerrada!",
                    text: datos.mensaje,
                    icon: "info",
                    confirmButtonText: "Salir",
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    customClass: {
                        popup: "rounded-popup",
                        confirmButton: 'rounded-button',
                    }
                }).then((result) => {
                    if (result.isConfirmed) {
                        window.location.href = '/';
                    }
                });
                return;
            }
            else if (datos.accion === 'NUEVO_USUARIO') {
                // Nuevo usuario se unió a la sala
                mostrarNuevoUsuario(datos.nombre, datos.avatar);
                mostrarNotificacion(`${datos.nombre} se unió a la sala`, 'join');

                // Si eres anfitrión, sincroniza los filtros con el nuevo usuario
                if (ES_ANFITRION && stompClient && stompClient.connected) {
                    const filtrosActuales = [];
                    document.querySelectorAll('.tag.active').forEach(t => {
                        filtrosActuales.push({
                            tipo: t.getAttribute('data-tipo'),
                            id: t.getAttribute('data-id')
                        });
                    });
                    stompClient.send(`/app/sala/${SALA_CODIGO}/filtros`, {}, JSON.stringify({ filtros: filtrosActuales }));
                }
            }
            else if (datos.accion === 'PARTICIPANTE_ABANDONO') {
                // Un usuario abandonó la sala
                const items = document.querySelectorAll('.participant-item');
                items.forEach(item => {
                    const nombreSpan = item.querySelector('.name-text');
                    if (nombreSpan && nombreSpan.textContent.trim() === datos.nombre) {
                        item.remove();
                        // Actualiza contador de participantes
                        const spanContador = document.getElementById('contador-online');
                        if (spanContador) {
                            let valorActual = parseInt(spanContador.textContent);
                            spanContador.textContent = valorActual - 1;
                        }
                    }
                });
                mostrarNotificacion(`${datos.nombre} abandonó la sala`, 'leave');
            }
            else if (datos.accion === 'APLICAR_FILTROS') {
                // Se aplican los filtros seleccionados por el anfitrión
                if (ES_ANFITRION) return;  // El anfitrión ya conoce sus filtros
                document.querySelectorAll('.tag').forEach(t => t.classList.remove('active'));
                if (datos.filtros && datos.filtros.length > 0) {
                    datos.filtros.forEach(filtro => {
                        const tagReal = document.querySelector(`.tag[data-tipo="${filtro.tipo}"][data-id="${filtro.id}"]`);
                        if (tagReal) {
                            tagReal.classList.add('active');
                        }
                    });
                }
            }
            else if (datos.accion === 'MOSTRAR_CARGANDO') {
                // El anfitrión está preparando la votación
                if (!ES_ANFITRION) {
                    Swal.fire({
                        title: '¡Preparando la cartelera!',
                        html: 'Estamos eligiendo las mejores películas...',
                        allowOutsideClick: false,
                        showConfirmButton: false,
                        customClass: {
                            popup: "rounded-popup",
                            confirmButton: 'rounded-button',
                        },
                        didOpen: () => { Swal.showLoading(); }
                    });
                }
            }
            else if (datos.accion === 'INICIAR_VOTACION') {
                // Redirige a la página de votación
                setTimeout(() => {
                    window.location.href = `/votacion?codigo=${SALA_CODIGO}`;
                }, 500);
            }
        });

        // Notifica que este usuario entró a la sala
        const datosEntrada = {
            nombre: nombreParticipante,
            avatar: avatarParticipante
        };
        stompClient.send(`/app/sala/${salaCodigo}`, {}, JSON.stringify(datosEntrada));
    });
}

// ===========================
// FUNCIONES DE ACTUALIZACIÓN UI
// ===========================

/**
 * Agrega un nuevo participante a la lista visual
 * @param {string} nombre - Nombre del nuevo participante
 * @param {string} avatar - URL del avatar del participante
 */
function mostrarNuevoUsuario(nombre, avatar) {
    if (!nombre || nombre === 'null' || nombre === 'undefined') {
        console.warn("Llegó un usuario sin datos válidos, ignorando...");
        return;
    }
    const listaUsuarios = document.querySelector('.participant-list');
    if (!listaUsuarios) return;

    // Evita duplicados
    const nombresEnPantalla = Array.from(document.querySelectorAll('.name-text'))
        .map(span => span.textContent.trim());
    if (nombresEnPantalla.includes(nombre)) {
        return;
    }

    // Crea el elemento del participante
    const nuevoLi = document.createElement('li');
    nuevoLi.className = 'participant-item';

    const avatarContainer = document.createElement('div');
    avatarContainer.className = 'avatar-container';

    const img = document.createElement('img');
    img.setAttribute('src', avatar);
    img.className = 'avatar-mini';
    img.alt = 'avatar';

    const nameInfo = document.createElement('div');
    nameInfo.className = 'name-info';

    const nameSpan = document.createElement('span');
    nameSpan.className = 'name-text';
    nameSpan.textContent = nombre;

    // Estructura: [avatar] [nombre]
    avatarContainer.appendChild(img);
    nameInfo.appendChild(nameSpan);
    nuevoLi.appendChild(avatarContainer);
    nuevoLi.appendChild(nameInfo);
    listaUsuarios.appendChild(nuevoLi);

    // Actualiza contador
    const spanContador = document.getElementById('contador-online');
    if (spanContador) {
        let valorActual = parseInt(spanContador.textContent);
        spanContador.textContent = valorActual + 1;
    }
}

/**
 * Muestra una notificación flotante temporal
 * Se muestra en la esquina superior derecha y desaparece después de 3 segundos
 * @param {string} texto - Texto de la notificación
 * @param {string} tipo - Tipo: 'join' (verde) o 'leave' (rojo)
 */
function mostrarNotificacion(texto, tipo = 'join') {
    const container = document.getElementById('notificacion-container');
    const pill = document.createElement('div');

    // Estilos dinámicos según el tipo
    pill.style.cssText = `
        padding: 8px 16px;
        border-radius: 50px;
        font-size: 0.82rem;
        font-weight: 600;
        color: white;
        opacity: 0;
        transform: translateX(20px);
        transition: all 0.3s ease;
        background: ${tipo === 'join'
        ? 'rgba(74, 222, 128, 0.15)'
        : 'rgba(239, 68, 68, 0.15)'};
        border: 1px solid ${tipo === 'join'
        ? 'rgba(74, 222, 128, 0.4)'
        : 'rgba(239, 68, 68, 0.4)'};
        color: ${tipo === 'join' ? '#4ade80' : '#f87171'};
    `;
    pill.textContent = texto;
    container.appendChild(pill);

    // Anima la entrada
    requestAnimationFrame(() => {
        pill.style.opacity = '1';
        pill.style.transform = 'translateX(0)';
    });

    // Auto-desaparece después de 3 segundos
    setTimeout(() => {
        pill.style.opacity = '0';
        pill.style.transform = 'translateX(20px)';
        setTimeout(() => pill.remove(), 300);
    }, 3000);
}

// Conexión WebSocket al cargar
document.addEventListener('DOMContentLoaded', conectarWebSocket);