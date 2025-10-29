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
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pruebas de estrés específicas para escenarios críticos
 * Usando servicios mock para evitar impacto en infraestructura real
 */
@SpringBootTest
@ActiveProfiles("performance")
@Import(PerformanceMockConfig.class)
@Slf4j
class StressTestWithMocksTest {

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IKeycloakService keycloakService;
    
    @Autowired
    private PerformanceMockConfig.PerformanceStats performanceStats;

    @Test
    @DisplayName("Estrés - Ráfagas de creación de usuarios")
    void testBurstUserCreation() {
        log.info("💥 INICIANDO PRUEBA DE ESTRÉS - RÁFAGAS DE USUARIOS");
        
        int burstSize = 500; // Usuarios por ráfaga
        int numberOfBursts = 5;
        Duration pauseBetweenBursts = Duration.ofSeconds(2);
        
        AtomicInteger totalCreated = new AtomicInteger(0);
        List<Duration> burstDurations = new ArrayList<>();
        
        Instant testStart = Instant.now();
        
        for (int burst = 1; burst <= numberOfBursts; burst++) {
            final int currentBurst = burst; // Variable final para usar en lambda
            log.info("🔥 Ejecutando ráfaga {} de {}", currentBurst, numberOfBursts);
            
            Instant burstStart = Instant.now();
            
            // Crear usuarios en una ráfaga intensa
            StepVerifier.create(
                Flux.range(1, burstSize)
                    .flatMap(i -> {
                        int globalIndex = (currentBurst - 1) * burstSize + i;
                        User user = User.builder()
                                .firstname("Burst")
                                .lastname("User" + globalIndex)
                                .email("burst.user" + globalIndex + "@stress.com")
                                .documentType(DocumentType.DNI)
                                .documentNumber("3000" + String.format("%05d", globalIndex))
                                .phone("960" + String.format("%06d", globalIndex))
                                .roles(Set.of(Role.teacher.name()))
                                .institutionId("BURST_TEST_INST")
                                .build();

                        return keycloakService.createUser(user)
                                .doOnNext(id -> totalCreated.incrementAndGet());
                    }, 100) // Alta concurrencia
            )
            .expectNextCount(burstSize)
            .verifyComplete();
            
            Instant burstEnd = Instant.now();
            Duration burstDuration = Duration.between(burstStart, burstEnd);
            burstDurations.add(burstDuration);
            
            double burstRate = burstSize / (burstDuration.toMillis() / 1000.0);
            log.info("   • Ráfaga {} completada en {} ms ({:.2f} usuarios/seg)", 
                    currentBurst, burstDuration.toMillis(), burstRate);
            
            // Pausa entre ráfagas (excepto la última)
            if (currentBurst < numberOfBursts) {
                try {
                    Thread.sleep(pauseBetweenBursts.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        Instant testEnd = Instant.now();
        Duration totalDuration = Duration.between(testStart, testEnd);
        
        // Análisis de resultados
        double avgBurstTime = burstDurations.stream()
                .mapToLong(Duration::toMillis)
                .average()
                .orElse(0.0);
        
        double overallRate = totalCreated.get() / (totalDuration.toMillis() / 1000.0);
        
        log.info("📊 RESULTADOS PRUEBA DE RÁFAGAS:");
        log.info("   • Total usuarios creados: {}", totalCreated.get());
        log.info("   • Número de ráfagas: {}", numberOfBursts);
        log.info("   • Usuarios por ráfaga: {}", burstSize);
        log.info("   • Tiempo promedio por ráfaga: {:.2f} ms", avgBurstTime);
        log.info("   • Tiempo total: {} ms", totalDuration.toMillis());
        log.info("   • Tasa general: {:.2f} usuarios/seg", overallRate);
        
        // Verificaciones
        assert totalCreated.get() == burstSize * numberOfBursts;
        assert avgBurstTime < 30000; // Máximo 30 segundos por ráfaga
        assert overallRate > 15; // Al menos 15 usuarios por segundo general
        
        log.info("✅ PRUEBA DE RÁFAGAS COMPLETADA - SISTEMA RESISTENTE A PICOS");
    }

    @Test
    @DisplayName("Estrés - Sostenibilidad a largo plazo")
    void testLongTermSustainability() {
        log.info("⏰ INICIANDO PRUEBA DE SOSTENIBILIDAD A LARGO PLAZO");
        
        Duration testDuration = Duration.ofMinutes(3); // 3 minutos de estrés continuo
        int operationsPerSecond = 20;
        
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        Instant startTime = Instant.now();
        
        // Crear un flujo continuo de operaciones
        Flux<String> continuousLoad = Flux.interval(Duration.ofMillis(1000 / operationsPerSecond))
                .take(testDuration)
                .flatMap(tick -> {
                    totalOperations.incrementAndGet();
                    
                    // Alternar entre diferentes tipos de operaciones
                    if (tick % 3 == 0) {
                        // Crear usuario
                        return createUserOperation(tick.intValue())
                                .doOnNext(id -> successfulOperations.incrementAndGet())
                                .doOnError(e -> errorCount.incrementAndGet())
                                .onErrorReturn("error-" + tick);
                    } else if (tick % 3 == 1) {
                        // Enviar email
                        return sendEmailOperation(tick.intValue())
                                .doOnSuccess(v -> successfulOperations.incrementAndGet())
                                .doOnError(e -> errorCount.incrementAndGet())
                                .onErrorReturn((Void) null)
                                .map(v -> "email-" + tick);
                    } else {
                        // Operaciones combinadas
                        return combinedOperation(tick.intValue())
                                .doOnNext(id -> successfulOperations.incrementAndGet())
                                .doOnError(e -> errorCount.incrementAndGet())
                                .onErrorReturn("combined-error-" + tick);
                    }
                })
                .subscribeOn(Schedulers.parallel());

        StepVerifier.create(continuousLoad)
                .thenConsumeWhile(result -> true) // Consumir todos los resultados
                .verifyComplete();

        Instant endTime = Instant.now();
        Duration actualDuration = Duration.between(startTime, endTime);
        
        // Métricas de sostenibilidad
        double actualRate = totalOperations.get() / (actualDuration.toMillis() / 1000.0);
        double successRate = (successfulOperations.get() / (double) totalOperations.get()) * 100;
        
        log.info("📊 RESULTADOS PRUEBA DE SOSTENIBILIDAD:");
        log.info("   • Duración planificada: {} minutos", testDuration.toMinutes());
        log.info("   • Duración real: {} ms", actualDuration.toMillis());
        log.info("   • Operaciones totales: {}", totalOperations.get());
        log.info("   • Operaciones exitosas: {}", successfulOperations.get());
        log.info("   • Errores: {}", errorCount.get());
        log.info("   • Tasa de éxito: {:.2f}%", successRate);
        log.info("   • Operaciones por segundo: {:.2f}", actualRate);
        
        // Verificaciones de sostenibilidad
        assert successRate > 95.0 : "Debe mantener >95% éxito a largo plazo";
        assert actualRate > operationsPerSecond * 0.8 : "Debe mantener al menos 80% de la tasa objetivo";
        assert errorCount.get() < totalOperations.get() * 0.05 : "Errores deben ser <5%";
        
        log.info("✅ PRUEBA DE SOSTENIBILIDAD COMPLETADA - SISTEMA ESTABLE A LARGO PLAZO");
    }

    @Test
    @DisplayName("Estrés - Recuperación después de sobrecarga")
    void testRecoveryAfterOverload() {
        log.info("🔄 INICIANDO PRUEBA DE RECUPERACIÓN POST-SOBRECARGA");
        
        // Fase 1: Sobrecarga intensa
        log.info("Fase 1: Aplicando sobrecarga extrema...");
        int overloadOperations = 1000;
        
        Instant overloadStart = Instant.now();
        
        StepVerifier.create(
            Flux.range(1, overloadOperations)
                .flatMap(i -> {
                    User user = User.builder()
                            .firstname("Overload")
                            .lastname("User" + i)
                            .email("overload.user" + i + "@recovery.com")
                            .documentType(DocumentType.DNI)
                            .documentNumber("4000" + String.format("%05d", i))
                            .phone("970" + String.format("%06d", i))
                            .roles(Set.of(Role.auxiliary.name()))
                            .institutionId("RECOVERY_TEST_INST")
                            .build();

                    return keycloakService.createUser(user);
                }, 200) // Paralelismo muy alto para crear sobrecarga
        )
        .expectNextCount(overloadOperations)
        .verifyComplete();
        
        Instant overloadEnd = Instant.now();
        Duration overloadDuration = Duration.between(overloadStart, overloadEnd);
        
        log.info("   • Sobrecarga completada en {} ms", overloadDuration.toMillis());
        
        // Fase 2: Período de recuperación
        log.info("Fase 2: Período de recuperación (5 segundos)...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Fase 3: Operaciones normales post-recuperación
        log.info("Fase 3: Verificando operaciones normales...");
        int normalOperations = 100;
        AtomicInteger recoverySuccess = new AtomicInteger(0);
        
        Instant recoveryStart = Instant.now();
        
        StepVerifier.create(
            Flux.range(1, normalOperations)
                .flatMap(i -> {
                    User user = User.builder()
                            .firstname("Recovery")
                            .lastname("User" + i)
                            .email("recovery.user" + i + "@normal.com")
                            .documentType(DocumentType.DNI)
                            .documentNumber("5000" + String.format("%05d", i))
                            .phone("980" + String.format("%06d", i))
                            .roles(Set.of(Role.teacher.name()))
                            .institutionId("NORMAL_TEST_INST")
                            .build();

                    return keycloakService.createUser(user)
                            .doOnNext(id -> recoverySuccess.incrementAndGet());
                }, 10) // Paralelismo normal
        )
        .expectNextCount(normalOperations)
        .verifyComplete();
        
        Instant recoveryEnd = Instant.now();
        Duration recoveryDuration = Duration.between(recoveryStart, recoveryEnd);
        
        // Análisis de recuperación
        double overloadRate = overloadOperations / (overloadDuration.toMillis() / 1000.0);
        double recoveryRate = normalOperations / (recoveryDuration.toMillis() / 1000.0);
        double recoveryRatio = recoveryRate / overloadRate;
        
        log.info("📊 RESULTADOS PRUEBA DE RECUPERACIÓN:");
        log.info("   • Operaciones de sobrecarga: {}", overloadOperations);
        log.info("   • Tasa durante sobrecarga: {:.2f} ops/seg", overloadRate);
        log.info("   • Tiempo de sobrecarga: {} ms", overloadDuration.toMillis());
        log.info("   • Operaciones post-recuperación: {}", normalOperations);
        log.info("   • Operaciones exitosas post-recuperación: {}", recoverySuccess.get());
        log.info("   • Tasa post-recuperación: {:.2f} ops/seg", recoveryRate);
        log.info("   • Tiempo de recuperación: {} ms", recoveryDuration.toMillis());
        log.info("   • Ratio de recuperación: {:.2f}", recoveryRatio);
        
        // Verificaciones de recuperación
        assert recoverySuccess.get() == normalOperations : "Debe recuperarse completamente";
        assert recoveryRate > 5.0 : "Debe mantener tasa mínima después de recuperación";
        assert recoveryDuration.toSeconds() < 60 : "Recuperación debe ser rápida";
        
        log.info("✅ PRUEBA DE RECUPERACIÓN COMPLETADA - SISTEMA SE RECUPERA EXITOSAMENTE");
    }

    // Métodos auxiliares para operaciones específicas
    private Mono<String> createUserOperation(int index) {
        User user = User.builder()
                .firstname("Sustained")
                .lastname("User" + index)
                .email("sustained.user" + index + "@long.com")
                .documentType(DocumentType.DNI)
                .documentNumber("6000" + String.format("%05d", index))
                .phone("990" + String.format("%06d", index))
                .roles(Set.of(Role.teacher.name()))
                .institutionId("SUSTAINED_TEST_INST")
                .build();

        return keycloakService.createUser(user);
    }

    private Mono<Void> sendEmailOperation(int index) {
        return emailService.sendTemporaryCredentialsEmail(
                "sustained.email" + index + "@long.com",
                "SustainedUser" + index,
                "sustainedPass" + index,
                "sustainedToken" + index
        );
    }

    private Mono<String> combinedOperation(int index) {
        return createUserOperation(index)
                .flatMap(keycloakId -> 
                    sendEmailOperation(index)
                            .then(keycloakService.changePassword(keycloakId, "newPass" + index))
                            .thenReturn(keycloakId)
                );
    }
}