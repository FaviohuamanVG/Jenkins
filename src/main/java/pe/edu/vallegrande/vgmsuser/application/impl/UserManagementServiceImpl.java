package pe.edu.vallegrande.vgmsuser.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.application.service.IUserManagementService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import pe.edu.vallegrande.vgmsuser.infraestructure.util.KeycloakProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements IUserManagementService {

    private final IKeycloakService keycloakService;
    private final IEmailService emailService;
    private final KeycloakProvider keycloakProvider;

    @Override
    public Mono<KeycloakUserDto> createCompleteUser(User user) {
        log.info("Creating complete user with username: {}", user.getUsername());
        
        // Validar que solo se puedan asignar roles teacher, auxiliary o secretary
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // Si no tiene roles, asignar teacher por defecto
            user.setRoles(Set.of("teacher"));
            log.info("No roles provided, setting default role: teacher");
        } else {
            // Validar que solo contenga roles teacher, auxiliary o secretary
            boolean hasValidRoles = user.getRoles().stream()
                    .allMatch(role -> role.equals("teacher") || 
                                     role.equals("auxiliary") || 
                                     role.equals("secretary"));
            
            if (!hasValidRoles) {
                log.error("Invalid roles provided: {}. Only teacher, auxiliary, secretary allowed", user.getRoles());
                return Mono.error(new RuntimeException("Solo se permiten roles teacher, auxiliary o secretary en este endpoint"));
            }
            log.info("Valid roles provided: {}", user.getRoles());
        }
        
        // Establecer contraseña temporal basada en documentNumber
        String temporaryPassword = user.getDocumentNumber();
        user.setPassword(temporaryPassword);
        log.info("Setting temporary password for user: {}", user.getUsername());
        
        return keycloakService.createUser(user)
                .flatMap(keycloakResponse -> {
                    log.info("Keycloak response received: {}", keycloakResponse);
                    
                    // Extraer el ID de Keycloak de la respuesta
                    String keycloakId = extractKeycloakIdFromResponse(keycloakResponse);
                    log.info("Extracted Keycloak ID: {}", keycloakId);
                    
                    if (keycloakId != null && !keycloakResponse.contains("Error")) {
                        // Generar token de reseteo y guardarlo en atributos de Keycloak
                        String resetToken = java.util.UUID.randomUUID().toString();
                        log.info("Generated reset token for user: {} - Token: {}", user.getUsername(), resetToken);
                        
                        // Actualizar el atributo passwordResetToken en Keycloak
                        return keycloakService.getUserByKeycloakId(keycloakId)
                                .flatMap(keycloakUser -> {
                                    // Actualizar atributos incluyendo el reset token
                                    return keycloakService.updatePasswordResetToken(keycloakId, resetToken)
                                            .then(Mono.defer(() -> {
                                                // Enviar correo con credenciales temporales
                                                log.info("Preparing to send temporary credentials email to: {}", user.getEmail());
                                                String fullName = buildFullName(user.getFirstname(), user.getLastname());
                                                
                                                // Enviar email en background - no bloquea la creación si falla
                                                emailService.sendTemporaryCredentialsEmail(
                                                        user.getEmail(),
                                                        fullName,
                                                        temporaryPassword,
                                                        resetToken
                                                )
                                                .doOnSuccess(emailResult -> log.info("Email enviado exitosamente a: {}", user.getEmail()))
                                                .doOnError(emailError -> log.error("Error enviando email (no crítico): {}", emailError.getMessage()))
                                                .onErrorResume(error -> Mono.empty()) // Ignorar error de email
                                                .subscribe(); // Ejecutar en background
                                                
                                                // Retornar el usuario creado
                                                return keycloakService.getUserByKeycloakId(keycloakId);
                                            }));
                                });
                    } else {
                        log.error("Failed to extract Keycloak ID or error in response: {}", keycloakResponse);
                        return Mono.error(new RuntimeException("Error creating user in Keycloak: " + keycloakResponse));
                    }
                })
                .doOnSuccess(result -> log.info("Complete user creation finished"))
                .doOnError(error -> log.error("Error creating complete user: {}", error.getMessage()));
    }
    
    @Override
    public Flux<KeycloakUserDto> getStaffByInstitution(String institutionId) {
        log.info("Getting staff for institution: {}", institutionId);
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> institutionId.equals(user.getInstitutionId()))
                .filter(this::isStaffUser)
                .doOnError(error -> log.error("Error getting staff by institution: {}", error.getMessage()));
    }
    
    @Override
    public Flux<KeycloakUserDto> getStaffByInstitutionAndRole(String institutionId, String role) {
        log.info("Getting staff by role: {} for institution: {}", role, institutionId);
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> institutionId.equals(user.getInstitutionId()))
                .filter(user -> user.getRoles() != null && user.getRoles().contains(role.toLowerCase()))
                .doOnError(error -> log.error("Error getting staff by institution and role: {}", error.getMessage()));
    }
    
    /**
     * Verifica si un usuario es staff (teacher, auxiliary, secretary)
     */
    private boolean isStaffUser(KeycloakUserDto user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.equals("teacher") || role.equals("auxiliary") || role.equals("secretary"));
    }
    
    /**
     * Actualiza el token de reseteo de contraseña en los atributos de Keycloak
     */
    private Mono<Void> updatePasswordResetToken(String keycloakId, String resetToken) {
        return Mono.fromRunnable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource().get(keycloakId);
                UserRepresentation userRep = userResource.toRepresentation();
                
                Map<String, java.util.List<String>> attributes = userRep.getAttributes();
                if (attributes == null) {
                    attributes = new java.util.HashMap<>();
                }
                
                attributes.put("passwordResetToken", java.util.List.of(resetToken));
                userRep.setAttributes(attributes);
                userResource.update(userRep);
                
                log.info("Password reset token updated in Keycloak for user: {}", keycloakId);
            } catch (Exception e) {
                log.error("Error updating password reset token: {}", e.getMessage());
                throw new RuntimeException("Error updating password reset token: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).then();
    }

        @Override
    public Mono<KeycloakUserDto> getCompleteUserByKeycloakId(String keycloakId) {
        log.debug("Getting complete user by keycloakId: {}", keycloakId);
        return keycloakService.getUserByKeycloakId(keycloakId);
    }

    @Override
    public Mono<KeycloakUserDto> getCompleteUserByUsername(String username) {
        log.debug("Getting complete user by username: {}", username);
        return keycloakService.getUserByUsername(username);
    }

    @Override
    public Mono<KeycloakUserDto> getCompleteUserByEmail(String email) {
        log.info("Getting complete user by email: {}", email);
        return keycloakService.getUserByEmail(email);
    }

    @Override
    public Mono<KeycloakUserDto> updateCompleteUser(String keycloakId, User user) {
        log.info("Updating complete user with keycloakId: {}", keycloakId);
        
        // Validar que solo se puedan asignar roles teacher, auxiliary o secretary SI se proporcionan
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            boolean hasValidRoles = user.getRoles().stream()
                    .allMatch(role -> role.equals(Role.teacher.name()) || 
                                     role.equals(Role.auxiliary.name()) || 
                                     role.equals(Role.secretary.name()));
            
            if (!hasValidRoles) {
                return Mono.error(new RuntimeException("Solo se permiten roles teacher, auxiliary o secretary en este endpoint"));
            }
        }
        
        // Actualizar directamente en Keycloak (atributos + datos básicos)
        return keycloakService.updateUserAttributes(keycloakId, user)
                .then(keycloakService.getUserByKeycloakId(keycloakId))
                .doOnSuccess(result -> log.info("Complete user update finished"))
                .doOnError(error -> log.error("Error updating complete user: {}", error.getMessage()));
    }

    @Override
    public Mono<String> deleteCompleteUser(String keycloakId) {
        log.info("Deleting complete user with keycloakId: {}", keycloakId);
        
    // Eliminar solo de Keycloak
        return keycloakService.deleteUser(keycloakId)
                .doOnSuccess(result -> log.info("Complete user deletion finished: {}", result))
                .doOnError(error -> log.error("Error deleting complete user: {}", error.getMessage()));
    }

    @Override
    public Flux<KeycloakUserDto> getAllCompleteUsers() {
        log.debug("Getting all complete users from Keycloak");
        return keycloakService.getAllUsersWithAttributes();
    }

    @Override
    public Mono<KeycloakUserDto> changeUserStatus(String keycloakId, UserStatus status) {
        log.info("Changing user status to {} for keycloakId: {}", status, keycloakId);
        
        return keycloakService.updateUserStatus(keycloakId, status.name())
                .then(keycloakService.getUserByKeycloakId(keycloakId));
    }

    @Override
    public Flux<KeycloakUserDto> getUsersByStatus(UserStatus status) {
        log.debug("Getting users by status: {} from Keycloak", status);
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> user.getStatus() == status);
    }

    @Override
    public Mono<KeycloakUserDto> activateUser(String keycloakId) {
        log.info("Activating user with keycloakId: {}", keycloakId);
        
        return keycloakService.enableUser(keycloakId)
                .then(keycloakService.updateUserStatus(keycloakId, UserStatus.A.name()))
                .then(keycloakService.getUserByKeycloakId(keycloakId))
                .doOnSuccess(user -> log.info("User activated successfully: {}", keycloakId))
                .doOnError(error -> log.error("Error activating user: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> deactivateUser(String keycloakId) {
        log.info("Deactivating user with keycloakId: {}", keycloakId);
        
        return keycloakService.disableUser(keycloakId)
                .then(keycloakService.updateUserStatus(keycloakId, UserStatus.I.name()))
                .then(keycloakService.getUserByKeycloakId(keycloakId))
                .doOnSuccess(user -> log.info("User deactivated successfully: {}", keycloakId))
                .doOnError(error -> log.error("Error deactivating user: {}", error.getMessage()));
    }

    private String extractKeycloakIdFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Caso 1: Respuesta con mensaje completo
            if (response.contains("Usuario creado exitosamente con ID: ")) {
                String[] parts = response.split("Usuario creado exitosamente con ID: ");
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
            
            // Caso 2: Respuesta es solo el ID (UUID format)
            String trimmedResponse = response.trim();
            if (trimmedResponse.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                log.info("Response is a valid UUID, using as Keycloak ID: {}", trimmedResponse);
                return trimmedResponse;
            }
            
            log.warn("Unable to extract Keycloak ID from response: {}", response);
            return null;
            
        } catch (Exception e) {
            log.error("Error extracting Keycloak ID from response: {}", e.getMessage());
            return null;
        }
    }

    private String buildFullName(String firstname, String lastname) {
        if (firstname != null && lastname != null) {
            return firstname + " " + lastname;
        } else if (firstname != null) {
            return firstname;
        } else if (lastname != null) {
            return lastname;
        } else {
            return "Usuario";
        }
    }
}
