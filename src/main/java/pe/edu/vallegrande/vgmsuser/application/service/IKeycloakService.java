package pe.edu.vallegrande.vgmsuser.application.service;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;

import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IKeycloakService {
    Mono<List<UserRepresentation>> findAllUsers();
    Mono<List<UserRepresentation>> searchUserByUsername(String username);
    Mono<String> createUser(User userDTO);
    /**
     * Eliminar usuario de Keycloak
     */
    Mono<String> deleteUser(String keycloakId);
    
    /**
     * Cambiar contraseña de usuario en Keycloak
     */
    Mono<Void> changePassword(String keycloakId, String newPassword);
    Mono<Void> updateUser(String userId, User userDTO);
    
    /**
     * Activar usuario en Keycloak (enabled = true)
     */
    Mono<Void> enableUser(String keycloakId);
    
    /**
     * Desactivar usuario en Keycloak (enabled = false) - Eliminado lógico
     */
    Mono<Void> disableUser(String keycloakId);
    
    /**
     * Obtener usuario completo de Keycloak con todos sus atributos
     */
    Mono<KeycloakUserDto> getUserByKeycloakId(String keycloakId);
    
    /**
     * Obtener todos los usuarios de Keycloak con sus atributos
     */
    Flux<KeycloakUserDto> getAllUsersWithAttributes();
    
    /**
     * Actualizar atributos personalizados del usuario en Keycloak
     */
    Mono<Void> updateUserAttributes(String keycloakId, User user);
    
    /**
     * Actualizar token de reseteo de contraseña en Keycloak
     */
    Mono<Void> updatePasswordResetToken(String keycloakId, String resetToken);
    
    /**
     * Actualizar estado de contraseña en Keycloak
     */
    Mono<Void> updatePasswordStatus(String keycloakId, String passwordStatus, String passwordCreatedAt);
    
    /**
     * Actualizar estado del usuario en Keycloak (atributo status)
     */
    Mono<Void> updateUserStatus(String keycloakId, String status);
    
    /**
     * Buscar usuario por username en Keycloak
     */
    Mono<KeycloakUserDto> getUserByUsername(String username);
    
    /**
     * Buscar usuario por email en Keycloak
     */
    Mono<KeycloakUserDto> getUserByEmail(String email);
}