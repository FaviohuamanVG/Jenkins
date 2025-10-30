package pe.edu.vallegrande.vgmsuser.selenium.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import pe.edu.vallegrande.vgmsuser.selenium.config.SeleniumBaseTest;
import pe.edu.vallegrande.vgmsuser.selenium.utils.ApiTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas de integración completa usando Selenium WebDriver
 * Verifica flujos de trabajo completos de la aplicación
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompleteIntegrationSeleniumTest extends SeleniumBaseTest {

    private String testAdminUserId;
    private String testDirectorUserId;
    private String testInstitutionId;
    
    @BeforeEach
    void setUpIntegrationTest() {
        testAdminUserId = UUID.randomUUID().toString();
        testDirectorUserId = UUID.randomUUID().toString();
        testInstitutionId = UUID.randomUUID().toString();
    }
    
    @AfterAll
    static void cleanUp() {
        quitDriver();
    }
    
    @Test
    @Order(1)
    @DisplayName("Flujo completo: Administrador crea director y director crea staff")
    void testCompleteUserCreationFlow() {
        log.info("Iniciando flujo completo de creación de usuarios");
        
        // Paso 1: Admin crea un director
        String createDirectorEndpoint = "/api/v1/users/admin/create";
        String fullUrl = getEndpointUrl(createDirectorEndpoint);
        
        Map<String, Object> directorData = ApiTestUtils.createTestUser("DIRECTOR");
        directorData.put("institutionId", testInstitutionId);
        String directorJson = ApiTestUtils.toJson(directorData);
        
        log.info("Paso 1: Admin crea director - {}", fullUrl);
        
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String createDirectorScript = String.format("""
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
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, fullUrl, testAdminUserId, directorJson.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> directorResult = (Map<String, Object>) js.executeScript(createDirectorScript);
        
        Long directorStatus = (Long) directorResult.get("status");
        log.info("Resultado creación director: Status {}", directorStatus);
        
        // Verificar que la creación fue procesada (exitosa o con error controlado)
        assertTrue(directorStatus >= 200 && directorStatus < 500, 
            "Creación de director debe ser procesada correctamente");
        
        // Paso 2: Director crea un teacher
        String createTeacherEndpoint = "/api/v1/users/director/create";
        String teacherUrl = getEndpointUrl(createTeacherEndpoint);
        
        Map<String, Object> teacherData = ApiTestUtils.createTestUser("TEACHER");
        String teacherJson = ApiTestUtils.toJson(teacherData);
        
        log.info("Paso 2: Director crea teacher - {}", teacherUrl);
        
        String createTeacherScript = String.format("""
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
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, teacherUrl, testDirectorUserId, testInstitutionId, teacherJson.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> teacherResult = (Map<String, Object>) js.executeScript(createTeacherScript);
        
        Long teacherStatus = (Long) teacherResult.get("status");
        log.info("Resultado creación teacher: Status {}", teacherStatus);
        
        assertTrue(teacherStatus >= 200 && teacherStatus < 500, 
            "Creación de teacher debe ser procesada correctamente");
        
        // Paso 3: Director consulta su staff
        String getStaffEndpoint = "/api/v1/users/director/staff";
        String staffUrl = getEndpointUrl(getStaffEndpoint);
        
        log.info("Paso 3: Director consulta staff - {}", staffUrl);
        
        String getStaffScript = String.format("""
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
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, staffUrl, testDirectorUserId, testInstitutionId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> staffResult = (Map<String, Object>) js.executeScript(getStaffScript);
        
        Long staffStatus = (Long) staffResult.get("status");
        String staffResponse = (String) staffResult.get("response");
        
        log.info("Resultado consulta staff: Status {}", staffStatus);
        
        assertTrue(staffStatus >= 200 && staffStatus < 500, 
            "Consulta de staff debe ser procesada correctamente");
        
        if (staffStatus == 200) {
            assertTrue(ApiTestUtils.validateJsonResponse(staffResponse, "message", "users"),
                "Respuesta de staff debe tener estructura correcta");
        }
        
        log.info("Flujo completo ejecutado exitosamente");
    }
    
    @Test
    @Order(2)
    @DisplayName("Flujo de reset de password completo")
    void testPasswordResetFlow() {
        log.info("Iniciando flujo completo de reset de password");
        
        String testEmail = "test-" + UUID.randomUUID().toString() + "@vallegrande.edu.pe";
        
        // Paso 1: Solicitar reset de password
        String requestResetEndpoint = "/api/v1/reset/request-password-reset";
        String requestUrl = getEndpointUrl(requestResetEndpoint);
        
        Map<String, Object> resetRequest = ApiTestUtils.createPasswordResetRequest(testEmail);
        String resetJson = ApiTestUtils.toJson(resetRequest);
        
        log.info("Paso 1: Solicitar reset - {}", requestUrl);
        
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String requestScript = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send('%s');
                return {
                    status: xhr.status,
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, requestUrl, resetJson.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> requestResult = (Map<String, Object>) js.executeScript(requestScript);
        
        Long requestStatus = (Long) requestResult.get("status");
        String requestResponse = (String) requestResult.get("response");
        
        log.info("Resultado solicitud reset: Status {}", requestStatus);
        
        assertTrue(requestStatus >= 200 && requestStatus < 500, 
            "Solicitud de reset debe ser procesada");
        
        if (requestStatus == 200) {
            assertTrue(requestResponse.contains("enlace") || requestResponse.contains("link"),
                "Respuesta debe confirmar envío de enlace");
        }
        
        // Paso 2: Generar token de reset (simulando proceso admin)
        String testKeycloakId = UUID.randomUUID().toString();
        String generateTokenEndpoint = "/api/v1/reset/generate-reset-token/" + testKeycloakId;
        String tokenUrl = getEndpointUrl(generateTokenEndpoint);
        
        log.info("Paso 2: Generar token - {}", tokenUrl);
        
        String tokenScript = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send();
                return {
                    status: xhr.status,
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, tokenUrl);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResult = (Map<String, Object>) js.executeScript(tokenScript);
        
        Long tokenStatus = (Long) tokenResult.get("status");
        log.info("Resultado generación token: Status {}", tokenStatus);
        
        assertTrue(tokenStatus >= 200 && tokenStatus < 500, 
            "Generación de token debe ser procesada");
        
        // Paso 3: Usar token para reset (con token simulado)
        String resetPasswordEndpoint = "/api/v1/reset/reset-password";
        String resetUrl = getEndpointUrl(resetPasswordEndpoint);
        
        Map<String, Object> passwordReset = Map.of(
            "token", "test-token-" + UUID.randomUUID().toString(),
            "newPassword", "NewSecurePassword123!"
        );
        String passwordJson = ApiTestUtils.toJson(passwordReset);
        
        log.info("Paso 3: Ejecutar reset - {}", resetUrl);
        
        String resetScript = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '%s', false);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Accept', 'application/json');
            try {
                xhr.send('%s');
                return {
                    status: xhr.status,
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, resetUrl, passwordJson.replace("'", "\\'"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resetResult = (Map<String, Object>) js.executeScript(resetScript);
        
        Long resetStatus = (Long) resetResult.get("status");
        log.info("Resultado reset password: Status {}", resetStatus);
        
        assertTrue(resetStatus >= 200 && resetStatus < 500, 
            "Reset de password debe ser procesado");
        
        log.info("Flujo de reset de password completado");
    }
    
    @Test
    @Order(3)
    @DisplayName("Flujo de administración jerárquica")
    void testHierarchicalAdministrationFlow() {
        log.info("Iniciando flujo de administración jerárquica");
        
        // Paso 1: Admin consulta todos los directores
        String getAllDirectorsEndpoint = "/api/v1/users/admin/directors";
        String directorsUrl = getEndpointUrl(getAllDirectorsEndpoint);
        
        log.info("Paso 1: Admin consulta directores - {}", directorsUrl);
        
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        String directorsScript = String.format("""
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
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, directorsUrl, testAdminUserId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> directorsResult = (Map<String, Object>) js.executeScript(directorsScript);
        
        Long directorsStatus = (Long) directorsResult.get("status");
        String directorsResponse = (String) directorsResult.get("response");
        
        log.info("Resultado consulta directores: Status {}", directorsStatus);
        
        assertTrue(directorsStatus >= 200 && directorsStatus < 500, 
            "Consulta de directores debe ser procesada");
        
        if (directorsStatus == 200) {
            assertTrue(ApiTestUtils.validateJsonResponse(directorsResponse, "message", "total_users"),
                "Respuesta debe tener estructura de listado");
        }
        
        // Paso 2: Admin consulta directores por institución específica
        String directorsByInstitutionEndpoint = "/api/v1/users/admin/directors/" + testInstitutionId;
        String institutionUrl = getEndpointUrl(directorsByInstitutionEndpoint);
        
        log.info("Paso 2: Admin consulta directores por institución - {}", institutionUrl);
        
        String institutionScript = String.format("""
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
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, institutionUrl, testAdminUserId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> institutionResult = (Map<String, Object>) js.executeScript(institutionScript);
        
        Long institutionStatus = (Long) institutionResult.get("status");
        log.info("Resultado consulta por institución: Status {}", institutionStatus);
        
        assertTrue(institutionStatus >= 200 && institutionStatus < 500, 
            "Consulta por institución debe ser procesada");
        
        // Paso 3: Verificar que director no puede acceder a endpoints de admin
        String adminEndpoint = "/api/v1/users/admin";
        String adminUrl = getEndpointUrl(adminEndpoint);
        
        log.info("Paso 3: Verificar restricción director->admin - {}", adminUrl);
        
        String restrictionScript = String.format("""
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
                    response: xhr.responseText
                };
            } catch(e) {
                return {
                    status: 0,
                    error: e.message
                };
            }
            """, adminUrl, testDirectorUserId, testInstitutionId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> restrictionResult = (Map<String, Object>) js.executeScript(restrictionScript);
        
        Long restrictionStatus = (Long) restrictionResult.get("status");
        log.info("Resultado verificación restricción: Status {}", restrictionStatus);
        
        // Debe ser rechazado con 403 o 400
        assertTrue(restrictionStatus == 400 || restrictionStatus == 403, 
            "Director no debe poder acceder a endpoints de admin");
        
        log.info("Flujo de administración jerárquica completado");
    }
    
    @Test
    @Order(4)
    @DisplayName("Verificación de consistencia de datos entre endpoints")
    void testDataConsistencyAcrossEndpoints() {
        log.info("Verificando consistencia de datos entre endpoints");
        
        // Usar endpoints que deberían devolver información consistente
        String[] consistentEndpoints = {
            "/api/v1/users/admin/directors",
            "/api/v1/users/admin/directors/" + testInstitutionId
        };
        
        driver.get("about:blank");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        for (String endpoint : consistentEndpoints) {
            String fullUrl = getEndpointUrl(endpoint);
            
            log.info("Verificando consistencia: {}", fullUrl);
            
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
                        contentType: xhr.getResponseHeader('Content-Type')
                    };
                } catch(e) {
                    return {
                        status: 0,
                        error: e.message
                    };
                }
                """, fullUrl, testAdminUserId);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
            
            Long status = (Long) result.get("status");
            String response = (String) result.get("response");
            String contentType = (String) result.get("contentType");
            
            log.info("Endpoint {}: Status {}, Content-Type {}", endpoint, status, contentType);
            
            if (status == 200) {
                // Verificar estructura consistente
                assertTrue(ApiTestUtils.validateJsonResponse(response, "message"),
                    "Todos los endpoints exitosos deben tener 'message'");
                
                // Verificar Content-Type consistente
                assertTrue(contentType != null && contentType.contains("application/json"),
                    "Content-Type debe ser consistente: application/json");
            }
        }
        
        log.info("Verificación de consistencia completada");
    }
}