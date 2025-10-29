package pe.edu.vallegrande.vgmsuser.performance;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.vallegrande.vgmsuser.application.service.IEmailService;
import pe.edu.vallegrande.vgmsuser.application.service.IKeycloakService;
import pe.edu.vallegrande.vgmsuser.config.PerformanceMockConfig;
import pe.edu.vallegrande.vgmsuser.domain.model.User;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pruebas de carga usando servicios mock
 * Evalúa el rendimiento sin impactar servicios externos
 */
@SpringBootTest
@ActiveProfiles("performance")
@Import(PerformanceMockConfig.class)
@Slf4j
class LoadTestWithMocksTest {

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IKeycloakService keycloakService;
    
    @Autowired
    private PerformanceMockConfig.PerformanceStats performanceStats;

    @Test
    @DisplayName("Prueba de Carga - Creación masiva de usuarios (Mock)")
    void testMassiveUserCreationLoad() {
        log.info("🚀 INICIANDO PRUEBA DE CARGA - CREACIÓN MASIVA DE USUARIOS");
        
        int totalUsers = 1000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        Instant startTime = Instant.now();

        // Crear usuarios en paralelo usando mocks
        Flux<String> userCreationFlux = Flux.range(1, totalUsers)
                .flatMap(i -> {
                    User user = User.builder()
                            .firstname("LoadTest")
                            .lastname("User" + i)
                            .email("loadtest.user" + i + "@mock.com")
                            .documentType(DocumentType.DNI)
                            .documentNumber("1000000" + String.format("%03d", i))
                            .phone("900000" + String.format("%03d", i))
                            .roles(Set.of(Role.teacher.name()))
                            .institutionId("LOAD_TEST_INST")
                            .build();

                    return keycloakService.createUser(user)
                            .doOnNext(keycloakId -> successCount.incrementAndGet())
                            .doOnError(error -> {
                                errorCount.incrementAndGet();
                                log.error("Error creando usuario {}: {}", i, error.getMessage());
                            })
                            .onErrorReturn("error-" + i);
                }, 50); // Paralelismo de 50

        StepVerifier.create(userCreationFlux)
                .expectNextCount(totalUsers)
                .verifyComplete();

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);

        // Métricas de rendimiento
        double usersPerSecond = totalUsers / (duration.toMillis() / 1000.0);
        
        log.info("📊 RESULTADOS DE PRUEBA DE CARGA - CREACIÓN DE USUARIOS:");
        log.info("   • Total usuarios: {}", totalUsers);
        log.info("   • Usuarios creados exitosamente: {}", successCount.get());
        log.info("   • Errores: {}", errorCount.get());
        log.info("   • Tiempo total: {} ms", duration.toMillis());
        log.info("   • Usuarios por segundo: {:.2f}", usersPerSecond);
        log.info("   • Tiempo promedio por usuario: {:.2f} ms", duration.toMillis() / (double) totalUsers);
        
        // Verificaciones de rendimiento
        assert successCount.get() > totalUsers * 0.95; // Al menos 95% de éxito
        assert usersPerSecond > 10; // Al menos 10 usuarios por segundo
        assert duration.toSeconds() < 300; // Máximo 5 minutos
        
