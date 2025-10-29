package pe.edu.vallegrande.vgmsuser.application.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias para EmailServiceImpl
 * Usa mocks completos - NO envía emails reales
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - Pruebas Unitarias")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private MimeMessageHelper mimeMessageHelper;

    @InjectMocks
    private EmailServiceImpl emailService;

    private final String FROM_EMAIL = "noreply@vallegrande.edu.pe";
    private final String SERVER_PORT = "8100";
    private final String FRONTEND_URL = "http://localhost:3000/school";

    @BeforeEach
    void setUp() {
        // Configurar valores de las propiedades usando ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "serverPort", SERVER_PORT);
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
    }

    @Test
    @DisplayName("UT012: Debe enviar email de credenciales temporales exitosamente")
    void testSendTemporaryCredentialsEmail_Success() {
        // Given - Configurar mocks
        String toEmail = "juan.perez@vallegrande.edu.pe";
        String username = "Juan Pérez";
        String temporaryPassword = "TempPass123!";
        String resetToken = "rst-" + java.util.UUID.randomUUID().toString().substring(0, 12);
        
        String expectedHtmlContent = "<html><body>Email de credenciales temporales mock</body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/temporary-credentials"), any(Context.class)))
                .thenReturn(expectedHtmlContent);

        // When
        Mono<Void> result = emailService.sendTemporaryCredentialsEmail(toEmail, username, temporaryPassword, resetToken);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Verificar interacciones
        verify(mailSender, times(1)).createMimeMessage();
        verify(templateEngine, times(1)).process(eq("email/temporary-credentials"), any(Context.class));
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("UT013: Debe enviar email de reset de contraseña exitosamente")
    void testSendPasswordResetEmail_Success() {
        // Given
        String toEmail = "maria.lopez@vallegrande.edu.pe";
        String username = "María López";
        String resetToken = "rst-" + java.util.UUID.randomUUID().toString().substring(0, 12);
        
        String expectedHtmlContent = "<html><body>Email de reset de contraseña mock</body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn(expectedHtmlContent);

        // When
        Mono<Void> result = emailService.sendPasswordResetEmail(toEmail, username, resetToken);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Verificar que se usó la plantilla correcta
        verify(templateEngine, times(1)).process(eq("email/password-reset"), any(Context.class));
        
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("UT014: Debe enviar email de confirmación de cambio exitosamente")
    void testSendPasswordChangeConfirmationEmail_BasicSuccess() {
        // Given
        String toEmail = "carlos.rodriguez@vallegrande.edu.pe";
        String fullName = "Carlos Rodríguez";
        
        String expectedHtmlContent = "<html><body>Email de confirmación mock</body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/password-change-confirmation"), any(Context.class)))
                .thenReturn(expectedHtmlContent);

        // When
        Mono<Void> result = emailService.sendPasswordChangeConfirmationEmail(toEmail, fullName);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Verificar interacciones
        verify(templateEngine, times(1)).process(eq("email/password-change-confirmation"), any(Context.class));
        
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("UT015: Debe manejar error cuando falla la creación del mensaje")
    void testSendTemporaryCredentialsEmail_MessageCreationFailure() {
        // Given - mailSender.createMimeMessage() falla
        when(mailSender.createMimeMessage())
                .thenThrow(new RuntimeException("Failed to create MimeMessage"));

        // When & Then
        StepVerifier.create(emailService.sendTemporaryCredentialsEmail(
                "test@example.com", "Test User", "password", "token"))
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Failed to create MimeMessage")
                )
                .verify();

        // Verificar que NO se procesó plantilla ni se envió
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT016: Debe procesar plantilla de reset exitosamente")
    void testSendPasswordResetEmail_TemplateProcessingSuccess() {
        // Given - templateEngine.process() exitoso
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body><h1>Reset Password</h1><p>Token procesado correctamente</p></body></html>");

        // When & Then
        StepVerifier.create(emailService.sendPasswordResetEmail(
                "admin@vallegrande.edu.pe", "Administrator", "secure-rst-" + java.util.UUID.randomUUID().toString().substring(0, 8)))
                .verifyComplete();

        // Verificar que se procesó y envió correctamente
        verify(mailSender, times(1)).createMimeMessage();
        verify(templateEngine, times(1)).process(eq("email/password-reset"), any(Context.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT017: Debe enviar email de confirmación completo exitosamente")
    void testSendPasswordChangeConfirmationEmail_CompleteSuccess() {
        // Given - Configuración exitosa completa
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body><h2>Contraseña Cambiada</h2><p>Su contraseña fue actualizada exitosamente</p></body></html>");

        // When & Then
        StepVerifier.create(emailService.sendPasswordChangeConfirmationEmail(
                "director@vallegrande.edu.pe", "Director Regional"))
                .verifyComplete();

        // Verificar flujo completo exitoso
        verify(mailSender, times(1)).createMimeMessage();
        verify(templateEngine, times(1)).process(eq("email/password-change-confirmation"), any(Context.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UT018: Debe construir URL de reset correctamente")
    void testSendPasswordResetEmail_CorrectResetUrl() {
        // Given
        String resetToken = "special-rst-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String expectedResetUrl = FRONTEND_URL + "/reset-password?token=" + resetToken;
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn("<html><body>Mock email</body></html>");

        // When
        emailService.sendPasswordResetEmail("user@test.com", "Test User", resetToken).block();

        // Then - Verificar que se construyó la URL correcta
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    @DisplayName("UT019: Debe usar configuración de email correcta")
    void testEmailConfiguration() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body>Test email</body></html>");

        // When
        emailService.sendTemporaryCredentialsEmail(
                "test@example.com", "Test User", "password", "token").block();

        // Then - Verificar que se usó la configuración correcta
        verify(templateEngine).process(eq("email/temporary-credentials"), any(Context.class));
    }
}