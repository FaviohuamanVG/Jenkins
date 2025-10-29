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
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import pe.edu.vallegrande.vgmsuser.infraestructure.util.KeycloakProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas Unitarias SIMPLIFICADAS para UserManagementServiceImpl
 * Se enfocan en la lógica de negocio sin dependencias externas complejas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementService - Pruebas Unitarias Simplificadas")
class UserManagementServiceSimpleTest {

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
                .roles(Set.of("teacher"))
                .institutionId("INST001")
                .build();

        mockKeycloakUser = KeycloakUserDto.builder()
                .keycloakId(UUID.randomUUID().toString())
                .username("juan.perez")
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan.perez@vallegrande.edu.pe")
                .status(UserStatus.A)
                .passwordStatus(PasswordStatus.TEMPORARY)
                .roles(Set.of("teacher"))
                .institutionId("INST001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("UT001: Debe validar roles correctamente - Solo teacher, auxiliary, secretary permitidos")
    void testValidateRoles_OnlyValidRolesAllowed() {
        // Test 1: Roles válidos - teacher
        User userWithTeacher = createUserWithRoles(Set.of("teacher"));
        // Test 2: Roles válidos - auxiliary  
        User userWithAuxiliary = createUserWithRoles(Set.of("auxiliary"));
        // Test 3: Roles válidos - secretary
        User userWithSecretary = createUserWithRoles(Set.of("secretary"));
        // Test 4: Roles válidos - múltiples
        User userWithMultipleValid = createUserWithRoles(Set.of("teacher", "auxiliary"));

        // Configurar mocks para casos exitosos - Simular creación exitosa
        when(keycloakService.createUser(any(User.class))).thenReturn(Mono.just(UUID.randomUUID().toString()));

        // Mock para getUserByKeycloakId que es llamado después de createUser
        when(keycloakService.getUserByKeycloakId(anyString()))
                .thenReturn(Mono.just(mockKeycloakUser));

        // Mock para updatePasswordResetToken
        when(keycloakService.updatePasswordResetToken(anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Mock para emailService.sendTemporaryCredentialsEmail
        when(emailService.sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Verificar que roles válidos son aceptados y procesan correctamente
        StepVerifier.create(userManagementService.createCompleteUser(userWithTeacher))
                .expectNextMatches(result -> result != null && result.getRoles().contains("teacher"))
                .verifyComplete();

        StepVerifier.create(userManagementService.createCompleteUser(userWithAuxiliary))
                .expectNextMatches(result -> result != null && result.getUsername().equals("juan.perez"))
                .verifyComplete();

        StepVerifier.create(userManagementService.createCompleteUser(userWithSecretary))
                .expectNextMatches(result -> result != null && result.getUsername().equals("juan.perez"))
                .verifyComplete();

        StepVerifier.create(userManagementService.createCompleteUser(userWithMultipleValid))
                .expectNextMatches(result -> result != null && result.getUsername().equals("juan.perez"))
                .verifyComplete();

        // Ninguno debe fallar por validación de roles
        verify(keycloakService, times(4)).createUser(any(User.class));
    }

    @Test
    @DisplayName("UT002: Debe rechazar roles inválidos - admin, superuser, etc.")
    void testValidateRoles_InvalidRolesRejected() {
        // Test: Roles inválidos - admin
        User userWithAdmin = createUserWithRoles(Set.of("admin"));
        // Test: Roles inválidos - superuser
        User userWithSuperuser = createUserWithRoles(Set.of("superuser"));
        // Test: Roles inválidos - múltiples con uno inválido
        User userWithMixed = createUserWithRoles(Set.of("teacher", "admin"));

        // Verificar que se rechazan roles inválidos ANTES de llamar a Keycloak
        StepVerifier.create(userManagementService.createCompleteUser(userWithAdmin))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Solo se permiten roles teacher, auxiliary o secretary")
                )
                .verify();

        StepVerifier.create(userManagementService.createCompleteUser(userWithSuperuser))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Solo se permiten roles teacher, auxiliary o secretary")
                )
                .verify();

        StepVerifier.create(userManagementService.createCompleteUser(userWithMixed))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Solo se permiten roles teacher, auxiliary o secretary")
                )
                .verify();

        // NO debe llamar a Keycloak si la validación falla
        verify(keycloakService, never()).createUser(any(User.class));
    }

    @Test
    @DisplayName("UT003: Debe asignar rol 'teacher' por defecto cuando no hay roles")
    void testDefaultRole_TeacherAssignedWhenNoRoles() {
        // Test 1: Roles null
        User userWithNullRoles = createUserWithRoles(null);
        // Test 2: Roles vacíos
        User userWithEmptyRoles = createUserWithRoles(Set.of());

        // Configurar mock para verificar que se llama con teacher por defecto
        when(keycloakService.createUser(any(User.class)))
                .thenAnswer(invocation -> {
                    User capturedUser = invocation.getArgument(0);
                    // Verificar que se asignó 'teacher' por defecto
                    assertThat(capturedUser.getRoles()).containsExactly("teacher");
                    return Mono.just(UUID.randomUUID().toString());
                });

        // Mock para getUserByKeycloakId que es llamado después de createUser
        when(keycloakService.getUserByKeycloakId(anyString()))
                .thenReturn(Mono.just(mockKeycloakUser));

        // Mock para updatePasswordResetToken
        when(keycloakService.updatePasswordResetToken(anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Mock para emailService.sendTemporaryCredentialsEmail
        when(emailService.sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Verificar comportamiento con roles null - debe asignar teacher por defecto y completar exitosamente
        StepVerifier.create(userManagementService.createCompleteUser(userWithNullRoles))
                .expectNextMatches(result -> result != null && result.getRoles().contains("teacher"))
                .verifyComplete();

        // Verificar comportamiento con roles vacíos - debe asignar teacher por defecto y completar exitosamente
        StepVerifier.create(userManagementService.createCompleteUser(userWithEmptyRoles))
                .expectNextMatches(result -> result != null && result.getRoles().contains("teacher"))
                .verifyComplete();

        verify(keycloakService, times(2)).createUser(any(User.class));
    }

    @Test
    @DisplayName("UT004: Debe usar documentNumber como contraseña temporal")
    void testTemporaryPassword_UsesDocumentNumber() {
        // Given
        User userWithDocumentNumber = User.builder()
                .username(testUser.getUsername())
                .firstname(testUser.getFirstname())
                .lastname(testUser.getLastname())
                .email(testUser.getEmail())
                .documentType(testUser.getDocumentType())
                .documentNumber("87654321")
                .roles(testUser.getRoles())
                .institutionId(testUser.getInstitutionId())
                .password(null) // Sin contraseña inicial
                .build();

        // Configurar mock para capturar la contraseña establecida
        when(keycloakService.createUser(any(User.class)))
                .thenAnswer(invocation -> {
                    User capturedUser = invocation.getArgument(0);
                    // Verificar que la contraseña es el documentNumber
                    assertThat(capturedUser.getPassword()).isEqualTo("87654321");
                    return Mono.just(UUID.randomUUID().toString());
                });

        // Mock para getUserByKeycloakId que es llamado después de createUser
        when(keycloakService.getUserByKeycloakId(anyString()))
                .thenReturn(Mono.just(mockKeycloakUser));

        // Mock para updatePasswordResetToken
        when(keycloakService.updatePasswordResetToken(anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Mock para emailService.sendTemporaryCredentialsEmail
        when(emailService.sendTemporaryCredentialsEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // When & Then - debe completar exitosamente y verificar que se usó el documentNumber como contraseña
        StepVerifier.create(userManagementService.createCompleteUser(userWithDocumentNumber))
                .expectNextMatches(result -> result != null && result.getUsername().equals("juan.perez"))
                .verifyComplete();

        verify(keycloakService, times(1)).createUser(any(User.class));
    }

    @Test
    @DisplayName("UT005: Debe filtrar usuarios staff correctamente por institución")
    void testGetStaffByInstitution_FiltersCorrectly() {
        // Given - Usuarios de diferentes instituciones y roles
        String staffId1 = UUID.randomUUID().toString();
        String staffId2 = UUID.randomUUID().toString();
        String adminId = UUID.randomUUID().toString();
        String otherInstId = UUID.randomUUID().toString();
        
        KeycloakUserDto staffUser1 = createKeycloakUser(staffId1, "teacher.1", Set.of("teacher"), "INST001");
        KeycloakUserDto staffUser2 = createKeycloakUser(staffId2, "auxiliary.1", Set.of("auxiliary"), "INST001");
        KeycloakUserDto nonStaffUser = createKeycloakUser(adminId, "admin.1", Set.of("admin"), "INST001");
        KeycloakUserDto differentInstitution = createKeycloakUser(otherInstId, "teacher.2", Set.of("teacher"), "INST002");

        when(keycloakService.getAllUsersWithAttributes())
                .thenReturn(Flux.just(staffUser1, staffUser2, nonStaffUser, differentInstitution));

        // When
        Flux<KeycloakUserDto> result = userManagementService.getStaffByInstitution("INST001");

        // Then - Solo debe retornar staff de INST001
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getKeycloakId().equals(staffId1))
                .expectNextMatches(user -> user.getKeycloakId().equals(staffId2))
                .verifyComplete();

        verify(keycloakService, times(1)).getAllUsersWithAttributes();
    }

    // ========== HELPER METHODS (Para evitar repetir código) ==========

    private User createUserWithRoles(Set<String> roles) {
        return User.builder()
                .username(testUser.getUsername())
                .firstname(testUser.getFirstname())
                .lastname(testUser.getLastname())
                .email(testUser.getEmail())
                .documentType(testUser.getDocumentType())
                .documentNumber(testUser.getDocumentNumber())
                .roles(roles)
                .institutionId(testUser.getInstitutionId())
                .build();
    }

    private KeycloakUserDto createKeycloakUser(String keycloakId, String username, Set<String> roles, String institutionId) {
        return KeycloakUserDto.builder()
                .keycloakId(keycloakId)
                .username(username)
                .firstname(mockKeycloakUser.getFirstname())
                .lastname(mockKeycloakUser.getLastname())
                .email(username + "@vallegrande.edu.pe")
                .status(UserStatus.A)
                .passwordStatus(PasswordStatus.TEMPORARY)
                .roles(roles)
                .institutionId(institutionId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}