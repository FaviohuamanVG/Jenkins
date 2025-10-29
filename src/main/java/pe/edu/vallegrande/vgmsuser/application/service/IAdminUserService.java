package pe.edu.vallegrande.vgmsuser.application.service;

import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface IAdminUserService {
    
    /**
     * POST /users/admin/create
     * Crear usuario admin/director (solo Keycloak) con roles admin o director
     */
    Mono<KeycloakUserDto> createAdminUser(User user);
    
    /**
     * GET /users/admin
     * Listar todos los usuarios ADMIN del sistema
     */
    Flux<KeycloakUserDto> getAllAdminUsers();
    
    /**
     * GET /users/admin/directors
     * Listar todos los usuarios DIRECTORES de todas las instituciones
     */
    Flux<KeycloakUserDto> getAllDirectors();
    
    /**
     * GET /users/admin/directors/{institution_id}
     * Listar usuarios DIRECTORES de una institución específica
     */
    Flux<KeycloakUserDto> getDirectorsByInstitution(String institutionId);
    
    /**
     * PUT /users/admin/update/{user_id}
     * Actualizar usuarios ADMIN y DIRECTORES (puede cambiar roles e instituciones)
     */
    Mono<KeycloakUserDto> updateAdminUser(String keycloakId, User user);
    
    /**
     * DELETE /users/admin/delete/{user_id}
     * Eliminar usuarios ADMIN y DIRECTORES únicamente
     */
    Mono<String> deleteAdminUser(String keycloakId);
    
    /**
     * Obtener usuario admin/director completo de Keycloak
     */
    Mono<KeycloakUserDto> getAdminUserByKeycloakId(String keycloakId);
    
    /**
     * Obtener usuario admin/director completo por username
     */
    Mono<KeycloakUserDto> getAdminUserByUsername(String username);
    
    /**
     * Cambiar estado del usuario admin/director
     */
    Mono<KeycloakUserDto> changeAdminUserStatus(String keycloakId, UserStatus status);
    
    /**
     * Activar usuario admin/director (Keycloak: enabled = true, status = ACTIVE)
     */
    Mono<KeycloakUserDto> activateAdminUser(String keycloakId);
    
    /**
     * Desactivar usuario admin/director (Keycloak: enabled = false, status = INACTIVE)
     */
    Mono<KeycloakUserDto> deactivateAdminUser(String keycloakId);
    
    /**
     * Buscar usuarios admin/director por estado
     */
    Flux<KeycloakUserDto> getAdminUsersByStatus(UserStatus status);
    
    /**
     * GET /users/admin/directors-with-staff
     * Obtener DIRECTORES con su personal (TEACHER, AUXILIARY, SECRETARY) agrupado por institución
     */
    Flux<Map<String, Object>> getDirectorsWithStaff();
}
