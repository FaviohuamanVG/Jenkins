package pe.edu.vallegrande.vgmsuser.performance;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Suite completa de pruebas de rendimiento con reporte final
 * Genera métricas detalladas sin impactar servicios externos
 */
@SpringBootTest
@ActiveProfiles("performance")
@Import(PerformanceMockConfig.class)
@Slf4j
class PerformanceTestSuite {

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IKeycloakService keycloakService;
    
    @Autowired
    private PerformanceMockConfig.PerformanceStats performanceStats;

    private final StringBuilder reportBuilder = new StringBuilder();

    @BeforeEach
    void setUp() {
        performanceStats.reset();
        log.info("🚀 INICIANDO SUITE DE PRUEBAS DE RENDIMIENTO");
        log.info("================================================================");
        
        reportBuilder.setLength(0);
        reportBuilder.append("REPORTE DE PRUEBAS DE RENDIMIENTO\n");
        reportBuilder.append("================================\n\n");
    }

    @AfterEach
    void tearDown() {
        performanceStats.logStats();
        log.info("================================================================");
        log.info("📊 SUITE DE RENDIMIENTO COMPLETADA");
        
        // Imprimir reporte final
        log.info("\n{}", reportBuilder.toString());
    }

    @Test
    @DisplayName("Rendimiento - Creación rápida de usuarios")
    void testFastUserCreation() {
        log.info("⚡ Test: Creación rápida de usuarios");
        
        int userCount = 500;
        AtomicInteger success = new AtomicInteger(0);
        
        Instant start = Instant.now();
        
        StepVerifier.create(
            Flux.range(1, userCount)
                .flatMap(i -> {
                    User user = User.builder()
                            .firstname("Fast")
                            .lastname("User" + i)
                            .email("fast.user" + i + "@performance.com")
                            .documentType(DocumentType.DNI)
                            .documentNumber("7000" + String.format("%04d", i))
                            .phone("991" + String.format("%06d", i))
                            .roles(Set.of(Role.teacher.name()))
                            .institutionId("FAST_TEST_INST")
                            .build();

                    return keycloakService.createUser(user)
                            .doOnNext(id -> success.incrementAndGet());
                }, 25) // Paralelismo moderado
        )
        .expectNextCount(userCount)
        .verifyComplete();
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        double rate = userCount / (duration.toMillis() / 1000.0);
        
        String result = String.format(
            "Creación Rápida de Usuarios:\n" +
            "  - Usuarios creados: %d/%d\n" +
            "  - Tiempo: %d ms\n" +
            "  - Tasa: %.2f usuarios/seg\n" +
            "  - Tiempo promedio por usuario: %.2f ms\n\n",
            success.get(), userCount, duration.toMillis(), rate, duration.toMillis() / (double) userCount
        );
        
        reportBuilder.append(result);
        log.info("✅ Creación rápida completada: {} usuarios en {} ms ({:.2f}/seg)", 
                success.get(), duration.toMillis(), rate);
    }

    @Test
    @DisplayName("Rendimiento - Envío masivo de emails")
    void testMassEmailSending() {
        log.info("📧 Test: Envío masivo de emails");
        
        int emailCount = 1000;
        AtomicInteger success = new AtomicInteger(0);
        
        Instant start = Instant.now();
        
        StepVerifier.create(
            Flux.range(1, emailCount)
                .flatMap(i -> {
                    return emailService.sendTemporaryCredentialsEmail(
                            "mass.email" + i + "@performance.com",
                            "MassUser" + i,
                            "massPass" + i,
                            "massToken" + i
                    ).doOnSuccess(v -> success.incrementAndGet())
                     .thenReturn("email-sent-" + i); // Convertir a Mono<String>
                }, 50) // Alto paralelismo para emails
        )
        .expectNextCount(emailCount)
        .verifyComplete();
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        double rate = emailCount / (duration.toMillis() / 1000.0);
        
        String result = String.format(
            "Envío Masivo de Emails:\n" +
            "  - Emails enviados: %d/%d\n" +
            "  - Tiempo: %d ms\n" +
            "  - Tasa: %.2f emails/seg\n" +
            "  - Tiempo promedio por email: %.2f ms\n\n",
            success.get(), emailCount, duration.toMillis(), rate, duration.toMillis() / (double) emailCount
        );
        
        reportBuilder.append(result);
        log.info("✅ Envío masivo completado: {} emails en {} ms ({:.2f}/seg)", 
                success.get(), duration.toMillis(), rate);
    }