        log.info("✅ PRUEBA DE CARGA COMPLETADA - TODOS LOS USUARIOS CREADOS EN MOCK");
    }

    @Test
    @DisplayName("Prueba de Carga - Envío masivo de emails (Mock)")
    void testMassiveEmailSendingLoad() {
        log.info("🚀 INICIANDO PRUEBA DE CARGA - ENVÍO MASIVO DE EMAILS");
        
        int totalEmails = 2000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        Instant startTime = Instant.now();

        // Enviar emails en paralelo usando mocks
        Flux<String> emailSendingFlux = Flux.range(1, totalEmails)
                .flatMap(i -> {
                    String email = "loadtest" + i + "@mock.com";
                    String username = "LoadTestUser" + i;
                    String tempPassword = "tempPass" + i;
                    String resetToken = "token" + i;

                    return emailService.sendTemporaryCredentialsEmail(email, username, tempPassword, resetToken)
                            .doOnSuccess(v -> successCount.incrementAndGet())
                            .doOnError(error -> {
                                errorCount.incrementAndGet();
                                log.error("Error enviando email {}: {}", i, error.getMessage());
                            })
                            .thenReturn("email-sent-" + i) // Convertir Mono<Void> a Mono<String>
                            .onErrorReturn("email-error-" + i);
                }, 100); // Paralelismo de 100

        StepVerifier.create(emailSendingFlux)
                .expectNextCount(totalEmails)
                .verifyComplete();

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);

        double emailsPerSecond = totalEmails / (duration.toMillis() / 1000.0);
        
        log.info("📊 RESULTADOS DE PRUEBA DE CARGA - ENVÍO DE EMAILS:");
        log.info("   • Total emails: {}", totalEmails);
        log.info("   • Emails enviados exitosamente: {}", successCount.get());
        log.info("   • Errores: {}", errorCount.get());
        log.info("   • Tiempo total: {} ms", duration.toMillis());
        log.info("   • Emails por segundo: {:.2f}", emailsPerSecond);
        log.info("   • Tiempo promedio por email: {:.2f} ms", duration.toMillis() / (double) totalEmails);
        
        // Verificaciones de rendimiento
        assert successCount.get() > totalEmails * 0.95; // Al menos 95% de éxito
        assert emailsPerSecond > 50; // Al menos 50 emails por segundo
        assert duration.toSeconds() < 120; // Máximo 2 minutos
        
        log.info("✅ PRUEBA DE CARGA COMPLETADA - TODOS LOS EMAILS SIMULADOS");
    }

    @Test
    @DisplayName("Prueba de Estrés - Flujo completo de usuario con alta concurrencia")
    void testCompleteUserFlowStressTest() throws InterruptedException {
        log.info("🔥 INICIANDO PRUEBA DE ESTRÉS - FLUJO COMPLETO CON ALTA CONCURRENCIA");
        
        int concurrentUsers = 100;
        int operationsPerUser = 10;
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong successfulOperations = new AtomicLong(0);
        AtomicLong failedOperations = new AtomicLong(0);
        
        Instant startTime = Instant.now();

        // Ejecutar operaciones concurrentes
        for (int userId = 1; userId <= concurrentUsers; userId++) {
            final int finalUserId = userId;
            
            // Ejecutar en hilo separado para simular concurrencia real
            new Thread(() -> {
                try {
                    executeUserOperations(finalUserId, operationsPerUser, totalOperations, successfulOperations, failedOperations);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Esperar a que terminen todas las operaciones (máximo 10 minutos)
        boolean finished = latch.await(10, TimeUnit.MINUTES);
        
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);

        // Métricas de estrés
        long expectedOperations = (long) concurrentUsers * operationsPerUser;
        double operationsPerSecond = totalOperations.get() / (duration.toMillis() / 1000.0);
        double successRate = (successfulOperations.get() / (double) totalOperations.get()) * 100;
        
        log.info("📊 RESULTADOS DE PRUEBA DE ESTRÉS:");
        log.info("   • Usuario concurrentes: {}", concurrentUsers);
        log.info("   • Operaciones por usuario: {}", operationsPerUser);
        log.info("   • Operaciones esperadas: {}", expectedOperations);
        log.info("   • Operaciones ejecutadas: {}", totalOperations.get());
        log.info("   • Operaciones exitosas: {}", successfulOperations.get());
        log.info("   • Operaciones fallidas: {}", failedOperations.get());
        log.info("   • Tasa de éxito: {:.2f}%", successRate);
        log.info("   • Tiempo total: {} ms", duration.toMillis());
        log.info("   • Operaciones por segundo: {:.2f}", operationsPerSecond);
        log.info("   • Prueba completada: {}", finished ? "SÍ" : "TIMEOUT");
        
        // Verificaciones de estrés
        assert finished : "La prueba debe completarse dentro del tiempo límite";
        assert successRate > 90.0 : "La tasa de éxito debe ser mayor al 90%";
        assert operationsPerSecond > 10 : "Debe procesar al menos 10 operaciones por segundo";
        
        log.info("✅ PRUEBA DE ESTRÉS COMPLETADA - SISTEMA ESTABLE BAJO ALTA CARGA");
    }

    private void executeUserOperations(int userId, int operationsPerUser, 
                                     AtomicLong totalOps, AtomicLong successOps, AtomicLong failedOps) {
        
        String email = "stress.user" + userId + "@mock.com";
        
        for (int op = 1; op <= operationsPerUser; op++) {
            try {
                totalOps.incrementAndGet();
                
                // Operación 1: Crear usuario
                User user = User.builder()
                        .firstname("Stress")
                        .lastname("User" + userId + "Op" + op)
                        .email(email + "." + op)
                        .documentType(DocumentType.DNI)
                        .documentNumber("2000" + String.format("%04d", userId) + String.format("%02d", op))
                        .phone("950" + String.format("%04d", userId) + String.format("%02d", op))
                        .roles(Set.of(Role.auxiliary.name()))
                        .institutionId("STRESS_TEST_INST")
                        .build();

                String keycloakId = keycloakService.createUser(user).block();
                
                // Operación 2: Enviar email
                emailService.sendTemporaryCredentialsEmail(
                        user.getEmail(),
                        user.getFirstname() + " " + user.getLastname(),
                        "stressPass" + userId + op,
                        "stressToken" + userId + op
                ).block();
                
                // Operación 3: Cambiar contraseña
                keycloakService.changePassword(keycloakId, "newStressPass" + userId + op).block();
                
                // Operación 4: Enviar confirmación
                emailService.sendPasswordChangeConfirmationEmail(
                        user.getEmail(),
                        user.getFirstname() + " " + user.getLastname()
                ).block();
                
                successOps.incrementAndGet();
                
                // Simular carga variable
                if (op % 3 == 0) {
                    Thread.sleep(10); // Pequeña pausa ocasional
                }
                
            } catch (Exception e) {
                failedOps.incrementAndGet();
                log.debug("Error en operación usuario {} operación {}: {}", userId, op, e.getMessage());
            }
        }
    }
}