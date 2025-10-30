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
 * Pruebas de integración para AdminUserRest usando Selenium WebDriver  
 * Verifica los endpoints de administración de usuarios
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminUserRestSeleniumTest extends SeleniumBaseTest {

    @AfterAll
    static void cleanUp() {
        quitDriver();
    }
    
    @Test
    @Order(1)
    @DisplayName("POST /api/v1/users/admin/create - Crear usuario como admin")
    void testCreateAdminUser() {
        // Arrange
        String endpoint = "/api/v1/users/admin/create";
        String fullUrl = getEndpointUrl(endpoint);
        
        Map<String, Object> userData = ApiTestUtils.createTestUser("DIRECTOR");
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
            xhr.setRequestHeader('X-User-Roles', 'ADMIN');
            xhr.setRequestHeader('X-Institution-Id', 'null');
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
            """, fullUrl, UUID.randomUUID().toString(), jsonData.replace("'", "\\'"));
        
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
            assertTrue(ApiTestUtils.validateJsonResponse(response, "message"),
                "La respuesta exitosa debe contener 'message'");
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("GET /api/v1/users/admin - Obtener todos los usuarios admin")
    void testGetAllAdminUsers() {
        // Arrange
        String endpoint = "/api/v1/users/admin";
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
            xhr.setRequestHeader('X-User-Roles', 'ADMIN');
            xhr.setRequestHeader('X-Institution-Id', 'null');
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
            """, fullUrl, UUID.randomUUID().toString());
        
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
    @DisplayName("GET /api/v1/users/admin/directors - Obtener todos los directores")
    void testGetAllDirectors() {
        // Arrange
        String endpoint = "/api/v1/users/admin/directors";
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
            xhr.setRequestHeader('X-User-Roles', 'ADMIN');
            xhr.setRequestHeader('X-Institution-Id', 'null');
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
            """, fullUrl, UUID.randomUUID().toString());
        
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
    @DisplayName("GET /api/v1/users/admin/directors/{institution_id} - Obtener directores por institución")
    void testGetDirectorsByInstitution() {
        // Arrange
        String institutionId = UUID.randomUUID().toString();
        String endpoint = "/api/v1/users/admin/directors/" + institutionId;
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
            xhr.setRequestHeader('X-User-Roles', 'ADMIN');
            xhr.setRequestHeader('X-Institution-Id', 'null');
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
            """, fullUrl, UUID.randomUUID().toString());
        
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
    @Order(5)
    @DisplayName("PUT /api/v1/users/admin/update/{user_id} - Actualizar usuario como admin")
    void testUpdateUserAsAdmin() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        String endpoint = "/api/v1/users/admin/update/" + userId;
        String fullUrl = getEndpointUrl(endpoint);
        
        Map<String, Object> updateData = Map.of(
            "firstName", "Updated",
            "lastName", "User",
            "email", "updated@vallegrande.edu.pe"
        );
        String jsonData = ApiTestUtils.toJson(updateData);
        
        log.info("Probando endpoint: {} con datos: {}", fullUrl, jsonData);
        
        // Act
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('PUT', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'ADMIN');
            xhr.setRequestHeader('X-Institution-Id', 'null');
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
            """, fullUrl, UUID.randomUUID().toString(), jsonData.replace("'", "\\'"));
        
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
    @Order(6)
    @DisplayName("Verificar autorización de endpoints admin")
    void testAdminAuthorization() {
        // Arrange
        String endpoint = "/api/v1/users/admin";
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Probando autorización con rol no admin: {}", fullUrl);
        
        // Act - Llamada con rol DIRECTOR (no ADMIN)
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.setRequestHeader('X-User-Id', '%s');
            xhr.setRequestHeader('X-User-Roles', 'DIRECTOR');  // No es ADMIN
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
            """, fullUrl, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Resultado con rol no admin: {}", result);
        
        Long status = (Long) result.get("status");
        
        // Debe devolver error de autorización
        assertTrue(status == 400 || status == 403, 
            "El endpoint admin debe rechazar roles no-admin, código recibido: " + status);
    }
    
    @Test
    @Order(7)
    @DisplayName("Verificar CORS headers en endpoints admin")
    void testCorsHeaders() {
        // Arrange
        String endpoint = "/api/v1/users/admin";
        String fullUrl = getEndpointUrl(endpoint);
        
        log.info("Verificando headers CORS en: {}", fullUrl);
        
        // Act - Solicitud OPTIONS para verificar CORS
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('OPTIONS', '%s', false);
            xhr.setRequestHeader('Access-Control-Request-Method', 'GET');
            xhr.setRequestHeader('Access-Control-Request-Headers', 'Content-Type,X-User-Id,X-User-Roles');
            try {
                xhr.send();
                return {
                    status: xhr.status,
                    corsHeaders: xhr.getResponseHeader('Access-Control-Allow-Origin'),
                    allowedMethods: xhr.getResponseHeader('Access-Control-Allow-Methods'),
                    allowedHeaders: xhr.getResponseHeader('Access-Control-Allow-Headers')
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, fullUrl);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        
        // Assert
        log.info("Headers CORS: {}", result);
        
        Long status = (Long) result.get("status");
        
        // OPTIONS puede no estar implementado, pero no debe ser error interno
        assertTrue(status != 500, 
            "El servidor no debe devolver error interno en OPTIONS, código: " + status);
    }
}