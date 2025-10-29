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
 * Configuración de mocks optimizada para pruebas de rendimiento
 * Implementaciones ultra-rápidas sin operaciones costosas
 */
@TestConfiguration
@Slf4j
@Profile({"test", "performance"})
public class PerformanceMockConfig {

    // Mapas optimizados para alto rendimiento
    private final Map<String, KeycloakUserDto> mockUsers = new ConcurrentHashMap<>();
    private final AtomicInteger userIdCounter = new AtomicInteger(1000);
    private final AtomicInteger emailCounter = new AtomicInteger(0);

    @Bean
    @Primary
    public IKeycloakService performanceKeycloakService() {
        log.info("⚡ CREATING PERFORMANCE-OPTIMIZED MOCK KEYCLOAK SERVICE");
        return new IKeycloakService() {

            @Override
            public Mono<List<UserRepresentation>> findAllUsers() {
                return Mono.just(List.of());
            }

            @Override
            public Mono<List<UserRepresentation>> searchUserByUsername(String username) {
                return Mono.just(List.of());
            }

            @Override
            public Mono<String> createUser(User user) {
                String keycloakId = "perf-mock-id-" + userIdCounter.incrementAndGet();
                
                // Crear usuario optimizado sin campos opcionales para máximo rendimiento
                KeycloakUserDto keycloakUser = KeycloakUserDto.builder()
                        .keycloakId(keycloakId)
                        .username(user.getEmail())
                        .email(user.getEmail())
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .documentType(user.getDocumentType())
                        .documentNumber(user.getDocumentNumber())
                        .phone(user.getPhone())
                        .status(UserStatus.A)
                        .roles(user.getRoles())
                        .institutionId(user.getInstitutionId())
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                // Almacenamiento ultra-rápido
                mockUsers.put(keycloakId, keycloakUser);
                
                return Mono.just(keycloakId);
            }

            @Override
            public Mono<String> deleteUser(String keycloakId) {
                mockUsers.remove(keycloakId);
                return Mono.just(keycloakId);
            }

            @Override
            public Mono<Void> changePassword(String keycloakId, String newPassword) {
                KeycloakUserDto existingUser = mockUsers.get(keycloakId);
                if (existingUser != null) {
                    // Actualización mínima para máximo rendimiento
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
                return Mono.empty();
            }

            @Override
            public Mono<Void> enableUser(String keycloakId) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> disableUser(String keycloakId) {
                return Mono.empty();
            }

            @Override
            public Mono<KeycloakUserDto> getUserByKeycloakId(String keycloakId) {
                KeycloakUserDto user = mockUsers.get(keycloakId);
                return user != null ? Mono.just(user) : Mono.empty();
            }

            @Override
            public Flux<KeycloakUserDto> getAllUsersWithAttributes() {
                return Flux.fromIterable(mockUsers.values());
            }

            @Override
            public Mono<Void> updateUserAttributes(String keycloakId, User user) {
                KeycloakUserDto existingUser = mockUsers.get(keycloakId);
                if (existingUser != null) {
                    // Actualización optimizada
                    KeycloakUserDto updatedUser = KeycloakUserDto.builder()
                            .keycloakId(existingUser.getKeycloakId())
                            .username(existingUser.getUsername())
                            .email(existingUser.getEmail())
                            .firstname(user.getFirstname())
                            .lastname(user.getLastname())
                            .documentType(existingUser.getDocumentType())
                            .documentNumber(existingUser.getDocumentNumber())
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
                return Mono.empty();
            }

            @Override
            public Mono<KeycloakUserDto> getUserByUsername(String username) {
                return mockUsers.values().stream()
                        .filter(user -> user.getUsername().equals(username))
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.empty());
            }

            @Override
            public Mono<KeycloakUserDto> getUserByEmail(String email) {
                return mockUsers.values().stream()
                        .filter(user -> user.getEmail().equals(email))
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.empty());
            }
        };
    }

    /**
     * Servicio de email ultra-optimizado para pruebas de rendimiento
     */
    @Bean
    @Primary
    public IEmailService performanceEmailService() {
        log.info("⚡ CREATING PERFORMANCE-OPTIMIZED MOCK EMAIL SERVICE");
        return new IEmailService() {
            
            @Override
            public Mono<Void> sendTemporaryCredentialsEmail(String toEmail, String username, String temporaryPassword, String resetToken) {
                // Contador para verificar que se está llamando, pero sin logging costoso
                int count = emailCounter.incrementAndGet();
                
                // Log solo cada 1000 emails para evitar spam en performance tests
                if (count % 1000 == 0) {
                    log.debug("⚡ PERFORMANCE MOCK: {} emails processed so far", count);
                }
                
                return Mono.empty();
            }

            @Override
            public Mono<Void> sendPasswordChangeConfirmationEmail(String toEmail, String username) {
                emailCounter.incrementAndGet();
                return Mono.empty();
            }

            @Override
            public Mono<Void> sendPasswordResetEmail(String toEmail, String username, String resetToken) {
                emailCounter.incrementAndGet();
                return Mono.empty();
            }
        };
    }

    /**
     * Bean para obtener estadísticas de rendimiento durante las pruebas
     */
    @Bean
    public PerformanceStats performanceStats() {
        return new PerformanceStats(mockUsers, emailCounter);
    }

    /**
     * Clase para obtener estadísticas de rendimiento
     */
    public static class PerformanceStats {
        private final Map<String, KeycloakUserDto> userMap;
        private final AtomicInteger emailCount;

        public PerformanceStats(Map<String, KeycloakUserDto> userMap, AtomicInteger emailCount) {
            this.userMap = userMap;
            this.emailCount = emailCount;
        }

        public int getTotalUsersCreated() {
            return userMap.size();
        }

        public int getTotalEmailsSent() {
            return emailCount.get();
        }

        public void logStats() {
            log.info("📊 Performance Stats - Users: {}, Emails: {}", 
                    getTotalUsersCreated(), getTotalEmailsSent());
        }

        public void reset() {
            userMap.clear();
            emailCount.set(0);
            log.info("🔄 Performance stats reset");
        }
    }
}