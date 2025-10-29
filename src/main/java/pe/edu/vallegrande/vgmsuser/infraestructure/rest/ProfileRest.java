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
public class ProfileRest {

    private final IUserManagementService userManagementService;

    // 🟢 PERSONAL ENDPOINTS - TEACHER, AUXILIARY, SECRETARY, DIRECTOR

    @GetMapping("/personal/profile")
    public Mono<ResponseEntity<Map<String, Object>>> getPersonalProfile(ServerHttpRequest request) {
        
        log.info("🟢 PERSONAL - Getting personal profile");
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validatePersonalRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            return userManagementService.getCompleteUserByKeycloakId(headers.getUserId())
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User retrieved successfully");
                        response.put("user", user);
                        return ResponseEntity.ok(response);
                    })
                    .switchIfEmpty(Mono.fromSupplier(() -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "User not found");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
                    }))
                    .onErrorResume(error -> {
                        log.error("Error getting personal profile: {}", error.getMessage());
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

    @PutMapping("/personal/update")
    public Mono<ResponseEntity<Map<String, Object>>> updatePersonalProfile(
            ServerHttpRequest request,
            @Valid @RequestBody User user) {
        
        log.info("🟢 PERSONAL - Updating personal profile");
        
        try {
            HeaderValidator.HeaderValidationResult headers = HeaderValidator.validatePersonalRole(request);
            log.info("Headers - User ID: {}, Roles: {}, Institution: {}", headers.getUserId(), headers.getUserRoles(), headers.getInstitutionId());
            
            // NO permitir cambiar roles o instituciones
            user.setRoles(null); // Ignorar roles en el request
            user.setInstitutionId(null); // Ignorar institutionId en el request
            
            return userManagementService.updateCompleteUser(headers.getUserId(), user)
                    .map(updatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "User updated successfully");
                        response.put("user", updatedUser);
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(error -> {
                        log.error("Error updating personal profile: {}", error.getMessage());
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
}
