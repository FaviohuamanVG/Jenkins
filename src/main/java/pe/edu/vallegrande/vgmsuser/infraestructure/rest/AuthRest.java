package pe.edu.vallegrande.vgmsuser.infraestructure.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsuser.application.service.IAuthService;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/reset")
@RequiredArgsConstructor
public class AuthRest {

    private final IAuthService authService;

    @PostMapping("/reset-password")
    public Mono<ResponseEntity<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        log.info("Password reset request for token: {}", request.getToken());
        
        return authService.resetPassword(request.getToken(), request.getNewPassword())
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Error resetting password: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body("Error: " + error.getMessage()));
                });
    }

    @PostMapping("/force-password-change")
    public Mono<ResponseEntity<String>> forcePasswordChange(@Valid @RequestBody ForcePasswordChangeRequest request) {
        log.info("Force password change for keycloakId: {}", request.getKeycloakId());
        
        return authService.forcePasswordChange(
                request.getKeycloakId(), 
                request.getCurrentPassword(), 
                request.getNewPassword())
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Error forcing password change: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body("Error: " + error.getMessage()));
                });
    }

    @PostMapping("/generate-reset-token/{keycloakId}")
    public Mono<ResponseEntity<String>> generateResetToken(@PathVariable String keycloakId) {
        log.info("Generating reset token for keycloakId: {}", keycloakId);
        
        return authService.generatePasswordResetToken(keycloakId)
                .map(token -> ResponseEntity.ok("Token generado y enviado por email"))
                .onErrorResume(error -> {
                    log.error("Error generating reset token: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body("Error: " + error.getMessage()));
                });
    }

    @GetMapping("/password-status/{keycloakId}")
    public Mono<ResponseEntity<PasswordStatusResponse>> getPasswordStatus(@PathVariable String keycloakId) {
        log.info("Checking password status for keycloakId: {}", keycloakId);
        
        return authService.isPasswordTemporary(keycloakId)
                .map(isTemporary -> {
                    PasswordStatusResponse response = new PasswordStatusResponse(
                            isTemporary, 
                            isTemporary ? "Contraseña temporal - cambio requerido" : "Contraseña permanente"
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error checking password status: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(new PasswordStatusResponse(false, "Error: " + error.getMessage())));
                });
    }

    @PostMapping("/request-password-reset")
    public Mono<ResponseEntity<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        log.info("Password reset request for email/username: {}", request.getEmailOrUsername());
        
        return authService.generatePasswordResetTokenByEmail(request.getEmailOrUsername())
                .doOnSuccess(token -> log.info("Reset token generated successfully for: {}", request.getEmailOrUsername()))
                .doOnError(error -> log.error("Error requesting password reset for {}: {}", request.getEmailOrUsername(), error.getMessage(), error))
                .map(token -> {
                    log.info("Returning success response");
                    return ResponseEntity.ok("Si el email o usuario existe, recibirás un enlace para restablecer tu contraseña.");
                })
                .onErrorResume(error -> {
                    log.error("Error caught in onErrorResume: {}", error.getMessage(), error);
                    // Por seguridad, siempre devolver el mismo mensaje
                    return Mono.just(ResponseEntity.ok("Si el email o usuario existe, recibirás un enlace para restablecer tu contraseña."));
                });
    }

    // DTOs internos
    public static class PasswordResetRequest {
        @NotBlank(message = "Token is required")
        private String token;
        
        @NotBlank(message = "New password is required")
        private String newPassword;

        // Getters y setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ForcePasswordChangeRequest {
        @NotBlank(message = "KeycloakId is required")
        private String keycloakId;
        
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        
        @NotBlank(message = "New password is required")
        private String newPassword;

        // Getters y setters
        public String getKeycloakId() { return keycloakId; }
        public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class PasswordStatusResponse {
        private boolean isTemporary;
        private String message;

        public PasswordStatusResponse(boolean isTemporary, String message) {
            this.isTemporary = isTemporary;
            this.message = message;
        }

        // Getters y setters
        public boolean isTemporary() { return isTemporary; }
        public void setTemporary(boolean temporary) { isTemporary = temporary; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class PasswordResetRequestDto {
        @NotBlank(message = "Email o usuario es requerido")
        private String emailOrUsername;

        // Getters y setters
        public String getEmailOrUsername() { return emailOrUsername; }
        public void setEmailOrUsername(String emailOrUsername) { this.emailOrUsername = emailOrUsername; }
    }
}
