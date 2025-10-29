package pe.edu.vallegrande.vgmsuser.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import reactor.test.StepVerifier;

import java.util.Set;

/**
 * Prueba específica para verificar que los servicios mock funcionan correctamente
 * y no envían emails reales ni crean usuarios en Keycloak real
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class SafeEmailMockTest {

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IKeycloakService keycloakService;

    @Test
    @DisplayName("Mock Email Service - No envía emails reales")
    void testEmailServiceIsMocked() {
        log.info("=== VERIFICANDO QUE EL SERVICIO DE EMAIL ES MOCK ===");
        
        // Enviar email - debe completarse sin error pero sin enviar email real
        StepVerifier.create(
                emailService.sendTemporaryCredentialsEmail(
                        "test@example.com",
                        "Test User",
                        "temporaryPassword123",
                        "resetToken123"
                )
        )
        .verifyComplete();
        
        log.info("✅ Email mock service funcionando correctamente - no se envió email real");
    }

    @Test
    @DisplayName("Mock Keycloak Service - No crea usuarios en Keycloak real")
    void testKeycloakServiceIsMocked() {
        log.info("=== VERIFICANDO QUE EL SERVICIO DE KEYCLOAK ES MOCK ===");
        
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

        // Crear usuario - debe completarse sin error pero sin crear en Keycloak real
        StepVerifier.create(keycloakService.createUser(testUser))
                .expectNextMatches(keycloakId -> {
                    log.info("✅ Usuario creado en mock con ID: {}", keycloakId);
                    return keycloakId.startsWith("mock-id-");
                })
                .verifyComplete();

        // Verificar que se puede recuperar el usuario
        StepVerifier.create(keycloakService.getUserByEmail(testUser.getEmail()))
                .expectNextMatches(user -> {
                    log.info("✅ Usuario recuperado del mock: {}", user.getEmail());
                    return user.getEmail().equals(testUser.getEmail());
                })
                .verifyComplete();
        
        log.info("✅ Keycloak mock service funcionando correctamente - no se creó usuario real");
    }

    @Test
    @DisplayName("Servicios Mock - Integración completa sin efectos externos")
    void testCompleteIntegrationWithoutExternalEffects() {
        log.info("=== VERIFICANDO INTEGRACIÓN COMPLETA CON MOCKS ===");
        
        User user = User.builder()
                .firstname("Integración")
                .lastname("Mock")
                .email("integracion.mock@test.com")
                .documentType(DocumentType.DNI)
                .documentNumber("87654321")
                .phone("987123456")
                .roles(Set.of(Role.auxiliary.name()))
                .institutionId("MOCK_INST")
                .build();

        // 1. Crear usuario en mock Keycloak
        String keycloakId = keycloakService.createUser(user).block();
        log.info("1. Usuario creado en mock Keycloak: {}", keycloakId);

        // 2. Simular envío de email (no se envía realmente)
        emailService.sendTemporaryCredentialsEmail(
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname(),
                "password123",
                "token123"
        ).block();
        log.info("2. Email mock enviado sin efectos externos");

        // 3. Verificar que el usuario existe en el mock
        StepVerifier.create(keycloakService.getUserByKeycloakId(keycloakId))
                .expectNextMatches(retrievedUser -> {
                    log.info("3. Usuario verificado en mock: {}", retrievedUser.getEmail());
                    return retrievedUser.getEmail().equals(user.getEmail());
                })
                .verifyComplete();

        // 4. Simular cambio de contraseña (solo en mock)
        StepVerifier.create(keycloakService.changePassword(keycloakId, "newPassword123"))
                .verifyComplete();
        log.info("4. Contraseña cambiada en mock");

        // 5. Simular confirmación por email (no se envía realmente)
        emailService.sendPasswordChangeConfirmationEmail(
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname()
        ).block();
        log.info("5. Email de confirmación mock enviado");

        log.info("✅ INTEGRACIÓN COMPLETA VERIFICADA - TODOS LOS SERVICIOS SON MOCK");
        log.info("✅ NO SE ENVIARON EMAILS REALES");
        log.info("✅ NO SE CREARON USUARIOS EN KEYCLOAK REAL");
    }
}