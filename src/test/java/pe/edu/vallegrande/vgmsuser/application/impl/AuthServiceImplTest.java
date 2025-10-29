package pe.edu.vallegrande.vgmsuser.application.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias para AuthServiceImpl
 * Usa mocks completos - NO envía emails reales ni conecta con Keycloak real
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Pruebas Unitarias")
class AuthServiceImplTest {

    @Mock
    private IKeycloakService keycloakService;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private KeycloakUserDto mockKeycloakUser;
    private final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";
    private final String TEST_RESET_TOKEN = "test-reset-token-456";

    @BeforeEach
    void setUp() {
        mockKeycloakUser = KeycloakUserDto.builder()
                .keycloakId(TEST_KEYCLOAK_ID)
                .username("test.user")
                .firstname("Test")
                .lastname("User")
                .email("test.user@vallegrande.edu.pe")
                .status(UserStatus.A)  // Usar A en lugar de ACTIVE
                .passwordStatus(PasswordStatus.TEMPORARY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("UT006: Debe generar token de reset de contraseña exitosamente")
    void testGeneratePasswordResetToken_Success() {
        // Given - Configurar mocks
        when(keycloakService.getUserByKeycloakId(TEST_KEYCLOAK_ID))
                .thenReturn(Mono.just(mockKeycloakUser));
        
        when(keycloakService.updatePasswordResetToken(eq(TEST_KEYCLOAK_ID), anyString()))
                .thenReturn(Mono.empty());
        
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When
        Mono<String> result = authService.generatePasswordResetToken(TEST_KEYCLOAK_ID);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(token -> {
                    // Verificar que el token es un UUID válido
                    try {
                        UUID.fromString(token);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .verifyComplete();

        // Verificar interacciones
        verify(keycloakService, times(1)).getUserByKeycloakId(TEST_KEYCLOAK_ID);
        verify(keycloakService, times(1)).updatePasswordResetToken(eq(TEST_KEYCLOAK_ID), anyString());
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq(mockKeycloakUser.getEmail()),
                eq(mockKeycloakUser.getUsername()),
                anyString()
        );
    }

    @Test
    @DisplayName("UT007: Debe generar token exitosamente para usuario válido adicional")
    void testGeneratePasswordResetToken_AnotherSuccessCase() {
        // Given - Usuario válido con datos diferentes
        KeycloakUserDto anotherUser = KeycloakUserDto.builder()
                .keycloakId("another-user-id")
                .username("maria.rodriguez")
                .email("maria.rodriguez@vallegrande.edu.pe")
                .build();
        
        when(keycloakService.getUserByKeycloakId("another-user-id"))
                .thenReturn(Mono.just(anotherUser));
        when(keycloakService.updatePasswordResetToken(eq("another-user-id"), anyString()))
                .thenReturn(Mono.empty());
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(authService.generatePasswordResetToken("another-user-id"))
                .expectNextMatches(token -> token != null && !token.isEmpty())
                .verifyComplete();

        // Verificar interacciones exitosas
        verify(keycloakService, times(1)).getUserByKeycloakId("another-user-id");
        verify(keycloakService, times(1)).updatePasswordResetToken(eq("another-user-id"), anyString());
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq(anotherUser.getEmail()),
                eq(anotherUser.getUsername()),
                anyString()
        );
    }

    @Test
    @DisplayName("UT008: Debe procesar múltiples tokens exitosamente")
    void testGeneratePasswordResetToken_MultipleSuccess() {
        // Given - Configurar para múltiples usuarios exitosos
        when(keycloakService.getUserByKeycloakId(anyString()))
                .thenReturn(Mono.just(mockKeycloakUser));
        
        when(keycloakService.updatePasswordResetToken(anyString(), anyString()))
                .thenReturn(Mono.empty());
        
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When & Then - Generar múltiples tokens
        StepVerifier.create(authService.generatePasswordResetToken("user1"))
                .expectNextMatches(token -> token != null && !token.isEmpty())
                .verifyComplete();

        StepVerifier.create(authService.generatePasswordResetToken("user2"))
                .expectNextMatches(token -> token != null && !token.isEmpty())
                .verifyComplete();

        // Verificar que se procesaron ambos usuarios
        verify(keycloakService, times(2)).getUserByKeycloakId(anyString());
        verify(emailService, times(2)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UT009: Debe resetear contraseña exitosamente con token válido")
    void testResetPassword_Success() {
        // Given - Configurar mocks
        String validToken = "valid-reset-token-789";
        String newPassword = "NewSecurePassword123!";
        
        KeycloakUserDto userWithToken = KeycloakUserDto.builder()
                .keycloakId("user-with-token-id")
                .username("user.with.token")
                .email("user.with.token@vallegrande.edu.pe")
                .passwordResetToken(validToken)
                .status(UserStatus.A)  // Usar A en lugar de ACTIVE
                .build();

        when(keycloakService.getAllUsersWithAttributes())
                .thenReturn(Flux.just(userWithToken));
        
        when(keycloakService.changePassword(userWithToken.getKeycloakId(), newPassword))
                .thenReturn(Mono.empty());
        
        when(keycloakService.updatePasswordStatus(eq(userWithToken.getKeycloakId()), anyString(), anyString()))
                .thenReturn(Mono.empty());
        
        when(keycloakService.updatePasswordResetToken(userWithToken.getKeycloakId(), null))
                .thenReturn(Mono.empty());
        
        when(emailService.sendPasswordChangeConfirmationEmail(anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When
        Mono<String> result = authService.resetPassword(validToken, newPassword);

        // Then
        StepVerifier.create(result)
                .expectNext("Contraseña cambiada exitosamente")
                .verifyComplete();

        // Verificar interacciones
        verify(keycloakService, times(1)).getAllUsersWithAttributes();
        verify(keycloakService, times(1)).changePassword(userWithToken.getKeycloakId(), newPassword);
        verify(keycloakService, times(1)).updatePasswordStatus(eq(userWithToken.getKeycloakId()), anyString(), anyString());
        verify(keycloakService, times(1)).updatePasswordResetToken(userWithToken.getKeycloakId(), null);
        verify(emailService, times(1)).sendPasswordChangeConfirmationEmail(
                eq(userWithToken.getEmail()),
                eq(userWithToken.getUsername())
        );
    }

    @Test
    @DisplayName("UT010: Debe fallar cuando el token de reset es inválido")
    void testResetPassword_InvalidToken() {
        // Given - Token inválido
        String invalidToken = "invalid-token-123";
        String newPassword = "NewPassword123!";
        
        KeycloakUserDto userWithDifferentToken = KeycloakUserDto.builder()
                .keycloakId("other-user-id")
                .username("other.user")
                .passwordResetToken("different-token-456")
                .build();

        when(keycloakService.getAllUsersWithAttributes())
                .thenReturn(Flux.just(userWithDifferentToken));

        // When & Then
        StepVerifier.create(authService.resetPassword(invalidToken, newPassword))
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Token inválido o expirado")
                )
                .verify();

        // Verificar que NO se cambió contraseña
        verify(keycloakService, never()).changePassword(anyString(), anyString());
        verify(keycloakService, never()).updatePasswordResetToken(anyString(), isNull());
        verify(emailService, never()).sendPasswordChangeConfirmationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("UT011: Debe manejar error al cambiar contraseña en Keycloak")
    void testResetPassword_KeycloakPasswordChangeFailure() {
        // Given - Cambio de contraseña falla en Keycloak
        String validToken = "valid-token-111";
        String newPassword = "NewPassword123!";
        
        KeycloakUserDto userWithToken = KeycloakUserDto.builder()
                .keycloakId("user-password-fail-id")
                .username("user.password.fail")
                .email("user.fail@vallegrande.edu.pe")
                .passwordResetToken(validToken)
                .build();

        when(keycloakService.getAllUsersWithAttributes())
                .thenReturn(Flux.just(userWithToken));
        
        when(keycloakService.changePassword(userWithToken.getKeycloakId(), newPassword))
                .thenReturn(Mono.error(new RuntimeException("Keycloak password policy violation")));

        // When & Then
        StepVerifier.create(authService.resetPassword(validToken, newPassword))
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Keycloak password policy violation")
                )
                .verify();

        // Verificar que NO se limpió el token ni se envió confirmación
        verify(keycloakService, never()).updatePasswordResetToken(anyString(), isNull());
        verify(emailService, never()).sendPasswordChangeConfirmationEmail(anyString(), anyString());
    }
}