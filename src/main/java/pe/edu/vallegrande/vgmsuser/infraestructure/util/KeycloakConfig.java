package pe.edu.vallegrande.vgmsuser.infraestructure.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.realm-master}")
    private String realmMaster;
    @Value("${keycloak.admin-cli}")
    private String adminCli;
    @Value("${keycloak.user-console}")
    private String userConsole;
    @Value("${keycloak.password-console}")
    private String passwordConsole;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realmMaster)
                .clientId(adminCli)
                .username(userConsole)
                .password(passwordConsole)
                .clientSecret(clientSecret)
                .build();
    }
}

