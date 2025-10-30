package pe.edu.vallegrande.vgmsuser.selenium.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import pe.edu.vallegrande.vgmsuser.selenium.config.SeleniumBaseTest;
import pe.edu.vallegrande.vgmsuser.selenium.utils.ApiTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para AuthRest usando Selenium WebDriver
 * Verifica los endpoints de autenticación y reset de password
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthRestSeleniumTest extends SeleniumBaseTest {

    private TestRestTemplate restTemplate;
    
    @BeforeEach
    void setUpTest() {
        restTemplate = new TestRestTemplate();
    }
    
    @AfterAll
    static void cleanUp() {
        quitDriver();
    }
    
    @Test
    @Order(1)
    @DisplayName("GET /api/v1/reset/password-status/{keycloakId} - Verificar estado de password")
    void testGetPasswordStatus() {
        // Arrange
        String keycloakId = "123e4567-e89b-12d3-a456-426614174000";
        String endpoint = "/api/v1/reset/password-status/" + keycloakId;
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Probando endpoint: {}", fullUrl);
        
        // Act - Usar JavaScript para hacer la llamada
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send();
                return {
                    status: xhr.status,
                    response: xhr.responseText,
                    error: null
                };
            } catch(e) {
                return {
                    status: 0,
                    response: '',
                    error: e.message
                };
            }
            """, fullUrl);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        // El endpoint puede devolver diferentes códigos dependiendo del estado
        Long status = (Long) result.get("status");
        assertTrue(status >= 200 && status < 500, 
            "El endpoint debe responder con un código válido, recibido: " + status);
        
        String response = (String) result.get("response");
        assertNotNull(response, "La respuesta no debe ser null");
        
        if (status == 200) {
            // Si es exitoso, debe tener la estructura esperada
            assertTrue(ApiTestUtils.validateJsonResponse(response, "isTemporary", "message"),
                "La respuesta exitosa debe contener 'isTemporary' y 'message'");
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("POST /api/v1/reset/request-password-reset - Solicitar reset de password")
    void testRequestPasswordReset() {
        // Arrange
        String endpoint = "/api/v1/reset/request-password-reset";
        String fullUrl = getEndpointUrl(endpoint);
        
        Map<String, Object> requestData = ApiTestUtils.createPasswordResetRequest("test@vallegrande.edu.pe");
        String jsonData = ApiTestUtils.toJson(requestData);
        
        log.info("Probando endpoint: {} con datos: {}", fullUrl, jsonData);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send('%s');
                return {
                    status: xhr.status,
                    response: xhr.responseText,
                    error: null
                };
            } catch(e) {
                return {
                    status: 0,
                    response: '',
                    error: e.message
                };
            }
            """, fullUrl, jsonData.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        
        // El endpoint siempre devuelve 200 por seguridad, independientemente de si el usuario existe
        assertTrue(status == 200 || status == 400, 
            "El endpoint debe responder con 200 (éxito) o 400 (validación), recibido: " + status);
        
        assertNotNull(response, "La respuesta no debe ser null");
        
        if (status == 200) {
            assertTrue(response.contains("enlace para restablecer"),
                "La respuesta exitosa debe contener el mensaje de confirmación");
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("POST /api/v1/reset/reset-password - Reset password con token")
    void testResetPassword() {
        // Arrange
        String endpoint = "/api/v1/reset/reset-password";
        String fullUrl = getEndpointUrl(endpoint);
        
        Map<String, Object> requestData = Map.of(
            "token", "test-token-123",
            "newPassword", "NewPassword123!"
        );
        String jsonData = ApiTestUtils.toJson(requestData);
        
        log.info("Probando endpoint: {} con datos: {}", fullUrl, jsonData);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send('%s');
                return {
                    status: xhr.status,
                    response: xhr.responseText,
                    error: null
                };
            } catch(e) {
                return {
                    status: 0,
                    response: '',
                    error: e.message
                };
            }
            """, fullUrl, jsonData.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        
        // Puede ser 200 (éxito), 400 (token inválido), o 404 (token no encontrado)
        assertTrue(status >= 200 && status < 500, 
            "El endpoint debe responder con un código válido, recibido: " + status);
        
        assertNotNull(response, "La respuesta no debe ser null");
    }
    
    @Test
    @Order(4)
    @DisplayName("POST /api/v1/reset/generate-reset-token/{keycloakId} - Generar token de reset")
    void testGenerateResetToken() {
        // Arrange
        String keycloakId = "123e4567-e89b-12d3-a456-426614174000";
        String endpoint = "/api/v1/reset/generate-reset-token/" + keycloakId;
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Probando endpoint: {}", fullUrl);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send();
                return {
                    status: xhr.status,
                    response: xhr.responseText,
                    error: null
                };
            } catch(e) {
                return {
                    status: 0,
                    response: '',
                    error: e.message
                };
            }
            """, fullUrl);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        
        assertTrue(status >= 200 && status < 500, 
            "El endpoint debe responder con un código válido, recibido: " + status);
        
        assertNotNull(response, "La respuesta no debe ser null");
    }
    
    @Test
    @Order(5)
    @DisplayName("Navegación y disponibilidad general de endpoints")
    void testEndpointAvailability() {
        // Arrange - Lista de endpoints para verificar disponibilidad
        String[] endpoints = {
            "/api/v1/reset/password-status/test-id",
            "/api/v1/reset/generate-reset-token/test-id"
        };
        
        // Act & Assert
        for (String endpoint : endpoints) {
            String fullUrl = getEndpointUrl(endpoint);
            log.info("Verificando disponibilidad de: {}", fullUrl);
            
            driver.get("about:blank");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
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
            
            Long status = (Long) js.executeScript(script);
            
            // Verificar que el endpoint esté disponible (no 404 o error de red)
            assertTrue(status != 0 && status != 404, 
                String.format("Endpoint %s debe estar disponible, código recibido: %d", endpoint, status));
        }
    }
}