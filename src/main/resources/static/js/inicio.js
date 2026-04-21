/* ===========================
   INICIO.JS - PÁGINA DE INICIO
   =========================== */

document.addEventListener('DOMContentLoaded', () => {
    // ===========================
    // VALIDACIÓN DE ERRORES EN URL
    // ===========================
    const urlParams = new URLSearchParams(window.location.search);

    // Si viene con error de sala no existente
    if (urlParams.get('error') === 'sala_inexistente') {
        Swal.fire({
            title: "¡Ups!",
            text: "La sala a la que intentas acceder ya no existe.",
            icon: "error",
            confirmButtonText: "Entendido",
            allowOutsideClick: false,
            customClass: {
                popup: "rounded-popup",
                confirmButton: 'rounded-button',
            }
        });
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // Si viene con error de sala finalizada
    if (urlParams.get('error') === 'sala_finalizada') {
        Swal.fire({
            title: '¡La sala ya finalizó!',
            text: 'La votación de esta sala ya terminó.',
            icon: 'info',
            confirmButtonText: 'Entendido',
            allowOutsideClick: false,
            customClass: {
                popup: 'rounded-popup',
                confirmButton: 'rounded-button',
            }
        });
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    // ===========================
    // EFECTO GLOW DINÁMICO AL SCROLL
    // ===========================
    const glowEl = document.querySelector('.bg-glow');
    const heroSection = document.querySelector('.hero-section');
    const soloSection = document.querySelector('.solo-mode');

    if (glowEl && heroSection && soloSection) {
        /**
         * Mueve el efecto glow según el scroll
         * El glow se desplaza de arriba a abajo conforme se hace scroll
         */
        const moverGlow = () => {
            const scrollY = window.scrollY;
            const maxScroll = document.body.scrollHeight - window.innerHeight;
            const isMobile = window.innerWidth <= 768;
            const ajuste = isMobile ? 1000 : 520;
            const startTop = heroSection.offsetTop + heroSection.offsetHeight / 2 - 350;
            const endTop = soloSection.offsetTop + soloSection.offsetHeight / 2 - ajuste;
            const progress = Math.min(Math.max(scrollY / maxScroll, 0), 1);
            const glowTop = startTop + progress * (endTop - startTop);
            glowEl.style.setProperty('--glow-top', glowTop + 'px');
        };

        window.addEventListener('scroll', moverGlow);
        window.addEventListener('touchmove', moverGlow);
        moverGlow();
    }

    // ===========================
    // BOTÓN LOGOUT
    // ===========================
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
        /**
         * Evento del botón de logout - pide confirmación antes de cerrar sesión
         */
        btnLogout.addEventListener('click', () => {
            Swal.fire({
                title: '¿Cerrar sesión?',
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Salir',
                cancelButtonText: 'Cancelar',
                customClass: {
                    popup: 'rounded-popup',
                    confirmButton: 'rounded-button',
                    cancelButton: 'rounded-button cancel-button',
                }
            }).then((result) => {
                if (result.isConfirmed) {
                    document.getElementById('logoutForm').submit();
                }
            });
        });
    }
});

