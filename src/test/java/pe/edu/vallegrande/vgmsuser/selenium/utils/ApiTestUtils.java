package pe.edu.vallegrande.vgmsuser.selenium.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

/**
 * Utilidades para pruebas de API REST con Selenium
 * Ayuda con autenticación, headers y llamadas a endpoints
 */
@Slf4j
public class ApiTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Genera headers HTTP comunes para las pruebas
     */
    public static HttpHeaders createCommonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
    
    /**
     * Genera headers con autenticación para pruebas de administrador
     */
    public static HttpHeaders createAdminHeaders() {
        HttpHeaders headers = createCommonHeaders();
        headers.set("X-User-Id", UUID.randomUUID().toString());
        headers.set("X-User-Roles", "ADMIN");
        headers.set("X-Institution-Id", "null");
        return headers;
    }
    
    /**
     * Genera headers con autenticación para pruebas de director
     */
    public static HttpHeaders createDirectorHeaders(String institutionId) {
        HttpHeaders headers = createCommonHeaders();
        headers.set("X-User-Id", UUID.randomUUID().toString());
        headers.set("X-User-Roles", "DIRECTOR");
        headers.set("X-Institution-Id", institutionId != null ? institutionId : UUID.randomUUID().toString());
        return headers;
    }
    
    /**
     * Genera headers con autenticación para pruebas de personal
     */
    public static HttpHeaders createPersonalHeaders(String role, String institutionId) {
        HttpHeaders headers = createCommonHeaders();
        headers.set("X-User-Id", UUID.randomUUID().toString());
        headers.set("X-User-Roles", role); // TEACHER, AUXILIARY, SECRETARY
        headers.set("X-Institution-Id", institutionId != null ? institutionId : UUID.randomUUID().toString());
        return headers;
    }
    
    /**
     * Convierte un objeto a JSON string
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Error convirtiendo objeto a JSON: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Convierte JSON string a objeto
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error convirtiendo JSON a objeto: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Ejecuta JavaScript en el navegador para hacer llamadas AJAX
     */
    public static String executeAjaxCall(WebDriver driver, String method, String url, String data, Map<String, String> headers) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        StringBuilder headersJs = new StringBuilder();
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                headersJs.append(String.format("xhr.setRequestHeader('%s', '%s');", 
                    header.getKey(), header.getValue()));
            }
        }
        
        String script = String.format("""
            var xhr = new XMLHttpRequest();
            xhr.open('%s', '%s', false);
            %s
            xhr.send(%s);
            return xhr.responseText;
            """, method, url, headersJs.toString(), data != null ? "'" + data + "'" : "null");
        
        try {
            Object result = js.executeScript(script);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            log.error("Error ejecutando llamada AJAX: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Genera un usuario de prueba con datos válidos
     */
    public static Map<String, Object> createTestUser(String role) {
        String uuid = UUID.randomUUID().toString();
        
        return Map.of(
            "email", "test-" + uuid + "@vallegrande.edu.pe",
            "username", "testuser" + uuid.substring(0, 8),
            "firstName", "Test",
            "lastName", "User",
            "role", role,
            "institutionId", role.equals("ADMIN") ? null : UUID.randomUUID().toString(),
            "enabled", true,
            "emailVerified", true
        );
    }
    
    /**
     * Genera un request de reset de password
     */
    public static Map<String, Object> createPasswordResetRequest(String emailOrUsername) {
        return Map.of("emailOrUsername", emailOrUsername);
    }
    
    /**
     * Valida que una respuesta JSON contenga los campos esperados
     */
    public static boolean validateJsonResponse(String jsonResponse, String... expectedFields) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(jsonResponse, Map.class);
            
            for (String field : expectedFields) {
                if (!response.containsKey(field)) {
                    log.warn("Campo '{}' no encontrado en respuesta", field);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error validando respuesta JSON: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae un valor específico de una respuesta JSON
     */
    public static Object extractJsonValue(String jsonResponse, String fieldName) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(jsonResponse, Map.class);
            return response.get(fieldName);
        } catch (Exception e) {
            log.error("Error extrayendo valor '{}' de JSON: {}", fieldName, e.getMessage());
            return null;
        }
    }
}