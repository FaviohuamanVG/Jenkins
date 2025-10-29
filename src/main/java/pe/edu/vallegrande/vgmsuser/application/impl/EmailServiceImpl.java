package pe.edu.vallegrande.vgmsuser.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${server.port:8100}")
    private String serverPort;
    
    @Value("${app.frontend.url:http://localhost:3000/school}")
    private String frontendUrl;

    @Override
    public Mono<Void> sendTemporaryCredentialsEmail(String toEmail, String username, String temporaryPassword, String resetToken) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("Bienvenido - Credenciales Temporales de Acceso");
                
                // Preparar variables para la plantilla
                String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
                Context context = new Context();
                context.setVariable("fullName", username);
                context.setVariable("username", toEmail);
                context.setVariable("temporaryPassword", temporaryPassword);
                context.setVariable("resetUrl", resetUrl);
                context.setVariable("loginUrl", frontendUrl + "/login");
                
                // Procesar la plantilla
                String htmlContent = templateEngine.process("email/temporary-credentials", context);
                helper.setText(htmlContent, true);
                
                mailSender.send(mimeMessage);
                log.info("Email de credenciales temporales enviado a: {}", toEmail);
                
            } catch (MessagingException | RuntimeException e) {
                log.error("Error enviando email a {}: {}", toEmail, e.getMessage());
                throw new RuntimeException("Error enviando email: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendPasswordChangeConfirmationEmail(String toEmail, String username) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("Contraseña Cambiada Exitosamente");
                
                // Preparar variables para la plantilla
                Context context = new Context();
                context.setVariable("fullName", username);
                context.setVariable("changeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                context.setVariable("loginUrl", frontendUrl + "/login");
                
                // Procesar la plantilla
                String htmlContent = templateEngine.process("email/password-change-confirmation", context);
                helper.setText(htmlContent, true);
                
                mailSender.send(mimeMessage);
                log.info("Email de confirmación de cambio de contraseña enviado a: {}", toEmail);
                
            } catch (MessagingException | RuntimeException e) {
                log.error("Error enviando email de confirmación a {}: {}", toEmail, e.getMessage());
                throw new RuntimeException("Error enviando email: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("Restablecer Contraseña");
                
                String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
                
                // Preparar variables para la plantilla
                Context context = new Context();
                context.setVariable("fullName", username);
                context.setVariable("token", resetToken);
                context.setVariable("resetUrl", resetUrl);
                
                // Procesar la plantilla
                String htmlContent = templateEngine.process("email/password-reset", context);
                helper.setText(htmlContent, true);
                
                mailSender.send(mimeMessage);
                log.info("Email de restablecimiento de contraseña enviado a: {}", toEmail);
                
            } catch (MessagingException | RuntimeException e) {
                log.error("Error enviando email de restablecimiento a {}: {}", toEmail, e.getMessage());
                throw new RuntimeException("Error enviando email: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
