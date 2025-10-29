package pe.edu.vallegrande.vgmsuser.infraestructure.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsuser.application.service.IUserManagementService;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.UserInfoResponse;
import reactor.core.publisher.Mono;

/**
 * REST Controller para endpoints de comunicación entre microservicios
 * NO requiere headers de autenticación (uso interno)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserInfoRest {

    private final IUserManagementService userManagementService;

    /**
     * GET /user-role/{user_email}
     * Obtener información completa del usuario por email
     * Endpoint para comunicación entre microservicios
     * 
     * @param user_email El email del usuario a consultar
     * @return UserInfoResponse con id, email, roles, institution_id, status, has_access
     */
    @GetMapping("/user-role/{user_email}")
    public Mono<ResponseEntity<UserInfoResponse>> getUserRoleByEmail(@PathVariable("user_email") String userEmail) {
        log.info("🔗 MICROSERVICE - Getting user info by email: {}", userEmail);
        
        return userManagementService.getCompleteUserByEmail(userEmail)
                .map(keycloakUser -> {
                    UserInfoResponse response = mapToUserInfoResponse(keycloakUser);
                    log.info("✅ User found: {} with roles: {}", userEmail, response.getRoles());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("❌ Error getting user by email: {}", error.getMessage());
                    if (error.getMessage().contains("not found") || error.getMessage().contains("no encontrado")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * GET /user-role-by-id/{user_id}
     * Obtener información completa del usuario por ID (keycloakId)
     * Endpoint para comunicación entre microservicios
     * 
     * @param user_id El keycloakId del usuario a consultar
     * @return UserInfoResponse con id, email, roles, institution_id, status, has_access
     */
    @GetMapping("/user-role-by-id/{user_id}")
    public Mono<ResponseEntity<UserInfoResponse>> getUserRoleById(@PathVariable("user_id") String keycloakId) {
        log.info("🔗 MICROSERVICE - Getting user info by keycloakId: {}", keycloakId);
        
        return userManagementService.getCompleteUserByKeycloakId(keycloakId)
                .map(keycloakUser -> {
                    UserInfoResponse response = mapToUserInfoResponse(keycloakUser);
                    log.info("✅ User found: {} with roles: {}", keycloakUser.getEmail(), response.getRoles());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("❌ Error getting user by keycloakId: {}", error.getMessage());
                    if (error.getMessage().contains("not found") || error.getMessage().contains("no encontrado")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Mapea KeycloakUserDto a UserInfoResponse
     * @param keycloakUser Usuario de Keycloak
     * @return UserInfoResponse con formato para otros microservicios
     */
    private UserInfoResponse mapToUserInfoResponse(KeycloakUserDto keycloakUser) {
        String statusStr = keycloakUser.getStatus() != null ? keycloakUser.getStatus().name() : "I";
        
        return UserInfoResponse.builder()
                .id(keycloakUser.getKeycloakId())
                .email(keycloakUser.getEmail())
                .roles(keycloakUser.getRoles() != null ? new java.util.ArrayList<>(keycloakUser.getRoles()) : new java.util.ArrayList<>())
                .institutionId(keycloakUser.getInstitutionId())
                .status(statusStr)
                .hasAccess(keycloakUser.isEnabled() && "A".equals(statusStr))
                .build();
    }
}
