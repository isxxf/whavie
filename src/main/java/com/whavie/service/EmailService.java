package com.whavie.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
// @EnableAsync permite que Spring ejecute métodos en "segundo plano".
// Si no lo usamos, el usuario que se registra tendría que esperar mirando la pantalla
// de carga hasta que Google mande el correo para poder ver la página de inicio.
@EnableAsync
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final String appUrl;

    public EmailService(JavaMailSender mailSender, @Value("${app.url}") String appUrl) {
        this.mailSender = mailSender;
        this.appUrl = appUrl;
    }

    @Async
    public void enviarBienvenida(String destinatario, String username) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("whavie.app@gmail.com");
            helper.setTo(destinatario);
            helper.setSubject("¡Bienvenido/a a Whavie! ");
            helper.setText(construirHtml(username, appUrl), true);

            mailSender.send(mensaje);
            log.info("Email de bienvenida enviado a: {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar email de bienvenida a {}: {}", destinatario, e.getMessage(), e);
        }
    }

    @Async
    public void enviarRecuperacionPassword(String destinatario, String username, String token) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("whavie.app@gmail.com");
            helper.setTo(destinatario);
            helper.setSubject("Recupera tu contraseña de Whavie");
            String linkRecuperacion = appUrl + "/reset-password?token=" + token;
            helper.setText(construirHtmlRecuperacion(username, linkRecuperacion), true);

            mailSender.send(mensaje);
        } catch (Exception e) {
            log.error("Error al enviar email de recuperación a {}: {}", destinatario, e.getMessage());
        }
    }

    private String construirHtml(String username, String url) {
        return """
                <div style="background-color:#0d1117;padding:40px;font-family:Arial,Helvetica,sans-serif;color:white;max-width:600px;margin:auto;border-radius:12px;text-align:center;">
                    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 16px;">
                      <tr>
                        <td style="vertical-align:middle;font-size:1.5rem;transform:translateY(-4px);">
                          🎬
                        </td>
                        <td style="vertical-align:middle;padding-left:8px;font-weight:bold;">
                          <span style="color:#ffffff;font-size:2rem;">WHA</span><span style="color:#8b5cf6;font-size:2rem;">VIE</span>
                        </td>
                      </tr>
                    </table>
                    <h2 style="font-size:1.4rem;margin-bottom:16px;color:#ffffff">¡Hola, <span style="color:#8b5cf6;">%s</span>!</h2>
                    <p style="color:#a1a1aa;font-size:1rem;line-height:1.6;">
                        ¡Bienvenido/a a Whavie. Ya podés crear salas, invitar amigos y elegir la película perfecta para ver juntos!
                    </p>
                    <a href="%s"
                       style="display:inline-block;margin-top:30px;padding:12px 28px;
                       background:#8b5cf6;
                       background:linear-gradient(135deg,#8b5cf6,#7c3aed);
                       color:white;border-radius:8px;text-decoration:none;font-weight:600;min-width:200px;">
                        Empezar ahora
                    </a>
                <div style="margin-top:30px;">
                    <a href="https://github.com/isxxf" style="margin:0 8px;text-decoration:none;">
                        <img src="https://cdn-icons-png.flaticon.com/512/733/733553.png" width="24" alt="GitHub" style="display:inline-block;">
                    </a>
                    <a href="https://www.linkedin.com/in/isabella-ferraro-trivelli/" style="margin:0 8px;text-decoration:none;">
                        <img src="https://cdn-icons-png.flaticon.com/512/733/733561.png" width="24" alt="LinkedIn" style="display:inline-block;">
                    </a>
                </div>
                    <p style="margin-top:40px;color:#4b5563;font-size:0.8rem;">© 2026 Whavie</p>
                </div>
                """.formatted(username, url);
    }
    private String construirHtmlRecuperacion(String username, String linkRecuperacion) {
        return """
                <div style="background-color:#0d1117;padding:40px;font-family:Arial,Helvetica,sans-serif;color:white;max-width:600px;margin:auto;border-radius:12px;text-align:center;">
                    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 16px;">
                      <tr>
                        <td style="vertical-align:middle;font-size:1.5rem;transform:translateY(-4px);">
                          🎬
                        </td>
                        <td style="vertical-align:middle;padding-left:8px;font-weight:bold;">
                          <span style="color:#ffffff;font-size:2rem;">WHA</span><span style="color:#8b5cf6;font-size:2rem;">VIE</span>
                        </td>
                      </tr>
                    </table>
                    <h2 style="font-size:1.4rem;margin-bottom:16px;color:#ffffff">¡Hola, <span style="color:#8b5cf6;">%s</span>!</h2>
                    <p style="color:#a1a1aa;font-size:1rem;line-height:1.6;">
                        Si pediste recuperar tu contraseña, hacé click en el botón de abajo.
                    </p>
                    <a href="%s"
                       style="display:inline-block;margin-top:30px;padding:12px 28px;
                       background:#8b5cf6;
                       background:linear-gradient(135deg,#8b5cf6,#7c3aed);
                       color:white;border-radius:8px;text-decoration:none;font-weight:600;min-width:200px;">
                        Recuperar contraseña
                    </a>
                    <div style="margin-top:30px;">
                        <a href="https://github.com/isxxf" style="margin:0 8px;text-decoration:none;">
                            <img src="https://cdn-icons-png.flaticon.com/512/733/733553.png" width="24" alt="GitHub" style="display:inline-block;">
                        </a>
                        <a href="https://www.linkedin.com/in/isabella-ferraro-trivelli/" style="margin:0 8px;text-decoration:none;">
                            <img src="https://cdn-icons-png.flaticon.com/512/733/733561.png" width="24" alt="LinkedIn" style="display:inline-block;">
                        </a>
                    </div>
                    <p style="margin-top:40px;color:#4b5563;font-size:0.8rem;">© 2026 Whavie</p>
                </div>
                """.formatted(username, linkRecuperacion);
    }
}
