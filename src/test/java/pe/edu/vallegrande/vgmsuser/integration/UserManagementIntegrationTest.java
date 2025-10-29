package pe.edu.vallegrande.vgmsuser.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import java.util.Set;
import java.util.Map;

/**
 * Pruebas de integración para la gestión de usuarios con datos mock
 * No envía emails reales ni crea usuarios en Keycloak real
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Slf4j
class UserManagementIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String ADMIN_USER_ID = "admin-001";
    private final String ADMIN_ROLES = "admin";
    private final String INSTITUTION_ID = "INST001";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan.perez@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("12345678")
                .phone("987654321")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();
    }

    @Test
    @DisplayName("IT001: Creación exitosa de usuario por administrador")
    void testCreateStaffUser_Success() {
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User created successfully")
                .jsonPath("$.user.email").isEqualTo(testUser.getEmail())
                .jsonPath("$.user.firstname").isEqualTo(testUser.getFirstname())
                .jsonPath("$.user.lastname").isEqualTo(testUser.getLastname())
                .jsonPath("$.user.status").isEqualTo("ACTIVE")
                .jsonPath("$.user.keycloakId").isNotEmpty();
    }

    @Test
    @DisplayName("IT002: Creación exitosa de usuario por director")
    void testCreateStaffUser_ByDirector() {
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", "director-001")
                .header("X-User-Roles", "director")
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User created successfully")
                .jsonPath("$.user.email").isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("IT003: Validación de headers de autorización requeridos")
    void testCreateStaffUser_MissingHeaders() {
        webTestClient.post()
                .uri("/api/users/personal/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("IT004: Manejo de usuarios duplicados")
    void testCreateStaffUser_DuplicateUser() {
        // Primero crear el usuario
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated();

        // Intentar crear el mismo usuario nuevamente
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("IT005: Validación de datos de entrada inválidos")
    void testCreateStaffUser_InvalidData() {
        User invalidUser = User.builder()
                .firstname("") // Nombre vacío
                .lastname("")  // Apellido vacío
                .email("email-invalid") // Email inválido
                .documentType(DocumentType.DNI)
                .documentNumber("123") // DNI muy corto
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUser)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("IT006: Director no puede crear administradores")
    void testCreateAdminUser_UnauthorizedRole() {
        User adminUser = User.builder()
                .firstname(testUser.getFirstname())
                .lastname(testUser.getLastname())
                .email("admin.test@vallegrande.edu.pe")
                .documentType(testUser.getDocumentType())
                .documentNumber(testUser.getDocumentNumber())
                .phone(testUser.getPhone())
                .roles(Set.of(Role.admin.name()))
                .institutionId(testUser.getInstitutionId())
                .build();

        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", "director-001")
                .header("X-User-Roles", "director")
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(adminUser)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("IT007: Consulta de usuarios por institución")
    void testGetUsersByInstitution() {
        // Primero crear algunos usuarios
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated();

        // Luego consultar usuarios de la institución
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/by-institution")
                        .queryParam("institutionId", INSTITUTION_ID)
                        .build())
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].email").isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("IT008: Actualización de usuario existente")
    void testUpdateStaffUser() {
        // Primero crear el usuario
        Map<String, Object> createResponse = webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String actualKeycloakId = ((Map<String, Object>) createResponse.get("user")).get("keycloakId").toString();

        // Actualizar el usuario
        User updatedUser = User.builder()
                .firstname("Juan Carlos")
                .lastname(testUser.getLastname())
                .email(testUser.getEmail())
                .documentType(testUser.getDocumentType())
                .documentNumber(testUser.getDocumentNumber())
                .phone("999888777")
                .roles(testUser.getRoles())
                .institutionId(testUser.getInstitutionId())
                .build();

        webTestClient.put()
                .uri("/api/users/personal/update/" + actualKeycloakId)
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.firstname").isEqualTo("Juan Carlos")
                .jsonPath("$.user.phone").isEqualTo("999888777");
    }
}