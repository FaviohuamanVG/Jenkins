package pe.edu.vallegrande.vgmsuser.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.PasswordStatus;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO para representar un usuario completo desde Keycloak
 * con todos sus atributos personalizados
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeycloakUserDto {
    
    private String keycloakId;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private DocumentType documentType;
    private String documentNumber;
    private String phone;
    private UserStatus status;
    private PasswordStatus passwordStatus;
    private LocalDateTime passwordCreatedAt;
    private String passwordResetToken;
    private String institutionId; // ID de la institución asignada
    private Set<String> roles;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
