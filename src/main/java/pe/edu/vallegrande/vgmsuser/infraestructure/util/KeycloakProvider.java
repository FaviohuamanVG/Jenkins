package pe.edu.vallegrande.vgmsuser.infraestructure.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class KeycloakProvider {
    private final Keycloak keycloak;
    private final String realmName;

    @Autowired
    public KeycloakProvider(Keycloak keycloak, @Value("${keycloak.realm-name}") String realmName) {
        this.keycloak = keycloak;
        this.realmName = realmName;
    }

    public RealmResource getRealmResource() {
        return keycloak.realm(realmName);
    }

    public UsersResource getUserResource() {
        return getRealmResource().users();
    }

    public Keycloak getKeycloak() {
        return keycloak;
    }

    public String getRealmName() {
        return realmName;
    }
}
