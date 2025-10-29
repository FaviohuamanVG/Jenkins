package pe.edu.vallegrande.vgmsuser;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba básica de la aplicación Spring Boot
 */
@SpringBootTest
@ActiveProfiles("test")
class VgMsUserApplicationTests {

    @Test
    void contextLoads() {
        // Test que verifica que el contexto de Spring se carga correctamente
        // con la configuración de mocks para pruebas
    }
}