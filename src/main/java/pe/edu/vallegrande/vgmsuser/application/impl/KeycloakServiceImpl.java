package pe.edu.vallegrande.vgmsuser.application.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import pe.edu.vallegrande.vgmsuser.infraestructure.util.KeycloakProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeycloakServiceImpl implements IKeycloakService {

    private final KeycloakProvider keycloakProvider;

    @Autowired
    public KeycloakServiceImpl(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    @Override
    public Mono<List<UserRepresentation>> findAllUsers() {
        return Mono.fromCallable(() -> keycloakProvider.getRealmResource().users().list())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<List<UserRepresentation>> searchUserByUsername(String username) {
        return Mono.fromCallable(() -> keycloakProvider.getRealmResource().users().searchByUsername(username, true))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> createUser(User userDTO) {
        return Mono.fromCallable(() -> {
            UsersResource usersResource = keycloakProvider.getUserResource();

            // Check if the user already exists
            List<UserRepresentation> existingUsers = usersResource.searchByUsername(userDTO.getUsername(), true);
            if (!existingUsers.isEmpty()) {
                return "El usuario ya existe: " + userDTO.getUsername();
            }

            // Create user representation
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setFirstName(userDTO.getFirstname());
            userRepresentation.setLastName(userDTO.getLastname());
            userRepresentation.setEmail(userDTO.getEmail());
            userRepresentation.setUsername(userDTO.getUsername());
            userRepresentation.setEmailVerified(true);
            userRepresentation.setEnabled(true);
            
            // Agregar atributos personalizados
            java.util.Map<String, java.util.List<String>> attributes = new java.util.HashMap<>();
            
            // Solo agregar atributos si tienen valor (no vacíos)
            if (userDTO.getDocumentType() != null) {
                attributes.put("documentType", java.util.List.of(userDTO.getDocumentType().name()));
            }
            if (userDTO.getDocumentNumber() != null && !userDTO.getDocumentNumber().isEmpty()) {
                attributes.put("documentNumber", java.util.List.of(userDTO.getDocumentNumber()));
            }
            if (userDTO.getPhone() != null && !userDTO.getPhone().isEmpty()) {
                attributes.put("phone", java.util.List.of(userDTO.getPhone()));
            }
            if (userDTO.getInstitutionId() != null && !userDTO.getInstitutionId().isEmpty()) {
                attributes.put("institutionId", java.util.List.of(userDTO.getInstitutionId()));
            }
            if (userDTO.getStatus() != null) {
                attributes.put("status", java.util.List.of(userDTO.getStatus().name()));
            } else {
                attributes.put("status", java.util.List.of("A"));
            }
            
            attributes.put("passwordStatus", java.util.List.of("TEMPORARY"));
            attributes.put("passwordCreatedAt", java.util.List.of(java.time.LocalDateTime.now().toString()));
            
            userRepresentation.setAttributes(attributes);

            // Create the user
            Response response = usersResource.create(userRepresentation);
            int status = response.getStatus();

            if (status == 201) {
                String path = response.getLocation().getPath();
                String userId = path.substring(path.lastIndexOf('/') + 1);

                // Set password
                UserResource userResource = usersResource.get(userId);
                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(userDTO.getPassword());
                passwordCred.setTemporary(false);
                userResource.resetPassword(passwordCred);
                
                // Actualizar atributos después de crear (fix para Keycloak)
                UserRepresentation userRep = userResource.toRepresentation();
                userRep.setAttributes(attributes);
                userResource.update(userRep);

                // Assign roles
                RealmResource realmResource = keycloakProvider.getRealmResource();
                List<RoleRepresentation> rolesRepresentation;

                if (userDTO.getRoles() == null || userDTO.getRoles().isEmpty()) {
                    // Default role: teacher
                    RoleRepresentation teacherRole = realmResource.roles().get("teacher").toRepresentation();
                    rolesRepresentation = List.of(teacherRole);
                } else {
                    // Assign specified roles
                    rolesRepresentation = realmResource.roles()
                            .list()
                            .stream()
                            .filter(role -> userDTO.getRoles()
                                    .stream()
                                    .anyMatch(roleName -> roleName.equalsIgnoreCase(role.getName())))
                            .toList();
                }

                userResource.roles().realmLevel().add(rolesRepresentation);

                log.info("Usuario creado exitosamente con ID: {}", userId);
                return userId; // Retornar solo el ID del usuario

            } else if (status == 409) {
                return "El usuario ya existe con ese username o email";
            } else {
                String errorMessage = "";
                if (response.hasEntity()) {
                    errorMessage = response.readEntity(String.class);
                }
                return "Error al crear usuario. Status HTTP: " + status +
                        (errorMessage.isEmpty() ? "" : " - " + errorMessage);
            }

        }).subscribeOn(Schedulers.boundedElastic());
    }

    // eliminado físico por ahora puse eso
    @Override
    public Mono<String> deleteUser(String userId) {
        return Mono.fromCallable(() -> {
            keycloakProvider.getUserResource().get(userId).remove();
            return "Usuario eliminado exitosamente";
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> updateUser(String userId, User userDTO) {
        return Mono.fromRunnable(() -> {
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setFirstName(userDTO.getFirstname());
            userRepresentation.setLastName(userDTO.getLastname());
            userRepresentation.setEmail(userDTO.getEmail());
            userRepresentation.setUsername(userDTO.getUsername());
            userRepresentation.setEmailVerified(true);

            UserResource userResource = keycloakProvider.getUserResource().get(userId);

            // Solo actualizar contraseña si se proporciona
            if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(userDTO.getPassword());
                passwordCred.setTemporary(false);
                userResource.resetPassword(passwordCred);
            }

            // Actualizar datos básicos del usuario
            userResource.update(userRepresentation);

            // Actualizar roles si se proporcionan
            if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                RealmResource realmResource = keycloakProvider.getRealmResource();

                // Obtener roles actuales del usuario y removerlos
                List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
                if (!currentRoles.isEmpty()) {
                    userResource.roles().realmLevel().remove(currentRoles);
                }

                // Asignar nuevos roles
                List<RoleRepresentation> newRoles = realmResource.roles()
                        .list()
                        .stream()
                        .filter(role -> userDTO.getRoles()
                                .stream()
                                .anyMatch(roleName -> roleName.equalsIgnoreCase(role.getName())))
                        .toList();

                if (!newRoles.isEmpty()) {
                    userResource.roles().realmLevel().add(newRoles);
                }
            }

        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> changePassword(String keycloakId, String newPassword) {
        log.info("Changing password for user with keycloakId: {}", keycloakId);
        
        return Mono.fromRunnable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource().get(keycloakId);

                // Crear credenciales con la nueva contraseña
                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(newPassword);
                passwordCred.setTemporary(false); // No es temporal

                // Resetear la contraseña
                userResource.resetPassword(passwordCred);
                
                log.info("Password changed successfully for keycloakId: {}", keycloakId);

            } catch (Exception e) {
                log.error("Error changing password for keycloakId {}: {}", keycloakId, e.getMessage());
                throw new RuntimeException("Error changing password: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> enableUser(String keycloakId) {
        log.info("Enabling user with keycloakId: {}", keycloakId);
        
        return Mono.fromRunnable(() -> {
            try {
                // Obtener el usuario usando el provider
                UserResource userResource = keycloakProvider.getUserResource().get(keycloakId);
                UserRepresentation userRepresentation = userResource.toRepresentation();
                
                // Activar el usuario
                userRepresentation.setEnabled(true);
                
                // Actualizar en Keycloak
                userResource.update(userRepresentation);
                
                log.info("User enabled successfully in Keycloak: {}", keycloakId);
                
            } catch (Exception e) {
                log.error("Error enabling user with keycloakId {}: {}", keycloakId, e.getMessage());
                throw new RuntimeException("Error enabling user in Keycloak: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> disableUser(String keycloakId) {
        log.info("Disabling user with keycloakId: {}", keycloakId);
        
        return Mono.fromRunnable(() -> {
            try {
                // Obtener el usuario usando el provider
                UserResource userResource = keycloakProvider.getUserResource().get(keycloakId);
                UserRepresentation userRepresentation = userResource.toRepresentation();
                
                // Desactivar el usuario (eliminado lógico)
                userRepresentation.setEnabled(false);
                
                // Actualizar en Keycloak
                userResource.update(userRepresentation);
                
                log.info("User disabled successfully in Keycloak: {}", keycloakId);
                
            } catch (Exception e) {
                log.error("Error disabling user with keycloakId {}: {}", keycloakId, e.getMessage());
                throw new RuntimeException("Error disabling user in Keycloak: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<KeycloakUserDto> getUserByKeycloakId(String keycloakId) {
        log.info("Getting user by keycloakId: {}", keycloakId);
        
        return Mono.fromCallable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource().get(keycloakId);
                UserRepresentation userRep = userResource.toRepresentation();
                
                return mapToKeycloakUserDto(userRep);
                
            } catch (Exception e) {
                log.error("Error getting user with keycloakId {}: {}", keycloakId, e.getMessage());
                throw new RuntimeException("Error getting user from Keycloak: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<KeycloakUserDto> getAllUsersWithAttributes() {
        log.info("Getting all users with attributes from Keycloak");
        
        return Mono.fromCallable(() -> {
            // Obtener lista de usuarios
            List<UserRepresentation> users = keycloakProvider.getUserResource().list();
            
            // Para cada usuario, recargar con atributos completos
            return users.stream()
                    .map(user -> {
                        try {
                            // Recargar usuario individual para obtener todos los atributos
                            UserResource userResource = keycloakProvider.getUserResource().get(user.getId());
                            return userResource.toRepresentation();
                        } catch (Exception e) {
                            log.warn("Error reloading user {}: {}", user.getUsername(), e.getMessage());
                            return user; // Usar el original si falla
                        }
                    })
                    .collect(Collectors.toList());
        })
        .flatMapMany(Flux::fromIterable)
        .map(this::mapToKeycloakUserDto)
        .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> updateUserAttributes(String keycloakId, User user) {
        log.info("Updating user attributes for keycloakId: {}", keycloakId);
        
        return Mono.fromRunnable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource().get(keycloakId);
                UserRepresentation userRep = userResource.toRepresentation();
                
                // Actualizar datos básicos
                userRep.setFirstName(user.getFirstname());
                userRep.setLastName(user.getLastname());
                userRep.setEmail(user.getEmail());
                userRep.setUsername(user.getUsername());
                
                // Actualizar atributos personalizados
                Map<String, java.util.List<String>> attributes = userRep.getAttributes();
                if (attributes == null) {
                    attributes = new java.util.HashMap<>();
                }
                
                // Solo actualizar si tienen valor (no null ni vacío)
                if (user.getDocumentType() != null) {
                    attributes.put("documentType", java.util.List.of(user.getDocumentType().name()));
                }
                if (user.getDocumentNumber() != null && !user.getDocumentNumber().isEmpty()) {
                    attributes.put("documentNumber", java.util.List.of(user.getDocumentNumber()));
                }
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    attributes.put("phone", java.util.List.of(user.getPhone()));
                }
                if (user.getStatus() != null) {
                    attributes.put("status", java.util.List.of(user.getStatus().name()));
                }
                if (user.getInstitutionId() != null && !user.getInstitutionId().isEmpty()) {
                    attributes.put("institutionId", java.util.List.of(user.getInstitutionId()));
                }
                
                attributes.put("updatedAt", java.util.List.of(LocalDateTime.now().toString()));
                
                userRep.setAttributes(attributes);
                userResource.update(userRep);
                
                log.info("✅ Atributos actualizados en Keycloak: {}", attributes.keySet());
                
                // Actualizar roles si se proporcionan
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    RealmResource realmResource = keycloakProvider.getRealmResource();
                    
                    // Obtener roles actuales y removerlos
                    List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
                    if (!currentRoles.isEmpty()) {
                        userResource.roles().realmLevel().remove(currentRoles);
                    }
                    
                    // Asignar nuevos roles
                    List<RoleRepresentation> newRoles = realmResource.roles()
                            .list()
                            .stream()
                            .filter(role -> user.getRoles()
                                    .stream()
                                    .anyMatch(roleName -> roleName.equalsIgnoreCase(role.getName())))
                            .toList();
                    
                    if (!newRoles.isEmpty()) {
                        userResource.roles().realmLevel().add(newRoles);
                    }
                }
                
                log.info("User attributes updated successfully for keycloakId: {}", keycloakId);
                
            } catch (Exception e) {
                log.error("Error updating user attributes for keycloakId {}: {}", keycloakId, e.getMessage());
                throw new RuntimeException("Error updating user attributes in Keycloak: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Mapea un UserRepresentation de Keycloak a KeycloakUserDto
     */
    private KeycloakUserDto mapToKeycloakUserDto(UserRepresentation userRep) {
        Map<String, java.util.List<String>> attributes = userRep.getAttributes();
        
        // Obtener roles del usuario
        Set<String> roles = null;
        try {
            UserResource userResource = keycloakProvider.getUserResource().get(userRep.getId());
            roles = userResource.roles().realmLevel().listAll()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Could not fetch roles for user {}: {}", userRep.getId(), e.getMessage());
        }
        
        return KeycloakUserDto.builder()
                .keycloakId(userRep.getId())
                .username(userRep.getUsername())
                .email(userRep.getEmail())
                .firstname(userRep.getFirstName())
                .lastname(userRep.getLastName())
                .documentType(getEnumAttribute(attributes, "documentType", DocumentType.class))
                .documentNumber(getStringAttribute(attributes, "documentNumber"))
                .phone(getStringAttribute(attributes, "phone"))
                .status(getEnumAttribute(attributes, "status", UserStatus.class))
                .passwordStatus(getEnumAttribute(attributes, "passwordStatus", PasswordStatus.class))
                .passwordCreatedAt(getDateTimeAttribute(attributes, "passwordCreatedAt"))
                .passwordResetToken(getStringAttribute(attributes, "passwordResetToken"))
                .institutionId(getStringAttribute(attributes, "institutionId"))
                .roles(roles)
                .enabled(userRep.isEnabled())
                .createdAt(userRep.getCreatedTimestamp() != null ? 
                        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(userRep.getCreatedTimestamp()), 
                        java.time.ZoneId.systemDefault()) : null)
                .updatedAt(getDateTimeAttribute(attributes, "updatedAt"))
                .build();
    }

    private String getStringAttribute(Map<String, java.util.List<String>> attributes, String key) {
        if (attributes == null || !attributes.containsKey(key)) {
            return null;
        }
        java.util.List<String> values = attributes.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.get(0);
        // Retornar null si el valor es vacío
        return (value != null && !value.isEmpty()) ? value : null;
    }

    private <E extends Enum<E>> E getEnumAttribute(Map<String, java.util.List<String>> attributes, String key, Class<E> enumClass) {
        String value = getStringAttribute(attributes, key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value {} for {}", value, key);
            return null;
        }
    }

    private LocalDateTime getDateTimeAttribute(Map<String, java.util.List<String>> attributes, String key) {
        String value = getStringAttribute(attributes, key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            log.warn("Invalid datetime value {} for {}", value, key);
            return null;
        }
    }

    @Override
    public Mono<Void> updatePasswordResetToken(String keycloakId, String resetToken) {
        return Mono.fromRunnable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource()
                        .get(keycloakId);
                
                UserRepresentation userRep = userResource.toRepresentation();
                Map<String, java.util.List<String>> attributes = userRep.getAttributes();
                
                if (attributes == null) {
                    attributes = new HashMap<>();
                }
                
                // Actualizar o limpiar passwordResetToken
                if (resetToken != null && !resetToken.isEmpty()) {
                    attributes.put("passwordResetToken", java.util.List.of(resetToken));
                } else {
                    attributes.remove("passwordResetToken");
                }
                
                userRep.setAttributes(attributes);
                userResource.update(userRep);
                
                log.info("Password reset token updated for user: {}", keycloakId);
            } catch (Exception e) {
                log.error("Error updating password reset token: {}", e.getMessage());
                throw new RuntimeException("Error al actualizar token de reseteo: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<Void> updatePasswordStatus(String keycloakId, String passwordStatus, String passwordCreatedAt) {
        return Mono.fromRunnable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource()
                        .get(keycloakId);
                
                UserRepresentation userRep = userResource.toRepresentation();
                Map<String, java.util.List<String>> attributes = userRep.getAttributes();
                
                if (attributes == null) {
                    attributes = new HashMap<>();
                }
                
                // Actualizar passwordStatus
                if (passwordStatus != null && !passwordStatus.isEmpty()) {
                    attributes.put("passwordStatus", java.util.List.of(passwordStatus));
                }
                
                // Actualizar passwordCreatedAt
                if (passwordCreatedAt != null && !passwordCreatedAt.isEmpty()) {
                    attributes.put("passwordCreatedAt", java.util.List.of(passwordCreatedAt));
                }
                
                userRep.setAttributes(attributes);
                userResource.update(userRep);
                
                log.info("Password status updated for user: {}", keycloakId);
            } catch (Exception e) {
                log.error("Error updating password status: {}", e.getMessage());
                throw new RuntimeException("Error al actualizar estado de contraseña: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<Void> updateUserStatus(String keycloakId, String status) {
        return Mono.fromRunnable(() -> {
            try {
                UserResource userResource = keycloakProvider.getUserResource()
                        .get(keycloakId);
                
                UserRepresentation userRep = userResource.toRepresentation();
                Map<String, java.util.List<String>> attributes = userRep.getAttributes();
                
                if (attributes == null) {
                    attributes = new HashMap<>();
                }
                
                // Actualizar status
                if (status != null && !status.isEmpty()) {
                    attributes.put("status", java.util.List.of(status));
                }
                
                userRep.setAttributes(attributes);
                userResource.update(userRep);
                
                log.info("User status updated for user: {}", keycloakId);
            } catch (Exception e) {
                log.error("Error updating user status: {}", e.getMessage());
                throw new RuntimeException("Error al actualizar estado de usuario: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<KeycloakUserDto> getUserByUsername(String username) {
        return Mono.fromCallable(() -> {
            try {
                java.util.List<UserRepresentation> users = keycloakProvider.getUserResource()
                        .search(username, true); // Búsqueda exacta
                
                if (users.isEmpty()) {
                    return null;
                }
                
                UserRepresentation userRep = users.get(0);
                return mapToKeycloakUserDto(userRep);
                
            } catch (Exception e) {
                log.error("Error getting user by username {}: {}", username, e.getMessage());
                throw new RuntimeException("Error al obtener usuario por username: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<KeycloakUserDto> getUserByEmail(String email) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Searching user by email: {}", email);
                
                // Buscar por email usando searchByEmail
                java.util.List<UserRepresentation> users = keycloakProvider.getUserResource()
                        .searchByEmail(email, true); // Búsqueda exacta por email
                
                if (users.isEmpty()) {
                    log.warn("User not found with email: {}", email);
                    throw new RuntimeException("Usuario no encontrado con email: " + email);
                }
                
                UserRepresentation userRep = users.get(0);
                log.info("User found by email: {} - keycloakId: {}", email, userRep.getId());
                return mapToKeycloakUserDto(userRep);
                
            } catch (Exception e) {
                log.error("Error getting user by email {}: {}", email, e.getMessage());
                throw new RuntimeException("Error al obtener usuario por email: " + e.getMessage());
            }
        });
    }

}
