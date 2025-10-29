package pe.edu.vallegrande.vgmsuser.integration;

import lombok.extern.slf4j.Slf4j;
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
 * Pruebas de integración end-to-end que validan flujos completos
 * desde la creación hasta la gestión completa de usuarios
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Slf4j
class EndToEndIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private final String ADMIN_USER_ID = "admin-001";
    private final String ADMIN_ROLES = "admin";
    private final String DIRECTOR_USER_ID = "director-001";
    private final String DIRECTOR_ROLES = "director";
    private final String INSTITUTION_ID = "INST001";

    @Test
    @DisplayName("E2E001: Flujo completo de creación y gestión de usuario")
    void testCompleteUserManagementFlow() {
        // 1. Crear usuario
        User newUser = User.builder()
                .firstname("Ana")
                .lastname("Silva")
                .email("ana.silva@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("66778899")
                .phone("987654321")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        Map<String, Object> keycloakResponse = webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String actualKeycloakId = ((Map<String, Object>) keycloakResponse.get("user")).get("keycloakId").toString();

        // 2. Verificar que el usuario fue creado correctamente
        webTestClient.get()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", actualKeycloakId)
                .header("X-User-Roles", Role.teacher.name())
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(newUser.getEmail())
                .jsonPath("$.passwordStatus").isEqualTo("TEMPORARY");

        // 3. Generar token de restablecimiento de contraseña
        String resetToken = webTestClient.post()
                .uri("/api/auth/password-reset/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("emailOrUsername", newUser.getEmail()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("token")
                .toString();

        // 4. Cambiar contraseña temporal
        webTestClient.post()
                .uri("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "token", resetToken,
                        "newPassword", "NuevaPassword123!"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Password reset successfully");

        // 5. Actualizar perfil del usuario
        User updatedProfile = User.builder()
                .firstname("Ana María")
                .lastname(newUser.getLastname())
                .email(newUser.getEmail())
                .documentType(newUser.getDocumentType())
                .documentNumber(newUser.getDocumentNumber())
                .phone("987111333")
                .roles(newUser.getRoles())
                .institutionId(newUser.getInstitutionId())
                .build();

        webTestClient.put()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", actualKeycloakId)
                .header("X-User-Roles", Role.teacher.name())
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedProfile)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.firstname").isEqualTo("Ana María");

        // 6. Verificar que el usuario aparece en la lista de la institución
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
                .jsonPath("$[?(@.email == '" + newUser.getEmail() + "')].firstname").isEqualTo("Ana María");
    }

    @Test
    @DisplayName("E2E002: Flujo de autorización jerárquica - Director y sus usuarios")
    void testHierarchicalAuthorizationFlow() {
        // 1. Director crea un profesor
        User teacher = User.builder()
                .firstname("Pedro")
                .lastname("Martínez")
                .email("pedro.martinez@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("44556677")
                .phone("987222333")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        Map<String, Object> teacherResponse = webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", DIRECTOR_USER_ID)
                .header("X-User-Roles", DIRECTOR_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(teacher)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String actualTeacherKeycloakId = ((Map<String, Object>) teacherResponse.get("user")).get("keycloakId").toString();

        // 2. Director intenta crear un administrador (debe fallar)
        User admin = User.builder()
                .firstname(teacher.getFirstname())
                .lastname(teacher.getLastname())
                .email("admin.intento@vallegrande.edu.pe")
                .documentType(teacher.getDocumentType())
                .documentNumber(teacher.getDocumentNumber())
                .phone(teacher.getPhone())
                .roles(Set.of(Role.admin.name()))
                .institutionId(teacher.getInstitutionId())
                .build();

        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", DIRECTOR_USER_ID)
                .header("X-User-Roles", DIRECTOR_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(admin)
                .exchange()
                .expectStatus().isForbidden();

        // 3. Director puede ver los usuarios de su institución
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/by-institution")
                        .queryParam("institutionId", INSTITUTION_ID)
                        .build())
                .header("X-User-Id", DIRECTOR_USER_ID)
                .header("X-User-Roles", DIRECTOR_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.email == '" + teacher.getEmail() + "')]").exists();

        // 4. Director puede desactivar usuarios de su institución
        webTestClient.patch()
                .uri("/api/users/personal/status/" + actualTeacherKeycloakId)
                .header("X-User-Id", DIRECTOR_USER_ID)
                .header("X-User-Roles", DIRECTOR_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "INACTIVE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.status").isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("E2E003: Flujo de manejo de errores y recuperación")
    void testErrorHandlingAndRecoveryFlow() {
        // 1. Intentar crear usuario con datos inválidos
        User invalidUser = User.builder()
                .firstname("") // Nombre vacío
                .lastname("TestErrores")
                .email("email-sin-formato-valido") // Email inválido
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

        // 2. Corregir datos y crear usuario exitosamente
        User validUser = User.builder()
                .firstname("Usuario")
                .lastname(invalidUser.getLastname())
                .email("usuario.corregido@vallegrande.edu.pe")
                .documentType(invalidUser.getDocumentType())
                .documentNumber("88997766")
                .phone("987123456")
                .roles(invalidUser.getRoles())
                .institutionId(invalidUser.getInstitutionId())
                .build();

        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validUser)
                .exchange()
                .expectStatus().isCreated();

        // 3. Intentar crear usuario duplicado
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validUser) // Mismo usuario
                .exchange()
                .expectStatus().isBadRequest();

        // 4. Intentar operaciones sin autorización adecuada
        webTestClient.get()
                .uri("/api/users/personal/all")
                .header("X-User-Id", "teacher-001")
                .header("X-User-Roles", Role.teacher.name()) // Teacher no puede ver todos los usuarios
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("E2E004: Flujo de gestión de contraseñas completo")
    void testCompletePasswordManagementFlow() {
        // 1. Crear usuario
        User user = User.builder()
                .firstname("Luis")
                .lastname("Password")
                .email("luis.password@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("55443322")
                .phone("987555444")
                .roles(Set.of(Role.auxiliary.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        Map<String, Object> userResponse = webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String actualKeycloakId = ((Map<String, Object>) userResponse.get("user")).get("keycloakId").toString();

        // 2. Verificar que tiene contraseña temporal
        webTestClient.get()
                .uri("/api/auth/password/status/" + actualKeycloakId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.passwordStatus").isEqualTo("TEMPORARY")
                .jsonPath("$.requiresChange").isEqualTo(true);

        // 3. Cambiar contraseña usando el método de force-change
        webTestClient.post()
                .uri("/api/auth/password/force-change")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "keycloakId", actualKeycloakId,
                        "currentPassword", user.getDocumentNumber(), // Contraseña temporal
                        "newPassword", "MiNuevaPassword456!"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.passwordStatus").isEqualTo("ACTIVE");

        // 4. Verificar que ya no requiere cambio de contraseña
        webTestClient.get()
                .uri("/api/auth/password/status/" + actualKeycloakId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.passwordStatus").isEqualTo("ACTIVE")
                .jsonPath("$.requiresChange").isEqualTo(false);

        // 5. Generar token de reset para cambio voluntario
        String resetToken = webTestClient.post()
                .uri("/api/auth/password-reset/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("emailOrUsername", user.getEmail()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody()
                .get("token")
                .toString();

        // 6. Usar token para cambiar contraseña nuevamente
        webTestClient.post()
                .uri("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "token", resetToken,
                        "newPassword", "OtraPassword789!"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Password reset successfully");
    }

    @Test
    @DisplayName("E2E005: Flujo de búsqueda y filtrado avanzado")
    void testAdvancedSearchAndFilteringFlow() {
        // 1. Crear varios usuarios con diferentes roles y características
        User[] users = {
                User.builder()
                        .firstname("María").lastname("Búsqueda")
                        .email("maria.busqueda@vallegrande.edu.pe")
                        .documentType(DocumentType.DNI)
                        .documentNumber("11223344")
                        .roles(Set.of(Role.teacher.name()))
                        .institutionId(INSTITUTION_ID)
                        .build(),
                User.builder()
                        .firstname("José").lastname("Filtro")
                        .email("jose.filtro@vallegrande.edu.pe")
                        .documentType(DocumentType.DNI)
                        .documentNumber("22334455")
                        .roles(Set.of(Role.auxiliary.name()))
                        .institutionId(INSTITUTION_ID)
                        .build(),
                User.builder()
                        .firstname("Carmen").lastname("Test")
                        .email("carmen.test@vallegrande.edu.pe")
                        .documentType(DocumentType.DNI)
                        .documentNumber("33445566")
                        .roles(Set.of(Role.secretary.name()))
                        .institutionId(INSTITUTION_ID)
                        .build()
        };

        // Crear todos los usuarios
        for (User userItem : users) {
            webTestClient.post()
                    .uri("/api/users/personal/create")
                    .header("X-User-Id", ADMIN_USER_ID)
                    .header("X-User-Roles", ADMIN_ROLES)
                    .header("X-Institution-Id", INSTITUTION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userItem)
                    .exchange()
                    .expectStatus().isCreated();
        }

        // 2. Búsqueda por nombre
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/search")
                        .queryParam("query", "María")
                        .build())
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.firstname == 'María')]").exists();

        // 3. Filtro por rol específico
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
                .jsonPath("$[*].role").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(Role.teacher.name().toUpperCase())));

        // 4. Búsqueda por email parcial
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/personal/search")
                        .queryParam("query", "filtro")
                        .build())
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.email =~ /.*filtro.*/)]").exists();
    }

    @Test
    @DisplayName("E2E006: Flujo de integración con validación de instituciones")
    void testInstitutionValidationIntegrationFlow() {
        // Crear usuario para una institución específica
        User userWithInstitution = User.builder()
                .firstname("Institución")
                .lastname("Validada")
                .email("institucion.validada@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("77889900")
                .phone("987777888")
                .roles(Set.of(Role.teacher.name()))
                .institutionId("INST002") // Institución diferente
                .build();

        // El sistema debe validar que la institución existe
        webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", "INST002")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userWithInstitution)
                .exchange()
                .expectStatus().isCreated(); // En modo mock, todas las instituciones son válidas
    }

    @Test
    @DisplayName("E2E007: Flujo de eliminación y reactivación de usuarios")
    void testUserDeletionAndReactivationFlow() {
        // 1. Crear usuario
        User userToDelete = User.builder()
                .firstname("Usuario")
                .lastname("Eliminar")
                .email("usuario.eliminar@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("99887766")
                .phone("987999888")
                .roles(Set.of(Role.auxiliary.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        Map<String, Object> deleteResponse = webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userToDelete)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String actualKeycloakId = ((Map<String, Object>) deleteResponse.get("user")).get("keycloakId").toString();

        // 2. Desactivar usuario (soft delete)
        webTestClient.patch()
                .uri("/api/users/personal/status/" + actualKeycloakId)
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "INACTIVE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.status").isEqualTo("INACTIVE");

        // 3. Verificar que usuario inactivo no puede acceder
        webTestClient.get()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", actualKeycloakId)
                .header("X-User-Roles", Role.auxiliary.name())
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isForbidden();

        // 4. Reactivar usuario
        webTestClient.patch()
                .uri("/api/users/personal/status/" + actualKeycloakId)
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "ACTIVE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.status").isEqualTo("ACTIVE");

        // 5. Verificar que usuario puede acceder nuevamente
        webTestClient.get()
                .uri("/api/users/personal/profile")
                .header("X-User-Id", actualKeycloakId)
                .header("X-User-Roles", Role.auxiliary.name())
                .header("X-Institution-Id", INSTITUTION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(userToDelete.getEmail());
    }

    @Test
    @DisplayName("E2E008: Flujo de concurrencia y consistencia de datos")
    void testConcurrencyAndDataConsistencyFlow() {
        // Crear usuario base
        User baseUser = User.builder()
                .firstname("Concurrencia")
                .lastname("Test")
                .email("concurrencia.test@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("12345987")
                .phone("987123987")
                .roles(Set.of(Role.teacher.name()))
                .institutionId(INSTITUTION_ID)
                .build();

        Map<String, Object> concurrencyResponse = webTestClient.post()
                .uri("/api/users/personal/create")
                .header("X-User-Id", ADMIN_USER_ID)
                .header("X-User-Roles", ADMIN_ROLES)
                .header("X-Institution-Id", INSTITUTION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(baseUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        String actualKeycloakId = ((Map<String, Object>) concurrencyResponse.get("user")).get("keycloakId").toString();

        // Verificar consistencia después de múltiples operaciones
        for (int i = 0; i < 3; i++) {
            // Actualizar perfil
            User updatedUser = User.builder()
                    .firstname("Concurrencia" + i)
                    .lastname(baseUser.getLastname())
                    .email(baseUser.getEmail())
                    .documentType(baseUser.getDocumentType())
                    .documentNumber(baseUser.getDocumentNumber())
                    .phone("987123" + (987 + i))
                    .roles(baseUser.getRoles())
                    .institutionId(baseUser.getInstitutionId())
                    .build();

            webTestClient.put()
                    .uri("/api/users/personal/profile")
                    .header("X-User-Id", actualKeycloakId)
                    .header("X-User-Roles", Role.teacher.name())
                    .header("X-Institution-Id", INSTITUTION_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updatedUser)
                    .exchange()
                    .expectStatus().isOk();

            // Verificar que los cambios persisten
            webTestClient.get()
                    .uri("/api/users/personal/profile")
                    .header("X-User-Id", actualKeycloakId)
                    .header("X-User-Roles", Role.teacher.name())
                    .header("X-Institution-Id", INSTITUTION_ID)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.firstname").isEqualTo("Concurrencia" + i);
        }
    }
}