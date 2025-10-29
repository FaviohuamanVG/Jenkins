package pe.edu.vallegrande.vgmsuser.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import java.util.Set;

/**
 * Pruebas de integración para gestión de perfiles y sincronización con Keycloak
 * Utiliza servicios mock para evitar llamadas reales a servicios externos
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Slf4j
class ProfileManagementIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IKeycloakService keycloakService;

    private User testUser;
    private String testKeycloakId;
    private final String USER_ID = "user-001";
    private final String USER_ROLES = "teacher";
    private final String INSTITUTION_ID = "INST001";
    private final String ADMIN_USER_ID = "admin-001";
    private final String ADMIN_ROLES = "admin";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .firstname("Carlos")
                .lastname("Rodríguez")
                .email("carlos.rodriguez@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("11223344")
                .phone("987111222")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        // Crear usuario de prueba en mock de Keycloak
        testKeycloakId = keycloakService.createUser(testUser).block();
        log.info("Profile test user created with keycloakId: {}", testKeycloakId);
    }

    @Test
    @DisplayName("IT201: Consulta exitosa de perfil propio")
    void testGetOwnProfile() {
        webTestClient.get()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", testKeycloakId)
                .header("X-User-Roles", USER_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(testUser.getEmail())
                .jsonPath("$.firstname").isEqualTo(testUser.getFirstname())
                .jsonPath("$.lastname").isEqualTo(testUser.getLastname());
    }

    @Test
    @DisplayName("IT202: Actualización exitosa de perfil propio")
    void testUpdateOwnProfile() {
        User updatedProfile = User.builder()
                .firstname("Carlos Alberto")
                .lastname(testUser.getLastname())
                .email(testUser.getEmail())
                .documentType(testUser.getDocumentType())
                .documentNumber(testUser.getDocumentNumber())
                .phone("987333444")
                .roles(testUser.getRoles())
                .institutionId(testUser.getInstitutionId())
                .build();

        webTestClient.put()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", testKeycloakId)
                .header("X-User-Roles", USER_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedProfile)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Profile updated successfully")
                .jsonPath("$.user.firstname").isEqualTo("Carlos Alberto")
                .jsonPath("$.user.phone").isEqualTo("987333444");
    }

    @Test
    @DisplayName("IT203: Error al actualizar perfil con email duplicado")
    void testUpdateProfile_DuplicateEmail() {
        // Crear otro usuario primero
        User anotherUser = User.builder()
                .firstname("Otro")
                .lastname("Usuario")
                .email("otro.usuario@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("99887766")
                .phone("987654321")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();
        
        String anotherKeycloakId = keycloakService.createUser(anotherUser).block();

        // Intentar actualizar el perfil del primer usuario con el email del segundo
        User profileUpdate = User.builder()
                .firstname(testUser.getFirstname())
                .lastname(testUser.getLastname())
                .email("otro.usuario@vallegrande.edu.pe") // Email ya existente
                .documentType(testUser.getDocumentType())
                .documentNumber(testUser.getDocumentNumber())
                .phone(testUser.getPhone())
                .roles(testUser.getRoles())
                .institutionId(testUser.getInstitutionId())
                .build();

        webTestClient.put()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", testKeycloakId)
                .header("X-User-Roles", USER_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(profileUpdate)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").value(org.hamcrest.Matchers.containsString("email"));
    }

    @Test
    @DisplayName("IT204: Error al actualizar perfil con formato de email inválido")
    void testUpdateProfile_InvalidEmailFormat() {
        User invalidUpdate = User.builder()
                .firstname(testUser.getFirstname())
                .lastname(testUser.getLastname())
                .email("email-invalido-sin-arroba") // Email sin formato válido
                .documentType(testUser.getDocumentType())
                .documentNumber(testUser.getDocumentNumber())
                .phone(testUser.getPhone())
                .roles(testUser.getRoles())
                .institutionId(testUser.getInstitutionId())
                .build();

        webTestClient.put()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", testKeycloakId)
                .header("X-User-Roles", USER_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUpdate)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("IT205: Consulta de usuarios por institución con filtros")
    void testGetUsersByInstitution_WithFilters() {
        // Crear algunos usuarios adicionales para filtrar
        User teacher2 = User.builder()
                .firstname("Teacher2")
                .lastname("Apellido")
                .email("teacher2@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("55667788")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();
        
        User auxiliary = User.builder()
                .firstname("Auxiliary")
                .lastname("Apellido")
                .email("auxiliary@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("33445566")
                .roles(Set.of(Role.auxiliary.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        keycloakService.createUser(teacher2).block();
        keycloakService.createUser(auxiliary).block();

        // Filtrar solo profesores
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/by-institution")
                        .queryParam("institutionId", INSTITUTION_ID)
                        .queryParam("role", Role.teacher.name().toUpperCase())
                        .build())
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2); // testUser + teacher2
    }

    @Test
    @DisplayName("IT206: Búsqueda de usuarios por nombre o email")
    void testSearchUsers() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/search")
                        .queryParam("query", "Carlos")
                        .build())
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].firstname").value(org.hamcrest.Matchers.containsString("Carlos"));
    }

    @Test
    @DisplayName("IT207: Cambio de estado de usuario (activar/desactivar)")
    void testChangeUserStatus() {
        // Desactivar usuario
        webTestClient.patch()
                .uri("/api/users/personal/status/" + testKeycloakId)
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("status", "INACTIVE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User status updated successfully")
                .jsonPath("$.user.status").isEqualTo("INACTIVE");

        // Activar usuario nuevamente
        webTestClient.patch()
                .uri("/api/users/personal/status/" + testKeycloakId)
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("status", "ACTIVE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.status").isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("IT208: Autorización por institución - acceso denegado")
    void testGetProfile_DifferentInstitution() {
        webTestClient.get()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", testKeycloakId)
                .header("X-User-Roles", USER_ROLES)
                .header("X-Institution-Id", "OTRA_INST") // Institución diferente
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("IT209: Sincronización de datos con Keycloak - actualización")
    void testKeycloakSync_UpdateUserStatus() {
        // Simular cambio directo en Keycloak
        keycloakService.updateUserStatus(testKeycloakId, UserStatus.I.name()).block();

        // Verificar que el cambio se refleja en las consultas
        webTestClient.get()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", testKeycloakId)
                .header("X-User-Roles", USER_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isForbidden(); // Usuario inactivo no puede acceder
    }

    @Test
    @DisplayName("IT210: Consulta masiva de usuarios con paginación")
    void testGetAllUsers_WithPagination() {
        // Crear varios usuarios para probar paginación
        for (int i = 1; i <= 5; i++) {
            User user = User.builder()
                    .firstname("User" + i)
                    .lastname("Apellido")
                    .email("user" + i + "@vallegrande.edu.pe")
                    .documentType(DocumentType.DNI)
                    .documentNumber("1000000" + i)
                    .roles(Set.of(Role.teacher.name()))
                    .institutionId(INSTITUTION_ID)
                    .build();
            keycloakService.createUser(user).block();
        }

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/all")
                        .queryParam("page", 0)
                        .queryParam("size", 3)
                        .build())
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(3)
                .jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(5))
                .jsonPath("$.totalPages").value(org.hamcrest.Matchers.greaterThan(1));
    }
}