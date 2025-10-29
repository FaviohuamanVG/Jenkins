package pe.edu.vallegrande.vgmsuser.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.config.TestMockConfig;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import reactor.test.StepVerifier;

import java.util.Set;

/**
 * Prueba simple para verificar que los servicios mock funcionan correctamente
 * y no realizan llamadas externas reales
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMockConfig.class)
@Slf4j
class MockServicesVerificationTest {

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IKeycloakService keycloakService;

    @Test
    @DisplayName("Verificación básica - Servicios Mock funcionando")
    void testMockServicesBasicFunctionality() {
        log.info("=== INICIANDO VERIFICACIÓN DE SERVICIOS MOCK ===");
        
        // 1. Verificar que el servicio de email es mock
        log.info("Verificando servicio de email mock...");
        StepVerifier.create(
                emailService.sendTemporaryCredentialsEmail(
                        "test@example.com",
                        "Test User",
                        "tempPassword123",
                        "resetToken123"
                )
        )
        .verifyComplete();
        
        log.info("✅ Email mock service funcionando correctamente");

        // 2. Verificar que el servicio de Keycloak es mock
        log.info("Verificando servicio de Keycloak mock...");
        
        User testUser = User.builder()
                .firstname("Mock")
                .lastname("User")
                .email("mock.user@test.com")
                .documentType(DocumentType.DNI)
                .documentNumber("12345678")
                .phone("987654321")
                .roles(Set.of(Role.teacher.name()))
                .institutionId("TEST_INST")
                .build();

        // Crear usuario en mock
        StepVerifier.create(keycloakService.createUser(testUser))
                .expectNextMatches(keycloakId -> {
                    log.info("Usuario creado en mock con ID: {}", keycloakId);
                    return keycloakId.startsWith("mock-id-");
                })
                .verifyComplete();

        log.info("✅ Keycloak mock service funcionando correctamente");

        // 3. Verificar que se puede recuperar el usuario del mock
        log.info("Verificando recuperación de usuario del mock...");
        StepVerifier.create(keycloakService.getUserByEmail(testUser.getEmail()))
                .expectNextMatches(user -> {
                    log.info("Usuario recuperado del mock: {}", user.getEmail());
                    return user.getEmail().equals(testUser.getEmail());
                })
                .verifyComplete();

        log.info("✅ VERIFICACIÓN COMPLETA: TODOS LOS SERVICIOS SON MOCK");
        log.info("✅ NO SE REALIZARON LLAMADAS EXTERNAS REALES");
    }

    @Test
    @DisplayName("Verificación avanzada - Interacción entre servicios mock")
    void testMockServicesIntegration() {
        log.info("=== VERIFICANDO INTERACCIÓN ENTRE SERVICIOS MOCK ===");
        
        User user = User.builder()
                .firstname("Integración")
                .lastname("Test")
                .email("integration.test@mock.com")
                .documentType(DocumentType.DNI)
                .documentNumber("87654321")
                .phone("987123456")
                .roles(Set.of(Role.auxiliary.name()))
                .institutionId("MOCK_INSTITUTION")
                .build();

        // Flujo completo mock
        String keycloakId = keycloakService.createUser(user)
                .doOnNext(id -> log.info("1. Usuario creado en mock Keycloak: {}", id))
                .block();

        // Enviar email mock
        emailService.sendTemporaryCredentialsEmail(
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname(),
                "password123",
                "token123"
        )
        .doOnSuccess(v -> log.info("2. Email mock enviado exitosamente"))
        .block();

        // Verificar usuario existe en mock
        StepVerifier.create(keycloakService.getUserByKeycloakId(keycloakId))
                .expectNextMatches(retrievedUser -> {
                    log.info("3. Usuario verificado en mock: {}", retrievedUser.getEmail());
                    return retrievedUser.getEmail().equals(user.getEmail());
                })
                .verifyComplete();

        // Cambiar contraseña en mock
        StepVerifier.create(keycloakService.changePassword(keycloakId, "newPassword123"))
                .verifyComplete();
        log.info("4. Contraseña cambiada en mock");

        // Enviar confirmación por email mock
        emailService.sendPasswordChangeConfirmationEmail(
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname()
        )
        .doOnSuccess(v -> log.info("5. Email de confirmación mock enviado"))
        .block();

        log.info("✅ INTEGRACIÓN MOCK COMPLETA VERIFICADA");
        log.info("✅ FLUJO END-TO-END SIN SERVICIOS EXTERNOS");
    }
}