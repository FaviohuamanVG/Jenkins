package pe.edu.vallegrande.vgmsuser.selenium.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import pe.edu.vallegrande.vgmsuser.selenium.config.SeleniumBaseTest;
import pe.edu.vallegrande.vgmsuser.selenium.utils.ApiTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para UserManagementRest usando Selenium WebDriver
 * Verifica los endpoints de gestión de usuarios con diferentes roles
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserManagementRestSeleniumTest extends SeleniumBaseTest {

    private String testInstitutionId;
    
    @BeforeEach
    void setUpTest() {
        testInstitutionId = UUID.randomUUID().toString();
    }
    
    @AfterAll
    static void cleanUp() {
        quitDriver();
    }
    
    @Test
    @Order(1)
    @DisplayName("POST /api/v1/users/director/create - Crear usuario como director")
    void testCreateStaffUserAsDirector() {
        // Arrange
        String endpoint = "/api/v1/users/director/create";
        String fullUrl = getEndpointUrl(endpoint);
        
        Map<String, Object> userData = ApiTestUtils.createTestUser("TEACHER");
        String jsonData = ApiTestUtils.toJson(userData);
        
        log.info("Probando endpoint: {} con datos: {}", fullUrl, jsonData);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'DIRECTOR');
            xhr.setRequestHeader('X-Institution-Id', '%s');
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
            """, fullUrl, UUID.randomUUID().toString(), testInstitutionId, 
            jsonData.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        
        assertTrue(status >= 200 && status < 500, 
            "El endpoint debe responder con un código válido, recibido: " + status);
        
        assertNotNull(response, "La respuesta no debe ser null");
        
        if (status == 201 || status == 200) {
            // Si es exitoso, debe tener mensaje de confirmación
            assertTrue(ApiTestUtils.validateJsonResponse(response, "message"),
                "La respuesta exitosa debe contener 'message'");
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("GET /api/v1/users/director/staff - Obtener staff como director")
    void testGetAllStaffAsDirector() {
        // Arrange
        String endpoint = "/api/v1/users/director/staff";
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Probando endpoint: {}", fullUrl);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'DIRECTOR');
            xhr.setRequestHeader('X-Institution-Id', '%s');
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
            """, fullUrl, UUID.randomUUID().toString(), testInstitutionId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        
        assertTrue(status >= 200 && status < 500, 
            "El endpoint debe responder con un código válido, recibido: " + status);
        
        assertNotNull(response, "La respuesta no debe ser null");
        
        if (status == 200) {
            assertTrue(ApiTestUtils.validateJsonResponse(response, "message", "total_users", "users"),
                "La respuesta exitosa debe contener 'message', 'total_users' y 'users'");
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("GET /api/v1/users/director/by-role/TEACHER - Obtener usuarios por rol")
    void testGetUsersByRole() {
        // Arrange
        String role = "TEACHER";
        String endpoint = "/api/v1/users/director/by-role/" + role;
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Probando endpoint: {}", fullUrl);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'DIRECTOR');
            xhr.setRequestHeader('X-Institution-Id', '%s');
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
            """, fullUrl, UUID.randomUUID().toString(), testInstitutionId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado de la llamada: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        
        assertTrue(status >= 200 && status < 500, 
            "El endpoint debe responder con un código válido, recibido: " + status);
        
        assertNotNull(response, "La respuesta no debe ser null");
        
        if (status == 200) {
            assertTrue(ApiTestUtils.validateJsonResponse(response, "message", "total_users", "users"),
                "La respuesta exitosa debe contener 'message', 'total_users' y 'users'");
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("Verificar headers de autenticación requeridos")
    void testAuthenticationHeaders() {
        // Arrange
        String endpoint = "/api/v1/users/director/staff";
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Probando endpoint sin headers: {}", fullUrl);
        
        // Act - Llamada sin headers de autenticación
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            // No se incluyen headers de autenticación
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
        log.info("Resultado sin headers: {}", result);
        
        Long status = (Long) result.get("status");
        
        // Debe devolver error de autenticación (400 o 401)
        assertTrue(status == 400 || status == 401 || status == 403, 
            "El endpoint debe requerir autenticación, código recibido: " + status);
    }
    
    @Test
    @Order(5)
    @DisplayName("Verificar validación de roles")
    void testRoleValidation() {
        // Arrange
        String endpoint = "/api/v1/users/director/create";
        String fullUrl = getEndpointUrl(endpoint);
        
        Map<String, Object> userData = ApiTestUtils.createTestUser("TEACHER");
        String jsonData = ApiTestUtils.toJson(userData);
        
        log.info("Probando endpoint con rol incorrecto: {}", fullUrl);
        
        // Act - Llamada con rol TEACHER (debería ser DIRECTOR)
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'TEACHER');  // Rol incorrecto
            xhr.setRequestHeader('X-Institution-Id', '%s');
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
            """, fullUrl, UUID.randomUUID().toString(), testInstitutionId, 
            jsonData.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado con rol incorrecto: {}", result);
        
        Long status = (Long) result.get("status");
        
        // Debe devolver error de autorización (403 o 400)
        assertTrue(status == 400 || status == 403, 
            "El endpoint debe validar roles correctamente, código recibido: " + status);
    }
    
    @Test
    @Order(6)
    @DisplayName("Verificar estructura de respuestas JSON")
    void testJsonResponseStructure() {
        // Arrange
        String endpoint = "/api/v1/users/director/staff";
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Verificando estructura JSON de: {}", fullUrl);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'DIRECTOR');
            xhr.setRequestHeader('X-Institution-Id', '%s');
            try {
                xhr.send();
                return {
                    status: xhr.status,
                    response: xhr.responseText,
                    contentType: xhr.getResponseHeader('Content-Type')
                };
            } catch(e) {
                return {
                    status: 0,
                    response: '',
                    error: e.message
                };
            }
            """, fullUrl, UUID.randomUUID().toString(), testInstitutionId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Estructura de respuesta: {}", result);
        
        Long status = (Long) result.get("status");
        String response = (String) result.get("response");
        String contentType = (String) result.get("contentType");
        
        if (status == 200) {
            // Verificar Content-Type
            assertTrue(contentType != null && contentType.contains("application/json"),
                "La respuesta debe ser JSON, Content-Type: " + contentType);
            
            // Verificar que es JSON válido
            assertTrue(response.startsWith("{") || response.startsWith("["),
                "La respuesta debe ser JSON válido");
            
            // Verificar estructura esperada
            assertTrue(ApiTestUtils.validateJsonResponse(response, "message"),
                "La respuesta debe contener al menos 'message'");
        }
    }
}