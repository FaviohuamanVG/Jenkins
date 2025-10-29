package pe.edu.vallegrande.vgmsuser.application.service;

import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IUserManagementService {
    
    /**
     * POST /users/director/create
     * Crear usuario completo (solo Keycloak) - TEACHER, AUXILIARY, SECRETARY
     */
    Mono<KeycloakUserDto> createCompleteUser(User user);
    
    /**
     * GET /users/director/staff
     * Listar todos los usuarios de una institución (TEACHER, AUXILIARY, SECRETARY)
     */
    Flux<KeycloakUserDto> getStaffByInstitution(String institutionId);
    
    /**
     * GET /users/director/by-role/{role}
     * Listar usuarios por rol específico de una institución
     */
    Flux<KeycloakUserDto> getStaffByInstitutionAndRole(String institutionId, String role);
    
    /**
     * PUT /users/director/update/{user_id}
     * Actualizar usuario completo (NO puede cambiar roles o instituciones)
     */
    Mono<KeycloakUserDto> updateCompleteUser(String keycloakId, User user);
    
    /**
     * DELETE /users/director/delete/{user_id}
     * Eliminar usuario completo (solo Keycloak)
     */
    Mono<String> deleteCompleteUser(String keycloakId);
    
    /**
     * GET /users/personal/profile
     * Obtener usuario completo de Keycloak
     */
    Mono<KeycloakUserDto> getCompleteUserByKeycloakId(String keycloakId);
    
    /**
     * Obtener usuario completo por username
     */
    Mono<KeycloakUserDto> getCompleteUserByUsername(String username);
    
    /**
     * Obtener usuario completo por email
     */
    Mono<KeycloakUserDto> getCompleteUserByEmail(String email);
    
    /**
     * Listar todos los usuarios con información completa
     */
    Flux<KeycloakUserDto> getAllCompleteUsers();
    
    /**
     * Cambiar estado del usuario
     */
    Mono<KeycloakUserDto> changeUserStatus(String keycloakId, UserStatus status);
    
    /**
     * Activar usuario (Keycloak: enabled = true, status = ACTIVE)
     */
    Mono<KeycloakUserDto> activateUser(String keycloakId);
    
    /**
     * Desactivar usuario (Keycloak: enabled = false, status = INACTIVE)
     */
    Mono<KeycloakUserDto> deactivateUser(String keycloakId);
    
    /**
     * Buscar usuarios por estado
     */
    Flux<KeycloakUserDto> getUsersByStatus(UserStatus status);
}
