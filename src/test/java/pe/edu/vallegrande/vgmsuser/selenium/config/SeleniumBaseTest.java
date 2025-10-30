package pe.edu.vallegrande.vgmsuser.selenium.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

/**
 * Base class para pruebas de integración con Selenium WebDriver
 * Proporciona configuración común para pruebas de UI automatizadas
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class SeleniumBaseTest {

    protected static WebDriver driver;
    protected static WebDriverWait wait;
    
    @LocalServerPort
    protected int port;
    
    protected String baseUrl;
    
    // Configuración de navegadores
    private static final String BROWSER_PROPERTY = "selenium.browser";
    private static final String DEFAULT_BROWSER = "chrome";
    private static final boolean HEADLESS_MODE = Boolean.parseBoolean(
        System.getProperty("selenium.headless", "true")
    );
    
    @BeforeAll
    static void setupWebDriver() {
        String browser = System.getProperty(BROWSER_PROPERTY, DEFAULT_BROWSER).toLowerCase();
        
        log.info("Configurando WebDriver para navegador: {} (headless: {})", browser, HEADLESS_MODE);
        
        switch (browser) {
            case "chrome":
                setupChromeDriver();
                break;
            case "edge":
                setupEdgeDriver();
                break;
            case "remote-chrome":
                setupRemoteChromeDriver();
                break;
            default:
                log.warn("Navegador '{}' no soportado, usando Chrome por defecto", browser);
                setupChromeDriver();
        }
        
        // Configurar WebDriverWait con timeout de 10 segundos
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        log.info("WebDriver configurado exitosamente");
    }
    
    private static void setupChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        if (HEADLESS_MODE) {
            options.addArguments("--headless");
        }
        
        // Configuraciones adicionales para estabilidad
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-infobars",
            "--disable-notifications"
        );
        
        driver = new ChromeDriver(options);
        log.info("ChromeDriver configurado exitosamente");
    }
    
    private static void setupEdgeDriver() {
        // Usar Edge instalado localmente sin descarga automática
        String edgePath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
        
        EdgeOptions options = new EdgeOptions();
        options.setBinary(edgePath);
        
        if (HEADLESS_MODE) {
            options.addArguments("--headless");
        }
        
        // Configuraciones adicionales para estabilidad
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-infobars",
            "--disable-notifications",
            "--disable-web-security",
            "--allow-running-insecure-content"
        );
        
        try {
            driver = new EdgeDriver(options);
            log.info("EdgeDriver configurado exitosamente con Edge local");
        } catch (Exception e) {
            log.error("Error al inicializar EdgeDriver con Edge local, intentando con WebDriverManager fallback");
            try {
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver(options);
                log.info("EdgeDriver configurado exitosamente con WebDriverManager");
            } catch (Exception ex) {
                log.error("Error al inicializar EdgeDriver con WebDriverManager: {}", ex.getMessage());
                throw new RuntimeException("No se pudo inicializar EdgeDriver", ex);
            }
        }
        log.info("EdgeDriver configurado exitosamente");
    }
    
    private static void setupRemoteChromeDriver() {
        String hubUrl = System.getProperty("selenium.hub.url", "http://localhost:4444/wd/hub");
        
        ChromeOptions options = new ChromeOptions();
        
        if (HEADLESS_MODE) {
            options.addArguments("--headless");
        }
        
        // Configuraciones adicionales para Docker Selenium
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-infobars",
            "--disable-notifications",
            "--disable-web-security",
            "--allow-running-insecure-content"
        );
        
        try {
            URL hubURL = new URL(hubUrl);
            driver = new RemoteWebDriver(hubURL, options);
            log.info("RemoteWebDriver configurado exitosamente con Selenium Grid en: {}", hubUrl);
        } catch (Exception e) {
            log.error("Error al inicializar RemoteWebDriver: {}", e.getMessage());
            throw new RuntimeException("No se pudo conectar a Selenium Grid en: " + hubUrl, e);
        }
    }
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        log.info("Configurando prueba para URL base: {}", baseUrl);
        
        // Limpiar cookies y localStorage antes de cada prueba
        driver.manage().deleteAllCookies();
        driver.get("about:blank");
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                // Limpiar estado del navegador después de cada prueba
                driver.manage().deleteAllCookies();
                log.debug("Estado del navegador limpiado exitosamente");
            } catch (Exception e) {
                log.warn("Error al limpiar estado del navegador: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Método para cerrar el driver al final de todas las pruebas
     * Debe ser llamado manualmente en @AfterAll si es necesario
     */
    protected static void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver cerrado exitosamente");
            } catch (Exception e) {
                log.error("Error al cerrar WebDriver: {}", e.getMessage());
            } finally {
                driver = null;
                wait = null;
            }
        }
    }
    
    /**
     * Obtiene la URL completa para un endpoint
     */
    protected String getEndpointUrl(String endpoint) {
        return baseUrl + endpoint;
    }
    
    /**
     * Navega a un endpoint específico
     */
    protected void navigateToEndpoint(String endpoint) {
        String url = getEndpointUrl(endpoint);
        log.info("Navegando a: {}", url);
        driver.get(url);
    }
}