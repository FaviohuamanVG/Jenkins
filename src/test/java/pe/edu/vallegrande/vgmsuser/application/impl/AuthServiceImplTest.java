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
    private final String TEST_KEYCLOAK_ID = UUID.randomUUID().toString();
    private final String TEST_RESET_TOKEN = UUID.randomUUID().toString();

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
        String anotherUserId = UUID.randomUUID().toString();
        KeycloakUserDto anotherUser = KeycloakUserDto.builder()
                .keycloakId(anotherUserId)
                .username("maria.rodriguez")
                .email("maria.rodriguez@vallegrande.edu.pe")
                .build();
        
        when(keycloakService.getUserByKeycloakId(anotherUserId))
                .thenReturn(Mono.just(anotherUser));
        when(keycloakService.updatePasswordResetToken(eq(anotherUserId), anyString()))
                .thenReturn(Mono.empty());
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(authService.generatePasswordResetToken(anotherUserId))
                .expectNextMatches(token -> token != null && !token.isEmpty())
                .verifyComplete();

        // Verificar interacciones exitosas
        verify(keycloakService, times(1)).getUserByKeycloakId(anotherUserId);
        verify(keycloakService, times(1)).updatePasswordResetToken(eq(anotherUserId), anyString());
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
        String userId1 = UUID.randomUUID().toString();
        String userId2 = UUID.randomUUID().toString();
        
        StepVerifier.create(authService.generatePasswordResetToken(userId1))
                .expectNextMatches(token -> token != null && !token.isEmpty())
                .verifyComplete();

        StepVerifier.create(authService.generatePasswordResetToken(userId2))
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
        String validToken = UUID.randomUUID().toString();
        String newPassword = "NewSecurePassword123!";
        String userTokenId = UUID.randomUUID().toString();
        
        KeycloakUserDto userWithToken = KeycloakUserDto.builder()
                .keycloakId(userTokenId)
                .username("user.with.token")
                .email("user.with.token@vallegrande.edu.pe")
                .passwordResetToken(validToken)
                .status(UserStatus.A)  // Usar A en lugar de ACTIVE
                .build();

        when(keycloakService.getAllUsersWithAttributes())
                .thenReturn(Flux.just(userWithToken));
        
        when(keycloakService.changePassword(userTokenId, newPassword))
                .thenReturn(Mono.empty());
        
        when(keycloakService.updatePasswordStatus(eq(userTokenId), anyString(), anyString()))
                .thenReturn(Mono.empty());
        
        when(keycloakService.updatePasswordResetToken(userTokenId, null))
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
        verify(keycloakService, times(1)).changePassword(userTokenId, newPassword);
        verify(keycloakService, times(1)).updatePasswordStatus(eq(userTokenId), anyString(), anyString());
        verify(keycloakService, times(1)).updatePasswordResetToken(userTokenId, null);
        verify(emailService, times(1)).sendPasswordChangeConfirmationEmail(
                eq(userWithToken.getEmail()),
                eq(userWithToken.getUsername())
        );
    }

    @Test
    @DisplayName("UT010: Debe fallar cuando el token de reset es inválido")
    void testResetPassword_InvalidToken() {
        // Given - Token inválido
        String invalidToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        String otherUserId = UUID.randomUUID().toString();
        
        KeycloakUserDto userWithDifferentToken = KeycloakUserDto.builder()
                .keycloakId(otherUserId)
                .username("other.user")
                .passwordResetToken(UUID.randomUUID().toString())
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
        String validToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        String userFailId = UUID.randomUUID().toString();
        
        KeycloakUserDto userWithToken = KeycloakUserDto.builder()
                .keycloakId(userFailId)
                .username("user.password.fail")
                .email("user.fail@vallegrande.edu.pe")
                .passwordResetToken(validToken)
                .build();

        when(keycloakService.getAllUsersWithAttributes())
                .thenReturn(Flux.just(userWithToken));
        
        when(keycloakService.changePassword(userFailId, newPassword))
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