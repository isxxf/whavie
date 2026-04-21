/* ===========================
   FORGOT-PASSWORD.JS - MODAL RECUPERAR CONTRASENA
   =========================== */

document.addEventListener('DOMContentLoaded', function () {
    const forgotLink = document.getElementById('forgotLink');
    const forgotModal = document.getElementById('forgotModal');
    const forgotClose = document.getElementById('forgotClose');
    const forgotSubmit = document.getElementById('forgotSubmit');
    const forgotEmail = document.getElementById('forgotEmail');
    const forgotForm = document.getElementById('forgotForm');

    if (!forgotLink || !forgotModal || !forgotClose || !forgotSubmit || !forgotEmail || !forgotForm) return;

    function openModal() {
        forgotModal.classList.add('is-open');
        forgotModal.setAttribute('aria-hidden', 'false');
        forgotEmail.focus();
    }

    function closeModal() {
        forgotModal.classList.remove('is-open');
        forgotModal.setAttribute('aria-hidden', 'true');
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    forgotLink.addEventListener('click', function (e) {
        e.preventDefault();
        openModal();
    });

    forgotClose.addEventListener('click', function () {
        closeModal();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && forgotModal.classList.contains('is-open')) {
            closeModal();
        }
    });

    function handleSubmit(e) {
        if (e) e.preventDefault();

        if (forgotSubmit.disabled) return;

        const email = forgotEmail.value.trim().toLowerCase();
        if (!isValidEmail(email)) {
            showWhavieAlert('Email inválido', 'Revisá el formato del correo.', 'error');
            return;
        }

        const originalLabel = forgotSubmit.textContent;
        forgotSubmit.disabled = true;
        forgotSubmit.textContent = 'Enviando...';

        const body = new URLSearchParams({email}).toString();

        fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: {...getCsrfHeaders(), 'Content-Type': 'application/x-www-form-urlencoded'},
            body: body
        })
            .then(response => {
                if (response.ok) {
                    closeModal();
                    showWhavieAlert('Listo', 'Si el correo existe, te enviaremos un link de recuperación. Revisa la carpeta de Spam', 'success');
                    return;
                }
                return extractErrorMessage(response).then(errorData => {
                    showWhavieAlert('Error', errorData.text || 'No se pudo enviar el link.', 'error');
                });
            })
            .catch(error => {
                showWhavieAlert('Error', error.message || 'No se pudo enviar el link.', 'error');
            })
            .finally(() => {
                forgotSubmit.disabled = false;
                forgotSubmit.textContent = originalLabel;
            });
    }

    forgotSubmit.addEventListener('click', handleSubmit);
    forgotForm.addEventListener('submit', handleSubmit);
});
