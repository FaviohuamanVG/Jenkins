package pe.edu.vallegrande.vgmsuser.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.dto.KeycloakUserDto;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.keycloak.representations.idm.UserRepresentation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuración de mocks para pruebas de integración
 * Evita el uso de servicios reales (Keycloak, Email)
 */
@TestConfiguration
@Slf4j
@Profile({"test", "performance"})
public class TestMockConfig {

    private final Map<String, KeycloakUserDto> mockUsers = new ConcurrentHashMap<>();
    private final AtomicInteger userIdCounter = new AtomicInteger(1000);

    @Bean
    @Primary
    public IKeycloakService mockKeycloakService() {
        log.info("🔧 CREATING MOCK KEYCLOAK SERVICE");
        return new IKeycloakService() {

            @Override
            public Mono<List<UserRepresentation>> findAllUsers() {
                log.info("🔧 MOCK: findAllUsers called");
                return Mono.just(List.of());
            }

            @Override
            public Mono<List<UserRepresentation>> searchUserByUsername(String username) {
                log.info("🔧 MOCK: searchUserByUsername called with: {}", username);
                return Mono.just(List.of());
            }

            @Override
            public Mono<String> createUser(User user) {
                String keycloakId = "mock-id-" + userIdCounter.incrementAndGet();
                log.info("🔧 MOCK: Creating user with email: {} -> keycloakId: {}", user.getEmail(), keycloakId);
                
                KeycloakUserDto keycloakUser = KeycloakUserDto.builder()
                        .keycloakId(keycloakId)
                        .username(user.getEmail())
                        .email(user.getEmail())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .documentType(user.getDocumentType())
                        .documentNumber(user.getDocumentNumber())
                        .phone(user.getPhone())
                        .status(UserStatus.A) // Usar A en lugar de ACTIVE
                        .passwordStatus(PasswordStatus.PERMANENT) // Usuario mock con contraseña permanente
                        .passwordCreatedAt(LocalDateTime.now())
                        .roles(user.getRoles())
                        .institutionId(user.getInstitutionId())
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                mockUsers.put(keycloakId, keycloakUser);
                return Mono.just(keycloakId);
            }

            @Override
            public Mono<String> deleteUser(String keycloakId) {
                log.info("🔧 MOCK: Deleting user with keycloakId: {}", keycloakId);
                mockUsers.remove(keycloakId);
                return Mono.just(keycloakId);
            }

            @Override
            public Mono<Void> changePassword(String keycloakId, String newPassword) {
                log.info("🔧 MOCK: Changing password for keycloakId: {}", keycloakId);
                KeycloakUserDto existingUser = mockUsers.get(keycloakId);
                if (existingUser != null) {
                    KeycloakUserDto updatedUser = KeycloakUserDto.builder()
                            .keycloakId(existingUser.getKeycloakId())
                            .username(existingUser.getUsername())
                            .email(existingUser.getEmail())
                            .firstname(existingUser.getFirstname())
                            .lastname(existingUser.getLastname())
                            .documentType(existingUser.getDocumentType())
                            .documentNumber(existingUser.getDocumentNumber())
                            .phone(existingUser.getPhone())
                            .status(existingUser.getStatus())
                            .passwordStatus(PasswordStatus.PERMANENT)
                            .passwordCreatedAt(LocalDateTime.now())
                            .passwordResetToken(existingUser.getPasswordResetToken())
                            .institutionId(existingUser.getInstitutionId())
                            .roles(existingUser.getRoles())
                            .enabled(existingUser.isEnabled())
                            .createdAt(existingUser.getCreatedAt())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    mockUsers.put(keycloakId, updatedUser);
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> updateUser(String userId, User userDTO) {
                log.info("🔧 MOCK: Updating user with keycloakId: {}", userId);
                return Mono.empty();
            }

            @Override
            public Mono<Void> enableUser(String keycloakId) {
                log.info("🔧 MOCK: Enabling user with keycloakId: {}", keycloakId);
                return Mono.empty();
            }

            @Override
            public Mono<Void> disableUser(String keycloakId) {
                log.info("🔧 MOCK: Disabling user with keycloakId: {}", keycloakId);
                return Mono.empty();
            }

            @Override
            public Mono<KeycloakUserDto> getUserByKeycloakId(String keycloakId) {
                log.info("🔧 MOCK: Getting user by keycloakId: {}", keycloakId);
                KeycloakUserDto user = mockUsers.get(keycloakId);
                return user != null ? Mono.just(user) : Mono.empty();
            }

            @Override
            public Flux<KeycloakUserDto> getAllUsersWithAttributes() {
                log.info("🔧 MOCK: Getting all users with attributes");
                return Flux.fromIterable(mockUsers.values());
            }

            @Override
            public Mono<Void> updateUserAttributes(String keycloakId, User user) {
                log.info("🔧 MOCK: Updating user attributes for keycloakId: {}", keycloakId);
                KeycloakUserDto existingUser = mockUsers.get(keycloakId);
                if (existingUser != null) {
                    KeycloakUserDto updatedUser = KeycloakUserDto.builder()
                            .keycloakId(existingUser.getKeycloakId())
                            .username(existingUser.getUsername())
                            .email(existingUser.getEmail())
                            .firstname(user.getFirstname())
                            .lastname(user.getLastname())
                            .documentType(user.getDocumentType())
                            .documentNumber(user.getDocumentNumber())
                            .phone(user.getPhone())
                            .status(existingUser.getStatus())
                            .passwordStatus(existingUser.getPasswordStatus())
                            .passwordCreatedAt(existingUser.getPasswordCreatedAt())
                            .passwordResetToken(existingUser.getPasswordResetToken())
                            .institutionId(user.getInstitutionId())
                            .roles(user.getRoles())
                            .enabled(existingUser.isEnabled())
                            .createdAt(existingUser.getCreatedAt())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    mockUsers.put(keycloakId, updatedUser);
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> updatePasswordResetToken(String keycloakId, String resetToken) {
                log.info("🔧 MOCK: Updating password reset token for keycloakId: {}", keycloakId);
                KeycloakUserDto existingUser = mockUsers.get(keycloakId);
                if (existingUser != null) {
                    KeycloakUserDto updatedUser = KeycloakUserDto.builder()
                            .keycloakId(existingUser.getKeycloakId())
                            .username(existingUser.getUsername())
                            .email(existingUser.getEmail())
                            .firstname(existingUser.getFirstname())
                            .lastname(existingUser.getLastname())
                            .documentType(existingUser.getDocumentType())
                            .documentNumber(existingUser.getDocumentNumber())
                            .phone(existingUser.getPhone())
                            .status(existingUser.getStatus())
                            .passwordStatus(existingUser.getPasswordStatus())
                            .passwordCreatedAt(existingUser.getPasswordCreatedAt())
                            .passwordResetToken(resetToken)
                            .institutionId(existingUser.getInstitutionId())
                            .roles(existingUser.getRoles())
                            .enabled(existingUser.isEnabled())
                            .createdAt(existingUser.getCreatedAt())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    mockUsers.put(keycloakId, updatedUser);
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> updatePasswordStatus(String keycloakId, String passwordStatus, String passwordCreatedAt) {
                log.info("🔧 MOCK: Updating password status for keycloakId: {}", keycloakId);
                KeycloakUserDto existingUser = mockUsers.get(keycloakId);
                if (existingUser != null) {
                    KeycloakUserDto updatedUser = KeycloakUserDto.builder()
                            .keycloakId(existingUser.getKeycloakId())
                            .username(existingUser.getUsername())
                            .email(existingUser.getEmail())
                            .firstname(existingUser.getFirstname())
                            .lastname(existingUser.getLastname())
                            .documentType(existingUser.getDocumentType())
                            .documentNumber(existingUser.getDocumentNumber())
                            .phone(existingUser.getPhone())
                            .status(existingUser.getStatus())
                            .passwordStatus(PasswordStatus.PERMANENT)
                            .passwordCreatedAt(LocalDateTime.now())
                            .passwordResetToken(existingUser.getPasswordResetToken())
                            .institutionId(existingUser.getInstitutionId())
                            .roles(existingUser.getRoles())
                            .enabled(existingUser.isEnabled())
                            .createdAt(existingUser.getCreatedAt())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    mockUsers.put(keycloakId, updatedUser);
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> updateUserStatus(String keycloakId, String status) {
                log.info("🔧 MOCK: Updating user status for keycloakId: {}", keycloakId);
                return Mono.empty();
            }

            @Override
            public Mono<KeycloakUserDto> getUserByUsername(String username) {
                log.info("🔧 MOCK: Getting user by username: {}", username);
                return mockUsers.values().stream()
                        .filter(user -> user.getUsername().equals(username))
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.empty());
            }

            @Override
            public Mono<KeycloakUserDto> getUserByEmail(String email) {
                log.info("🔧 MOCK: Getting user by email: {}", email);
                return mockUsers.values().stream()
                        .filter(user -> user.getEmail().equals(email))
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.empty());
            }
        };
    }

    /**
     * Mock del servicio de Email para pruebas
     */
    @Bean
    @Primary
    public IEmailService mockEmailService() {
        log.info("🔧 CREATING MOCK EMAIL SERVICE");
        return new IEmailService() {
            
            @Override
            public Mono<Void> sendTemporaryCredentialsEmail(String toEmail, String username, String temporaryPassword, String resetToken) {
                log.info("🔧 MOCK: Sending temporary credentials email to: {} with username: {}", toEmail, username);
                log.debug("🔧 MOCK: Email content - Temporary password: {}, Reset token: {}", temporaryPassword, resetToken);
                // Simular éxito del envío de email sin enviar realmente
                return Mono.empty();
            }

            @Override
            public Mono<Void> sendPasswordChangeConfirmationEmail(String toEmail, String username) {
                log.info("🔧 MOCK: Sending password change confirmation email to: {} for user: {}", toEmail, username);
                // Simular éxito del envío de email sin enviar realmente
                return Mono.empty();
            }

            @Override
            public Mono<Void> sendPasswordResetEmail(String toEmail, String username, String resetToken) {
                log.info("🔧 MOCK: Sending password reset email to: {} for user: {} with token: {}", toEmail, username, resetToken);
                // Simular éxito del envío de email sin enviar realmente
                return Mono.empty();
            }
        };
    }
}