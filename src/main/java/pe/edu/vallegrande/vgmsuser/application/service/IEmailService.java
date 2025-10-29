package pe.edu.vallegrande.vgmsuser.application.service;

import reactor.core.publisher.Mono;

public interface IEmailService {
    
    /**
     * Enviar email con credenciales temporales
     */
    Mono<Void> sendTemporaryCredentialsEmail(String toEmail, String username, String temporaryPassword, String resetToken);
    
    /**
     * Enviar email de confirmación de cambio de contraseña
     */
    Mono<Void> sendPasswordChangeConfirmationEmail(String toEmail, String username);
    
    /**
     * Enviar email para restablecer contraseña
     */
    Mono<Void> sendPasswordResetEmail(String toEmail, String username, String resetToken);
}