    @Test
    @DisplayName("Rendimiento - Operaciones mixtas con alta concurrencia")
    void testMixedOperationsHighConcurrency() {
        log.info("🔄 Test: Operaciones mixtas con alta concurrencia");
        
        int operationSets = 200; // Cada set incluye crear usuario + enviar email + cambiar password
        AtomicInteger userCreations = new AtomicInteger(0);
        AtomicInteger emailsSent = new AtomicInteger(0);
        AtomicInteger passwordChanges = new AtomicInteger(0);
        
        Instant start = Instant.now();
        
        StepVerifier.create(
            Flux.range(1, operationSets)
                .flatMap(i -> {
                    // Crear usuario
                    User user = User.builder()
                            .firstname("Mixed")
                            .lastname("User" + i)
                            .email("mixed.user" + i + "@performance.com")
                            .documentType(DocumentType.DNI)
                            .documentNumber("8000" + String.format("%04d", i))
                            .phone("992" + String.format("%06d", i))
                            .roles(Set.of(Role.auxiliary.name()))
                            .institutionId("MIXED_TEST_INST")
                            .build();

                    return keycloakService.createUser(user)
                            .doOnNext(id -> userCreations.incrementAndGet())
                            .flatMap(keycloakId -> {
                                // Enviar email
                                return emailService.sendTemporaryCredentialsEmail(
                                        user.getEmail(),
                                        user.getFirstname() + " " + user.getLastname(),
                                        "mixedPass" + i,
                                        "mixedToken" + i
                                ).doOnSuccess(v -> emailsSent.incrementAndGet())
                                .then(
                                    // Cambiar contraseña
                                    keycloakService.changePassword(keycloakId, "newMixedPass" + i)
                                            .doOnSuccess(v -> passwordChanges.incrementAndGet())
                                )
                                .thenReturn(keycloakId);
                            });
                }, 30) // Alta concurrencia
        )
        .expectNextCount(operationSets)
        .verifyComplete();
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        int totalOperations = userCreations.get() + emailsSent.get() + passwordChanges.get();
        double rate = totalOperations / (duration.toMillis() / 1000.0);
        
        String result = String.format(
            "Operaciones Mixtas Alta Concurrencia:\n" +
            "  - Sets de operaciones: %d\n" +
            "  - Usuarios creados: %d\n" +
            "  - Emails enviados: %d\n" +
            "  - Contraseñas cambiadas: %d\n" +
            "  - Total operaciones: %d\n" +
            "  - Tiempo: %d ms\n" +
            "  - Tasa: %.2f operaciones/seg\n" +
            "  - Tiempo promedio por set: %.2f ms\n\n",
            operationSets, userCreations.get(), emailsSent.get(), passwordChanges.get(),
            totalOperations, duration.toMillis(), rate, duration.toMillis() / (double) operationSets
        );
        
        reportBuilder.append(result);
        log.info("✅ Operaciones mixtas completadas: {} sets en {} ms ({:.2f} ops/seg)", 
                operationSets, duration.toMillis(), rate);
    }

    @Test
    @DisplayName("Rendimiento - Simulación de carga de producción")
    void testProductionLoadSimulation() {
        log.info("🏭 Test: Simulación de carga de producción");
        
        // Simular una carga realista de producción durante 30 segundos
        Duration testDuration = Duration.ofSeconds(30);
        int operationsPerSecond = 15; // Carga realista
        
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        
        Instant start = Instant.now();
        
        StepVerifier.create(
            Flux.interval(Duration.ofMillis(1000 / operationsPerSecond))
                .take(testDuration)
                .flatMap(tick -> {
                    totalOperations.incrementAndGet();
                    
                    User user = User.builder()
                            .firstname("Production")
                            .lastname("User" + tick)
                            .email("production.user" + tick + "@performance.com")
                            .documentType(DocumentType.DNI)
                            .documentNumber("9000" + String.format("%05d", tick.intValue()))
                            .phone("993" + String.format("%06d", tick.intValue()))
                            .roles(Set.of(Role.teacher.name()))
                            .institutionId("PRODUCTION_TEST_INST")
                            .build();

                    return keycloakService.createUser(user)
                            .flatMap(keycloakId -> 
                                emailService.sendTemporaryCredentialsEmail(
                                    user.getEmail(),
                                    user.getFirstname() + " " + user.getLastname(),
                                    "prodPass" + tick,
                                    "prodToken" + tick
                                ).thenReturn(keycloakId)
                            )
                            .doOnNext(id -> successfulOperations.incrementAndGet())
                            .onErrorReturn("error-" + tick);
                }, 10) // Concurrencia moderada para simular producción
        )
        .thenConsumeWhile(result -> true)
        .verifyComplete();
        
        Instant end = Instant.now();
        Duration actualDuration = Duration.between(start, end);
        double successRate = (successfulOperations.get() / (double) totalOperations.get()) * 100;
        double actualRate = totalOperations.get() / (actualDuration.toMillis() / 1000.0);
        
        String result = String.format(
            "Simulación de Carga de Producción:\n" +
            "  - Duración planificada: %d segundos\n" +
            "  - Duración real: %d ms\n" +
            "  - Operaciones totales: %d\n" +
            "  - Operaciones exitosas: %d\n" +
            "  - Tasa de éxito: %.2f%%\n" +
            "  - Tasa objetivo: %d ops/seg\n" +
            "  - Tasa real: %.2f ops/seg\n" +
            "  - Eficiencia: %.2f%%\n\n",
            testDuration.toSeconds(), actualDuration.toMillis(),
            totalOperations.get(), successfulOperations.get(), successRate,
            operationsPerSecond, actualRate, (actualRate / operationsPerSecond) * 100
        );
        
        reportBuilder.append(result);
        log.info("✅ Simulación de producción completada: {:.2f}% éxito, {:.2f} ops/seg", 
                successRate, actualRate);
    }
}