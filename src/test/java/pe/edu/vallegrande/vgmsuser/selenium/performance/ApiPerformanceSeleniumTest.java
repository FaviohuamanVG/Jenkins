package pe.edu.vallegrande.vgmsuser.selenium.performance;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import pe.edu.vallegrande.vgmsuser.selenium.config.SeleniumBaseTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de rendimiento básico para la API REST usando Selenium
 * Mide tiempos de respuesta y verifica la estabilidad bajo carga ligera
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiPerformanceSeleniumTest extends SeleniumBaseTest {

    private static final int PERFORMANCE_RUNS = 5;
    private static final long MAX_RESPONSE_TIME_MS = 5000; // 5 segundos máximo
    
    @AfterAll
    static void cleanUp() {
        quitDriver();
    }
    
    @Test
    @Order(1)
    @DisplayName("Rendimiento - Tiempo de respuesta de endpoints GET")
    void testGetEndpointsResponseTime() {
        // Arrange
        String[] endpoints = {
            "/api/v1/reset/password-status/" + UUID.randomUUID().toString(),
            "/api/v1/users/admin",
            "/api/v1/users/admin/directors"
        };
        
        // Headers para autenticación admin
        String adminUserId = UUID.randomUUID().toString();
        
        log.info("Midiendo tiempos de respuesta para {} endpoints", endpoints.length);
        
        for (String endpoint : endpoints) {
            List<Long> responseTimes = new ArrayList<>();
            
            for (int i = 0; i < PERFORMANCE_RUNS; i++) {
                // Act
                String fullUrl = getEndpointUrl(endpoint);
                long startTime = System.currentTimeMillis();
                
                driver.get("about:blank");
                JavascriptExecutor js = (JavascriptExecutor) driver;
                
                String script = String.format("""
                    var startTime = performance.now();
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', '%s', false);
                    xhr.setRequestHeader('Accept', 'application/json');
                    xhr.setRequestHeader('X-User-Id', '%s');
                    xhr.setRequestHeader('X-User-Roles', 'ADMIN');
                    xhr.setRequestHeader('X-Institution-Id', 'null');
                    try {
                        xhr.send();
                        var endTime = performance.now();
                        return {
                            status: xhr.status,
                            responseTime: endTime - startTime,
                            timestamp: new Date().toISOString()
                        };
                    } catch(e) {
                        var endTime = performance.now();
                        return {
                            status: 0,
                            responseTime: endTime - startTime,
                            error: e.message
                        };
                    }
                    """, fullUrl, adminUserId);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
                
                // Calcular tiempo total (incluyendo overhead)
                long totalTime = System.currentTimeMillis() - startTime;
                Double jsResponseTime = (Double) result.get("responseTime");
                
                responseTimes.add(jsResponseTime != null ? jsResponseTime.longValue() : totalTime);
                
                log.debug("Endpoint: {} - Run {}: {} ms (JS: {} ms)", 
                    endpoint, i + 1, totalTime, jsResponseTime);
                
                // Pausa entre llamadas
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Assert
            long avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            
            log.info("Endpoint: {} - Promedio: {} ms, Min: {} ms, Max: {} ms", 
                endpoint, avgResponseTime, minResponseTime, maxResponseTime);
            
            assertTrue(avgResponseTime < MAX_RESPONSE_TIME_MS, 
                String.format("Tiempo promedio (%d ms) debe ser menor a %d ms para %s", 
                    avgResponseTime, MAX_RESPONSE_TIME_MS, endpoint));
            
            assertTrue(maxResponseTime < MAX_RESPONSE_TIME_MS * 2, 
                String.format("Tiempo máximo (%d ms) debe ser menor a %d ms para %s", 
                    maxResponseTime, MAX_RESPONSE_TIME_MS * 2, endpoint));
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Rendimiento - Tiempo de respuesta de endpoints POST")
    void testPostEndpointsResponseTime() {
        // Arrange
        Map<String, String> postEndpoints = Map.of(
            "/api/v1/reset/request-password-reset", """
                {"emailOrUsername": "test@vallegrande.edu.pe"}
                """,
            "/api/v1/reset/reset-password", """
                {"token": "test-token", "newPassword": "NewPassword123!"}
                """
        );
        
        log.info("Midiendo tiempos de respuesta para {} endpoints POST", postEndpoints.size());
        
        for (Map.Entry<String, String> entry : postEndpoints.entrySet()) {
            String endpoint = entry.getKey();
            String jsonData = entry.getValue().trim();
            
            List<Long> responseTimes = new ArrayList<>();
            
            for (int i = 0; i < PERFORMANCE_RUNS; i++) {
                // Act
                String fullUrl = getEndpointUrl(endpoint);
                long startTime = System.currentTimeMillis();
                
                driver.get("about:blank");
                JavascriptExecutor js = (JavascriptExecutor) driver;
                
                String script = String.format("""
                    var startTime = performance.now();
                    var xhr = new XMLHttpRequest();
                    xhr.open('POST', '%s', false);
                    xhr.setRequestHeader('Content-Type', 'application/json');
                    xhr.setRequestHeader('Accept', 'application/json');
                    try {
                        xhr.send('%s');
                        var endTime = performance.now();
                        return {
                            status: xhr.status,
                            responseTime: endTime - startTime,
                            timestamp: new Date().toISOString()
                        };
                    } catch(e) {
                        var endTime = performance.now();
                        return {
                            status: 0,
                            responseTime: endTime - startTime,
                            error: e.message
                        };
                    }
                    """, fullUrl, jsonData.replace("'", "\\'"));
                
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
                
                long totalTime = System.currentTimeMillis() - startTime;
                Double jsResponseTime = (Double) result.get("responseTime");
                
                responseTimes.add(jsResponseTime != null ? jsResponseTime.longValue() : totalTime);
                
                log.debug("Endpoint POST: {} - Run {}: {} ms (JS: {} ms)", 
                    endpoint, i + 1, totalTime, jsResponseTime);
                
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Assert
            long avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            
            log.info("Endpoint POST: {} - Promedio: {} ms, Max: {} ms", 
                endpoint, avgResponseTime, maxResponseTime);
            
            assertTrue(avgResponseTime < MAX_RESPONSE_TIME_MS, 
                String.format("Tiempo promedio POST (%d ms) debe ser menor a %d ms para %s", 
                    avgResponseTime, MAX_RESPONSE_TIME_MS, endpoint));
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Estabilidad - Llamadas consecutivas sin degradación")
    void testConsecutiveCallsStability() {
        // Arrange
        String endpoint = "/api/v1/reset/password-status/" + UUID.randomUUID().toString();
        String fullUrl = getEndpointUrl(endpoint);
        int consecutiveCalls = 10;
        
        log.info("Probando estabilidad con {} llamadas consecutivas", consecutiveCalls);
        
        List<Long> responseTimes = new ArrayList<>();
        List<Long> statusCodes = new ArrayList<>();
        
        // Act
        for (int i = 0; i < consecutiveCalls; i++) {
            driver.get("about:blank");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            String script = String.format("""
                var startTime = performance.now();
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '%s', false);
                xhr.setRequestHeader('Accept', 'application/json');
                try {
                    xhr.send();
                    var endTime = performance.now();
                    return {
                        status: xhr.status,
                        responseTime: endTime - startTime
                    };
                } catch(e) {
                    var endTime = performance.now();
                    return {
                        status: 0,
                        responseTime: endTime - startTime,
                        error: e.message
                    };
                }
                """, fullUrl);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
            
            Long status = (Long) result.get("status");
            Double responseTime = (Double) result.get("responseTime");
            
            statusCodes.add(status);
            responseTimes.add(responseTime != null ? responseTime.longValue() : 0);
            
            log.debug("Llamada {}: Status {}, Tiempo {} ms", i + 1, status, responseTime);
        }
        
        // Assert
        // Verificar que no hay degradación significativa (último 20% no debe ser >50% más lento que primero 20%)
        int firstBatch = consecutiveCalls / 5;
        int lastBatch = consecutiveCalls / 5;
        
        double avgFirstBatch = responseTimes.subList(0, firstBatch).stream()
            .mapToLong(Long::longValue).average().orElse(0);
        
        double avgLastBatch = responseTimes.subList(consecutiveCalls - lastBatch, consecutiveCalls).stream()
            .mapToLong(Long::longValue).average().orElse(0);
        
        log.info("Promedio primeras {} llamadas: {} ms", firstBatch, avgFirstBatch);
        log.info("Promedio últimas {} llamadas: {} ms", lastBatch, avgLastBatch);
        
        assertTrue(avgLastBatch < avgFirstBatch * 1.5, 
            String.format("No debe haber degradación significativa: último lote (%.1f ms) vs primer lote (%.1f ms)", 
                avgLastBatch, avgFirstBatch));
        
        // Verificar que la mayoría de llamadas fueron exitosas
        long successfulCalls = statusCodes.stream().filter(code -> code >= 200 && code < 500).count();
        assertTrue(successfulCalls >= consecutiveCalls * 0.8, 
            String.format("Al menos 80%% de llamadas deben ser exitosas: %d/%d", successfulCalls, consecutiveCalls));
    }
    
    @Test
    @Order(4)
    @DisplayName("Memoria - Verificar que no hay memory leaks en el navegador")
    void testBrowserMemoryUsage() {
        // Arrange
        String endpoint = "/api/v1/reset/password-status/" + UUID.randomUUID().toString();
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Verificando uso de memoria del navegador");
        
        // Act - Múltiples llamadas para verificar memory leaks
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        // Medir memoria inicial
        String memoryScript = """
            if (performance.memory) {
                return {
                    usedJSHeapSize: performance.memory.usedJSHeapSize,
                    totalJSHeapSize: performance.memory.totalJSHeapSize,
                    jsHeapSizeLimit: performance.memory.jsHeapSizeLimit
                };
            }
            return null;
            """;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> initialMemory = (Map<String, Object>) js.executeScript(memoryScript);
        
        if (initialMemory != null) {
            log.info("Memoria inicial: {} bytes", initialMemory.get("usedJSHeapSize"));
            
            // Hacer múltiples llamadas
            for (int i = 0; i < 20; i++) {
                String script = String.format("""
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', '%s', false);
                    xhr.setRequestHeader('Accept', 'application/json');
                    try {
                        xhr.send();
                        return xhr.status;
                    } catch(e) {
                        return 0;
                    }
                    """, fullUrl);
                
                js.executeScript(script);
            }
            
            // Forzar garbage collection si es posible
            js.executeScript("if (window.gc) { window.gc(); }");
            
            // Medir memoria final
            @SuppressWarnings("unchecked")
            Map<String, Object> finalMemory = (Map<String, Object>) js.executeScript(memoryScript);
            
            if (finalMemory != null) {
                Long initialUsed = ((Number) initialMemory.get("usedJSHeapSize")).longValue();
                Long finalUsed = ((Number) finalMemory.get("usedJSHeapSize")).longValue();
                
                log.info("Memoria final: {} bytes", finalUsed);
                log.info("Diferencia: {} bytes", finalUsed - initialUsed);
                
                // Assert - La memoria no debe crecer excesivamente (más de 10MB)
                long memoryGrowth = finalUsed - initialUsed;
                assertTrue(memoryGrowth < 10 * 1024 * 1024, 
                    String.format("El crecimiento de memoria (%d bytes) no debe exceder 10MB", memoryGrowth));
            } else {
                log.info("performance.memory no disponible, verificación de memoria omitida");
            }
        } else {
            log.info("performance.memory no soportado por el navegador, verificación omitida");
        }
    }
}