package pe.edu.vallegrande.vgmsuser.application.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.vgmsuser.application.service.IAdminUserService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.infraestructure.client.InstitutionValidationClient;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final IKeycloakService keycloakService;
    private final IEmailService emailService;
    private final InstitutionValidationClient institutionValidationClient;

    @Override
    public Mono<KeycloakUserDto> createAdminUser(User user) {
        log.info("Creating admin/director user with username: {}", user.getUsername());
        
        // Validar que solo se puedan asignar roles admin o director
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // Si no tiene roles, asignar admin por defecto
            user.setRoles(Set.of(Role.admin.name()));
        } else {
            // Validar que solo contenga roles admin o director
            boolean hasValidRoles = user.getRoles().stream()
                    .allMatch(role -> role.equals(Role.admin.name()) || role.equals(Role.director.name()));
            
            if (!hasValidRoles) {
                return Mono.error(new RuntimeException("Solo se permiten roles admin o director en este endpoint"));
            }
        }

        // Validar institución si el usuario tiene rol de director
        boolean isDirector = user.getRoles().contains(Role.director.name());
        
        if (isDirector) {
            // Validar que se proporcione institutionId para directores
            if (user.getInstitutionId() == null || user.getInstitutionId().trim().isEmpty()) {
                return Mono.error(new RuntimeException("El rol director requiere un institutionId válido"));
            }
            
            // Validar que la institución existe y está activa
            return institutionValidationClient.validateInstitution(user.getInstitutionId())
                    .flatMap(validationResponse -> {
                        if (validationResponse.getError() != null) {
                            return Mono.error(new RuntimeException("Error validando institución: " + validationResponse.getError()));
                        }
                        
                        if (!Boolean.TRUE.equals(validationResponse.getExists())) {
                            return Mono.error(new RuntimeException("La institución con ID " + user.getInstitutionId() + " no existe"));
                        }
                        
                        if (!Boolean.TRUE.equals(validationResponse.getActive())) {
                            return Mono.error(new RuntimeException("La institución " + validationResponse.getName() + " no está activa"));
                        }
                        
                        log.info("Institution validated successfully: {} - {}", user.getInstitutionId(), validationResponse.getName());
                        return createUserInKeycloak(user);
                    });
        } else {
            // Para usuarios ADMIN, no se requiere institución
            user.setInstitutionId(null);
            return createUserInKeycloak(user);
        }
    }
    
    private Mono<KeycloakUserDto> createUserInKeycloak(User user) {
        // Establecer contraseña temporal como el DNI
        String temporaryPassword = user.getDocumentNumber();
        user.setPassword(temporaryPassword);
        
        return keycloakService.createUser(user)
                .flatMap(keycloakUserId -> {
                    log.info("Admin/Director user created in Keycloak with ID: {}", keycloakUserId);
                    
                    // Generar token de reseteo y guardarlo en Keycloak
                    String resetToken = java.util.UUID.randomUUID().toString();
                    
                    return keycloakService.updatePasswordResetToken(keycloakUserId, resetToken)
                            .then(Mono.defer(() -> {
                                // Enviar email en background - no bloquea la creación si falla
                                emailService.sendTemporaryCredentialsEmail(
                                        user.getEmail(), 
                                        buildFullName(user.getFirstname(), user.getLastname(), user.getUsername()), 
                                        temporaryPassword, 
                                        resetToken)
                                        .doOnSuccess(v -> log.info("Email enviado exitosamente a: {}", user.getEmail()))
                                        .doOnError(error -> log.error("Error enviando email (no crítico): {}", error.getMessage()))
                                        .onErrorResume(error -> Mono.empty()) // Ignorar error de email
                                        .subscribe(); // Ejecutar en background
                                
                                // Retornar el usuario creado
                                return keycloakService.getUserByKeycloakId(keycloakUserId);
                            }));
                })
                .doOnError(error -> log.error("Error creating admin/director user: {}", error.getMessage()));
    }
    
    @Override
    public Flux<KeycloakUserDto> getAllDirectors() {
        log.info("Getting all directors");
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("director"))
                .doOnError(error -> log.error("Error getting all directors: {}", error.getMessage()));
    }
    
    @Override
    public Flux<KeycloakUserDto> getDirectorsByInstitution(String institutionId) {
        log.info("Getting directors for institution: {}", institutionId);
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> user.getRoles() != null && user.getRoles().contains("director"))
                .filter(user -> institutionId.equals(user.getInstitutionId()))
                .doOnError(error -> log.error("Error getting directors by institution: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> getAdminUserByKeycloakId(String keycloakId) {
        log.info("Getting admin/director user by keycloakId: {}", keycloakId);
        
        return keycloakService.getUserByKeycloakId(keycloakId)
                .filter(this::isAdminOrDirectorUser)
                .doOnNext(user -> log.info("Admin/Director user found: {}", user.getUsername()))
                .doOnError(error -> log.error("Error getting admin/director user by keycloakId: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> getAdminUserByUsername(String username) {
        log.info("Getting admin/director user by username: {}", username);
        
        return keycloakService.getUserByUsername(username)
                .filter(this::isAdminOrDirectorUser)
                .doOnNext(user -> log.info("Admin/Director user found: {}", user.getUsername()))
                .doOnError(error -> log.error("Error getting admin/director user by username: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> updateAdminUser(String keycloakId, User user) {
        log.info("Updating admin/director user with keycloakId: {}", keycloakId);
        
        // Validar que solo se puedan asignar roles admin o director
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            boolean hasValidRoles = user.getRoles().stream()
                    .allMatch(role -> role.equals(Role.admin.name()) || role.equals(Role.director.name()));
            
            if (!hasValidRoles) {
                return Mono.error(new RuntimeException("Solo se permiten roles admin o director en este endpoint"));
            }
        }
        
        return getAdminUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new RuntimeException("Admin/Director user not found")))
                .flatMap(existingUser -> {
                    // Si se proporciona un nuevo institutionId, validar la institución
                    if (user.getInstitutionId() != null && !user.getInstitutionId().trim().isEmpty()) {
                        log.info("New institutionId provided: {}, validating...", user.getInstitutionId());
                        
                        return institutionValidationClient.validateInstitution(user.getInstitutionId())
                                .flatMap(validationResponse -> {
                                    if (validationResponse.getError() != null) {
                                        return Mono.error(new RuntimeException("Error validando institución: " + validationResponse.getError()));
                                    }
                                    
                                    if (!Boolean.TRUE.equals(validationResponse.getExists())) {
                                        return Mono.error(new RuntimeException("La institución con ID " + user.getInstitutionId() + " no existe"));
                                    }
                                    
                                    if (!Boolean.TRUE.equals(validationResponse.getActive())) {
                                        return Mono.error(new RuntimeException("La institución " + validationResponse.getName() + " no está activa"));
                                    }
                                    
                                    log.info("Institution validated successfully: {} - {}", user.getInstitutionId(), validationResponse.getName());
                                    
                                    // Proceder con la actualización
                                    return performUpdate(keycloakId, user);
                                });
                    } else {
                        // Si no se proporciona institutionId o viene vacío, actualizar sin validación
                        log.info("No institutionId provided or empty, proceeding with update");
                        return performUpdate(keycloakId, user);
                    }
                })
                .doOnError(error -> log.error("Error updating admin/director user: {}", error.getMessage()));
    }
    
    /**
     * Método auxiliar para realizar la actualización en Keycloak
     */
    private Mono<KeycloakUserDto> performUpdate(String keycloakId, User user) {
        return keycloakService.updateUser(keycloakId, user)
                .then(keycloakService.updateUserAttributes(keycloakId, user))
                .then(keycloakService.getUserByKeycloakId(keycloakId));
    }

    @Override
    public Mono<String> deleteAdminUser(String keycloakId) {
        log.info("Deleting admin/director user with keycloakId: {}", keycloakId);
        
        return getAdminUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new RuntimeException("Admin/Director user not found")))
                .flatMap(user -> {
                    // Eliminar solo de Keycloak
                    return keycloakService.deleteUser(keycloakId)
                            .thenReturn("Usuario admin/director eliminado exitosamente");
                })
                .doOnError(error -> log.error("Error deleting admin/director user: {}", error.getMessage()));
    }

    @Override
    public Flux<KeycloakUserDto> getAllAdminUsers() {
        log.info("Getting all admin/director users");
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(this::isAdminOrDirectorUser)
                .doOnError(error -> log.error("Error getting all admin/director users: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> changeAdminUserStatus(String keycloakId, UserStatus status) {
        log.info("Changing admin/director user status to {} for keycloakId: {}", status, keycloakId);
        
        return keycloakService.updateUserStatus(keycloakId, status.name())
                .then(keycloakService.getUserByKeycloakId(keycloakId))
                .doOnError(error -> log.error("Error changing admin/director user status: {}", error.getMessage()));
    }

    @Override
    public Flux<KeycloakUserDto> getAdminUsersByStatus(UserStatus status) {
        log.info("Getting admin/director users by status: {}", status);
        
        return keycloakService.getAllUsersWithAttributes()
                .filter(user -> isAdminOrDirectorUser(user) && user.getStatus() == status)
                .doOnError(error -> log.error("Error getting admin/director users by status: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> activateAdminUser(String keycloakId) {
        log.info("Activating admin/director user with keycloakId: {}", keycloakId);
        
        return getAdminUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new RuntimeException("Admin/Director user not found")))
                .flatMap(existingUser -> {
                    // Activar en Keycloak (enabled = true) y actualizar status a ACTIVE
                    return keycloakService.enableUser(keycloakId)
                            .then(keycloakService.updateUserStatus(keycloakId, UserStatus.A.name()))
                            .then(keycloakService.getUserByKeycloakId(keycloakId));
                })
                .doOnSuccess(user -> log.info("Admin/Director user activated successfully: {}", keycloakId))
                .doOnError(error -> log.error("Error activating admin/director user: {}", error.getMessage()));
    }

    @Override
    public Mono<KeycloakUserDto> deactivateAdminUser(String keycloakId) {
        log.info("Deactivating admin/director user with keycloakId: {}", keycloakId);
        
        return getAdminUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new RuntimeException("Admin/Director user not found")))
                .flatMap(existingUser -> {
                    // Desactivar en Keycloak (enabled = false) y actualizar status a INACTIVE
                    return keycloakService.disableUser(keycloakId)
                            .then(keycloakService.updateUserStatus(keycloakId, UserStatus.I.name()))
                            .then(keycloakService.getUserByKeycloakId(keycloakId));
                })
                .doOnSuccess(user -> log.info("Admin/Director user deactivated successfully: {}", keycloakId))
                .doOnError(error -> log.error("Error deactivating admin/director user: {}", error.getMessage()));
    }

    @Override
    public Flux<Map<String, Object>> getDirectorsWithStaff() {
        log.info("Getting all directors with their staff");
        
        // Obtener todos los directores
        return getAllDirectors()
                .flatMap(director -> {
                    String institutionId = director.getInstitutionId();
                    
                    if (institutionId == null || institutionId.isEmpty()) {
                        // Si el director no tiene institución asignada, devolver solo el director sin staff
                        log.warn("Director {} doesn't have an institution assigned", director.getKeycloakId());
                        Map<String, Object> directorData = new java.util.HashMap<>();
                        directorData.put("director", director);
                        directorData.put("staff", java.util.Collections.emptyList());
                        directorData.put("total_staff", 0);
                        return Mono.just(directorData);
                    }
                    
                    // Obtener el staff de este director (usuarios con la misma institutionId y roles teacher, auxiliary, secretary)
                    return keycloakService.getAllUsersWithAttributes()
                            .filter(user -> {
                                // Filtrar usuarios que pertenezcan a la misma institución
                                boolean sameInstitution = institutionId.equals(user.getInstitutionId());
                                
                                // Filtrar solo roles teacher, auxiliary, secretary
                                boolean isStaff = user.getRoles() != null && user.getRoles().stream()
                                        .anyMatch(role -> role.equalsIgnoreCase("teacher") || 
                                                         role.equalsIgnoreCase("auxiliary") || 
                                                         role.equalsIgnoreCase("secretary"));
                                
                                return sameInstitution && isStaff;
                            })
                            .collectList()
                            .map(staffList -> {
                                Map<String, Object> directorData = new java.util.HashMap<>();
                                directorData.put("director", director);
                                directorData.put("staff", staffList);
                                directorData.put("total_staff", staffList.size());
                                return directorData;
                            });
                })
                .doOnComplete(() -> log.info("Successfully retrieved all directors with their staff"))
                .doOnError(error -> log.error("Error getting directors with staff: {}", error.getMessage()));
    }

    /**
     * Verifica si un usuario es admin o director basándose en sus roles
     */
    private boolean isAdminOrDirectorUser(KeycloakUserDto user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.equals("admin") || role.equals("director"));
    }

    /**
     * Construye el nombre completo del usuario
     */
    private String buildFullName(String firstname, String lastname, String username) {
        if (firstname != null && lastname != null && !firstname.trim().isEmpty() && !lastname.trim().isEmpty()) {
            return firstname.trim() + " " + lastname.trim();
        } else if (firstname != null && !firstname.trim().isEmpty()) {
            return firstname.trim();
        } else if (lastname != null && !lastname.trim().isEmpty()) {
            return lastname.trim();
        } else {
            return username; // Fallback al username si no hay nombres
        }
    }
}
