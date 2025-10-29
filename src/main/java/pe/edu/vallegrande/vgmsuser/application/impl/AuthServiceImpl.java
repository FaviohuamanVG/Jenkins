package pe.edu.vallegrande.vgmsuser.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsuser.application.service.IAuthService;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IKeycloakService keycloakService;
    private final IEmailService emailService;

    @Override
    public Mono<String> generatePasswordResetToken(String keycloakId) {
        log.info("Generating password reset token for keycloakId: {}", keycloakId);
        
        String resetToken = UUID.randomUUID().toString();
        
        return keycloakService.getUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(keycloakUser -> {
                    // Actualizar passwordResetToken en atributos de Keycloak
                    return keycloakService.updatePasswordResetToken(keycloakId, resetToken)
                            .then(emailService.sendPasswordResetEmail(
                                    keycloakUser.getEmail(), 
                                    keycloakUser.getUsername(), 
                                    resetToken))
                            .thenReturn(resetToken);
                })
                .doOnError(error -> log.error("Error generating reset token: {}", error.getMessage()));
    }

    @Override
    public Mono<String> resetPassword(String token, String newPassword) {
        log.info("Resetting password with token: {} (length: {})", token, token != null ? token.length() : 0);
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> token.equals(user.getPasswordResetToken()))
                .next()
                .doOnNext(user -> log.info("Found user for token: {} - User: {}", token, user.getUsername()))
                .switchIfEmpty(Mono.error(new RuntimeException("Token inválido o expirado")))
                .flatMap(keycloakUser -> {
                    // Cambiar contraseña en Keycloak
                    return keycloakService.changePassword(keycloakUser.getKeycloakId(), newPassword)
                            .then(Mono.defer(() -> {
                                // Actualizar estado de contraseña en Keycloak
                                return keycloakService.updatePasswordStatus(
                                        keycloakUser.getKeycloakId(), 
                                        PasswordStatus.PERMANENT.name(), 
                                        LocalDateTime.now().toString())
                                        .then(keycloakService.updatePasswordResetToken(keycloakUser.getKeycloakId(), null))
                                        .then(emailService.sendPasswordChangeConfirmationEmail(
                                                keycloakUser.getEmail(), 
                                                keycloakUser.getUsername()))
                                        .thenReturn("Contraseña cambiada exitosamente");
                            }));
                })
                .doOnError(error -> log.error("Error resetting password: {}", error.getMessage()));
    }

    @Override
    public Mono<String> forcePasswordChange(String keycloakId, String currentPassword, String newPassword) {
        log.info("Forcing password change for keycloakId: {}", keycloakId);
        
        return keycloakService.getUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(keycloakUser -> {
                    // Verificar que la contraseña actual sea temporal
                    if (keycloakUser.getPasswordStatus() != PasswordStatus.TEMPORARY) {
                        return Mono.error(new RuntimeException("La contraseña ya ha sido cambiada"));
                    }
                    
                    // Cambiar contraseña en Keycloak
                    return keycloakService.changePassword(keycloakId, newPassword)
                            .then(Mono.defer(() -> {
                                // Actualizar estado de contraseña en Keycloak
                                return keycloakService.updatePasswordStatus(
                                        keycloakId, 
                                        PasswordStatus.PERMANENT.name(), 
                                        LocalDateTime.now().toString())
                                        .then(keycloakService.updatePasswordResetToken(keycloakId, null))
                                        .then(emailService.sendPasswordChangeConfirmationEmail(
                                                keycloakUser.getEmail(), 
                                                keycloakUser.getUsername()))
                                        .thenReturn("Contraseña cambiada exitosamente. Tu cuenta está ahora activa.");
                            }));
                })
                .doOnError(error -> log.error("Error forcing password change: {}", error.getMessage()));
    }

    @Override
    public Mono<Boolean> isPasswordTemporary(String keycloakId) {
        log.info("Checking if password is temporary for keycloakId: {}", keycloakId);
        
        return keycloakService.getUserByKeycloakId(keycloakId)
                .map(keycloakUser -> keycloakUser.getPasswordStatus() == PasswordStatus.TEMPORARY)
                .defaultIfEmpty(false)
                .doOnError(error -> log.error("Error checking password status: {}", error.getMessage()));
    }

    @Override
    public Mono<String> generatePasswordResetTokenByEmail(String emailOrUsername) {
        log.info("Generating password reset token for email/username: {}", emailOrUsername);
        
        String resetToken = UUID.randomUUID().toString();
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> emailOrUsername.equalsIgnoreCase(user.getEmail()) || 
                               emailOrUsername.equalsIgnoreCase(user.getUsername()))
                .next()
                .doOnNext(user -> log.info("Found user for email/username: {} - User: {}", emailOrUsername, user.getUsername()))
                .flatMap(keycloakUser -> {
                    // Actualizar passwordResetToken en atributos de Keycloak
                    return keycloakService.updatePasswordResetToken(keycloakUser.getKeycloakId(), resetToken)
                            .then(emailService.sendPasswordResetEmail(
                                    keycloakUser.getEmail(), 
                                    keycloakUser.getUsername(), 
                                    resetToken))
                            .thenReturn(resetToken);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Por seguridad, no revelar si el usuario existe o no
                    log.warn("User not found for email/username: {}", emailOrUsername);
                    return Mono.just("token-not-sent"); // Token dummy para no revelar que el usuario no existe
                }))
                .doOnError(error -> log.error("Error generating reset token by email: {}", error.getMessage()));
    }
}
