package pe.edu.vallegrande.vgmsuser.application.service;

import reactor.core.publisher.Mono;

public interface IAuthService {
    
    /**
     * Generar token para cambio de contraseña
     */
    Mono<String> generatePasswordResetToken(String keycloakId);
    
    /**
     * Validar token y cambiar contraseña
     */
    Mono<String> resetPassword(String token, String newPassword);
    
    /**
     * Forzar cambio de contraseña (primera vez)
     */
    Mono<String> forcePasswordChange(String keycloakId, String currentPassword, String newPassword);
    
    /**
     * Verificar si la contraseña es temporal
     */
    Mono<Boolean> isPasswordTemporary(String keycloakId);
    
    /**
     * Generar token de reset por email/username
     */
    Mono<String> generatePasswordResetTokenByEmail(String emailOrUsername);
}
