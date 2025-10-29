package pe.edu.vallegrande.vgmsuser.infraestructure.rest;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import reactor.core.publisher.Mono;

/**
 * Controlador para operaciones directas con Keycloak (mantenido para compatibilidad)
 * Para operaciones completas (Keycloak), usar UserManagementRest
 */
@RestController
@RequestMapping("/api/v1/keycloak")
@RequiredArgsConstructor
public class KeycloakRest {

    private final IKeycloakService keycloakService;

    @GetMapping("/users")
    public Mono<ResponseEntity<?>> findAllUsers() {
        return keycloakService.findAllUsers()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/users/search/{username}")
    public Mono<ResponseEntity<?>> findUserByUsername(@PathVariable String username) {
        return keycloakService.searchUserByUsername(username)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/users")
    public Mono<ResponseEntity<?>> createUser(@RequestBody User userDTO) {
        return keycloakService.createUser(userDTO)
                .map(response -> {
                    try {
                        return ResponseEntity.created(new URI("/api/v1/keycloak/users/")).body(response);
                    } catch (URISyntaxException e) {
                        return ResponseEntity.badRequest().body("Error creating URI: " + e.getMessage());
                    }
                });
    }

    @DeleteMapping("/users/{userId}")
    public Mono<ResponseEntity<?>> deleteUser(@PathVariable String userId) {
        return keycloakService.deleteUser(userId)
                .map(response -> ResponseEntity.noContent().build());
    }

    @PutMapping("/users/{userId}")
    public Mono<ResponseEntity<?>> updateUser(@PathVariable String userId, @RequestBody User userDTO) {
        return keycloakService.updateUser(userId, userDTO)
                .map(response -> ResponseEntity.noContent().build());
    }

}
