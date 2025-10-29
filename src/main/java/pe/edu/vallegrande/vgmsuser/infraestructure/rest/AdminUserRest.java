package pe.edu.vallegrande.vgmsuser.infraestructure.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsuser.application.service.IAdminUserService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.infraestructure.util.HeaderValidator;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AdminUserRest {

    private final IAdminUserService adminUserService;

    // 🔴 ADMIN ENDPOINTS - Headers HTTP v5.0

    /**
     * POST /users/admin/create
     * Headers: X-User-Id, X-User-Roles (ADMIN), X-Institution-Id (null)
     * Crear usuarios ADMIN y DIRECTOR únicamente (puede asignar institución a directores)
     */
    @PostMapping("/admin/create")
    public Mono<ResponseEntity<Map<String, Object>>> createAdminUser(
            ServerHttpRequest request,
            @Valid @RequestBody User user) {
        
        log.info("🔴 ADMIN - Creating admin/director user with username: {}", user.getUsername());
        
        try {
            // Validar headers
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.createAdminUser(user)
                    .flatMap(createdUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User created successfully");
                        response.put("user", createdUser);
                        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(response));
                    })
                    .onErrorResume(error -> {
                        log.error("Error creating admin/director user: {}", error.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", error.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    /**
     * GET /users/admin
     * Headers: X-User-Id, X-User-Roles (ADMIN), X-Institution-Id (null)
     * Listar todos los usuarios ADMIN del sistema
     */
    @GetMapping("/admin")
    public Mono<ResponseEntity<Map<String, Object>>> getAllAdminUsers(ServerHttpRequest request) {
        
        log.info("🔴 ADMIN - Getting all admin users");
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.getAllAdminUsers()
                    .collectList()
                    .map(users -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Users retrieved successfully");
                        response.put("total_users", users.size());
                        response.put("users", users);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error getting all admin users: {}", error.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", error.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    /**
     * GET /users/admin/directors
     * Headers: X-User-Id (REQUIRED), X-User-Roles (REQUIRED - ADMIN), X-Institution-Id (OPTIONAL)
     * Listar todos los usuarios DIRECTORES de todas las instituciones
     */
    @GetMapping("/admin/directors")
    public Mono<ResponseEntity<Map<String, Object>>> getAllDirectors(ServerHttpRequest request) {
        
        log.info("🔴 ADMIN - Getting all directors");
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.getAllDirectors()
                    .collectList()
                    .map(directors -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Directors retrieved successfully");
                        response.put("total_users", directors.size());
                        response.put("users", directors);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error getting all directors: {}", error.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", error.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    @GetMapping("/admin/directors/{institution_id}")
    public Mono<ResponseEntity<Map<String, Object>>> getDirectorsByInstitution(
            ServerHttpRequest request,
            @PathVariable("institution_id") String targetInstitutionId) {
        
        log.info("🔴 ADMIN - Getting directors for institution: {}", targetInstitutionId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.getDirectorsByInstitution(targetInstitutionId)
                    .collectList()
                    .map(directors -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Directors retrieved successfully");
                        response.put("total_users", directors.size());
                        response.put("users", directors);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error getting directors by institution: {}", error.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", error.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    @PutMapping("/admin/update/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateAdminUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId,
            @Valid @RequestBody User user) {
        
        log.info("🔴 ADMIN - Updating user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.updateAdminUser(keycloakId, user)
                    .map(updatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User updated successfully");
                        response.put("user", updatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error updating admin user: {}", error.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", error.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    @DeleteMapping("/admin/delete/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteAdminUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId) {
        
        log.info("🔴 ADMIN - Deleting (physical) admin/director user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.deleteAdminUser(keycloakId)
                    .map(message -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", message);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Error deleting user: {}", e.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    @PatchMapping("/admin/deactivate/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> deactivateAdminUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId) {
        
        log.info("🔴 ADMIN - Deactivating admin/director user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.deactivateAdminUser(keycloakId)
                    .map(deactivatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User deactivated successfully");
                        response.put("user", deactivatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Error deactivating user: {}", e.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    @PatchMapping("/admin/activate/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> activateAdminUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId) {
        
        log.info("🔴 ADMIN - Activating admin/director user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateAdminRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return adminUserService.activateAdminUser(keycloakId)
                    .map(activatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User activated successfully");
                        response.put("user", activatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Error activating user: {}", e.getMessage());
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                    });
        } catch (Exception e) {
            log.error("Header validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }
}
