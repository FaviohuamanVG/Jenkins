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
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import pe.edu.vallegrande.vgmsuser.infraestructure.util.KeycloakProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias para UserManagementServiceImpl
 * Usa mocks completos - NO envía emails reales ni crea usuarios en Keycloak
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementService - Pruebas Unitarias")
class UserManagementServiceImplTest {

    @Mock
    private IKeycloakService keycloakService;

    @Mock
    private IEmailService emailService;

    @Mock
    private KeycloakProvider keycloakProvider;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private User testUser;
    private KeycloakUserDto mockKeycloakUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("juan.perez")
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan.perez@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("12345678")
                .phone("987654321")
                .roles(Set.of("teacher"))
                .institutionId("INST001")
                .build();

        mockKeycloakUser = KeycloakUserDto.builder()
                .keycloakId("mock-keycloak-id-123")
                .username("juan.perez")
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan.perez@vallegrande.edu.pe")
                .status(UserStatus.A)  // Usar A en lugar de ACTIVE
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("UT001: Debe crear usuario completo exitosamente con roles válidos")
    void testCreateCompleteUser_Success() {
        // Given - Configurar mocks
        String expectedKeycloakId = "mock-keycloak-id-123";
        String expectedPassword = "temp-password-123";
        String expectedToken = "reset-token-456";

        when(keycloakService.createUser(any(User.class)))
                .thenReturn(Mono.just(expectedKeycloakId));
        
        when(keycloakService.getUserByKeycloakId(expectedKeycloakId))
                .thenReturn(Mono.just(mockKeycloakUser));
        
        when(emailService.sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When - Ejecutar método
        Mono<KeycloakUserDto> result = userManagementService.createCompleteUser(testUser);

        // Then - Verificar resultado
        StepVerifier.create(result)
                .expectNextMatches(user -> {
                    return user.getKeycloakId().equals(expectedKeycloakId) &&
                           user.getUsername().equals("juan.perez") &&
                           user.getEmail().equals("juan.perez@vallegrande.edu.pe") &&
                           user.getStatus() == UserStatus.A;  // Usar A en lugar de ACTIVE
                })
                .verifyComplete();

        // Verificar interacciones con mocks
        verify(keycloakService, times(1)).createUser(any(User.class));
        verify(keycloakService, times(1)).getUserByKeycloakId(expectedKeycloakId);
        verify(emailService, times(1)).sendTemporaryCredentialsEmail(
                eq(testUser.getEmail()),
                anyString(),
                anyString(),
                anyString()
        );
        
        // Verificar que NO se llamaron servicios reales
        verifyNoMoreInteractions(keycloakService, emailService);
    }

    @Test
    @DisplayName("UT002: Debe asignar rol 'teacher' por defecto cuando no se proporcionan roles")
    void testCreateCompleteUser_DefaultRole() {
        // Given - Usuario sin roles
        User userWithoutRoles = User.builder()
                .username("maria.lopez")
                .firstname("María")
                .lastname("López")
                .email("maria.lopez@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("87654321")
                .roles(null) // Sin roles
                .institutionId("INST001")
                .build();

        when(keycloakService.createUser(any(User.class)))
                .thenReturn(Mono.just("mock-keycloak-id-456"));
        
        when(keycloakService.getUserByKeycloakId(anyString()))
                .thenReturn(Mono.just(mockKeycloakUser));
        
        when(emailService.sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When
        Mono<KeycloakUserDto> result = userManagementService.createCompleteUser(userWithoutRoles);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getKeycloakId().equals("mock-keycloak-id-456"))
                .verifyComplete();

        // Verificar que se asignó el rol por defecto
        verify(keycloakService).createUser(argThat(user -> 
                user.getRoles() != null && 
                user.getRoles().contains("teacher") &&
                user.getRoles().size() == 1
        ));
    }

    @Test
    @DisplayName("UT003: Debe fallar cuando se proporcionan roles inválidos")
    void testCreateCompleteUser_InvalidRoles() {
        // Given - Usuario con roles inválidos
        User userWithInvalidRoles = User.builder()
                .username("admin.user")
                .firstname("Admin")
                .lastname("User")
                .email("admin@vallegrande.edu.pe")
                .documentType(DocumentType.DNI)
                .documentNumber("11111111")
                .roles(Set.of("admin", "superuser")) // Roles no permitidos
                .institutionId("INST001")
                .build();

        // When & Then - Debe fallar con excepción
        StepVerifier.create(userManagementService.createCompleteUser(userWithInvalidRoles))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Invalid roles provided")
                )
                .verify();

        // Verificar que NO se llamó a Keycloak ni Email
        verify(keycloakService, never()).createUser(any());
        verify(emailService, never()).sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UT004: Debe manejar error cuando Keycloak falla al crear usuario")
    void testCreateCompleteUser_KeycloakFailure() {
        // Given - Keycloak falla
        when(keycloakService.createUser(any(User.class)))
                .thenReturn(Mono.error(new RuntimeException("Keycloak connection failed")));

        // When & Then
        StepVerifier.create(userManagementService.createCompleteUser(testUser))
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Keycloak connection failed")
                )
                .verify();

        // Verificar que NO se envió email
        verify(emailService, never()).sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("UT005: Debe manejar error cuando falla el envío de email")
    void testCreateCompleteUser_EmailFailure() {
        // Given - Email falla pero Keycloak exitoso
        when(keycloakService.createUser(any(User.class)))
                .thenReturn(Mono.just("mock-keycloak-id-789"));
        
        when(keycloakService.getUserByKeycloakId(anyString()))
                .thenReturn(Mono.just(mockKeycloakUser));
        
        when(emailService.sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("SMTP server unavailable")));

        // When & Then
        StepVerifier.create(userManagementService.createCompleteUser(testUser))
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("SMTP server unavailable")
                )
                .verify();

        // Verificar que Keycloak se llamó pero falló el email
        verify(keycloakService, times(1)).createUser(any(User.class));
        verify(emailService, times(1)).sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString());
    }
}