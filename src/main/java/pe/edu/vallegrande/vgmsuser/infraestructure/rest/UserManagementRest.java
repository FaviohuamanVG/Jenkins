package pe.edu.vallegrande.vgmsuser.infraestructure.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vgmsuser.application.service.IUserManagementService;
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
public class UserManagementRest {

    private final IUserManagementService userManagementService;

    // 🟡 DIRECTOR ENDPOINTS - Headers HTTP v5.0

    /**
     * POST /users/director/create
     * Headers: X-User-Id, X-User-Roles (DIRECTOR), X-Institution-Id (obligatorio)
     * Crear usuarios TEACHER, AUXILIARY, SECRETARY únicamente (institución se asigna automáticamente)
     */
    @PostMapping("/director/create")
    public Mono<ResponseEntity<Map<String, Object>>> createStaffUser(
            ServerHttpRequest request,
            @Valid @RequestBody User user) {
        
        log.info("🟡 DIRECTOR - Creating staff user with username: {}", user.getUsername());
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            // Auto-asignar la institución del director
            user.setInstitutionId(headers.getInstitutionId());
            
            return userManagementService.createCompleteUser(user)
                    .flatMap(createdUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User created successfully");
                        response.put("user", createdUser);
                        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(response));
                    })
                    .onErrorResume(error -> {
                        log.error("Error creating staff user: {}", error.getMessage());
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

    @GetMapping("/director/staff")
    public Mono<ResponseEntity<Map<String, Object>>> getAllStaff(ServerHttpRequest request) {
        
        log.info("🟡 DIRECTOR - Getting all staff");
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return userManagementService.getStaffByInstitution(headers.getInstitutionId())
                    .collectList()
                    .map(staff -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Users retrieved successfully");
                        response.put("total_users", staff.size());
                        response.put("users", staff);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error getting staff: {}", error.getMessage());
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

    @GetMapping("/director/by-role/{role}")
    public Mono<ResponseEntity<Map<String, Object>>> getStaffByRole(
            ServerHttpRequest request,
            @PathVariable String role) {
        
        log.info("🟡 DIRECTOR - Getting staff by role: {}", role);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return userManagementService.getStaffByInstitutionAndRole(headers.getInstitutionId(), role)
                    .collectList()
                    .map(staff -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Users retrieved successfully");
                        response.put("total_users", staff.size());
                        response.put("users", staff);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error getting staff by role: {}", error.getMessage());
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

    @PutMapping("/director/update/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateStaffUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId,
            @Valid @RequestBody User user) {
        
        log.info("🟡 DIRECTOR - Updating staff user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            // NO permitir cambiar roles o instituciones
            user.setRoles(null); // Ignorar roles en el request
            user.setInstitutionId(null); // Ignorar institutionId en el request
            
            return userManagementService.updateCompleteUser(keycloakId, user)
                    .map(updatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User updated successfully");
                        response.put("user", updatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error updating staff user: {}", error.getMessage());
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

    @DeleteMapping("/director/delete/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteStaffUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId) {
        
        log.info("🟡 DIRECTOR - Deleting (physical) staff user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return userManagementService.deleteCompleteUser(keycloakId)
                    .map(message -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", message);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Error deleting staff user: {}", e.getMessage());
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

    @PatchMapping("/director/deactivate/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> deactivateStaffUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId) {
        
        log.info("🟡 DIRECTOR - Deactivating staff user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return userManagementService.deactivateUser(keycloakId)
                    .map(deactivatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User deactivated successfully");
                        response.put("user", deactivatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Error deactivating staff user: {}", e.getMessage());
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

    @PatchMapping("/director/activate/{user_id}")
    public Mono<ResponseEntity<Map<String, Object>>> activateStaffUser(
            ServerHttpRequest request,
            @PathVariable("user_id") String keycloakId) {
        
        log.info("🟡 DIRECTOR - Activating staff user with keycloakId: {}", keycloakId);
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validateDirectorRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return userManagementService.activateUser(keycloakId)
                    .map(activatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User activated successfully");
                        response.put("user", activatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(e -> {
                        log.error("Error activating staff user: {}", e.getMessage());
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
