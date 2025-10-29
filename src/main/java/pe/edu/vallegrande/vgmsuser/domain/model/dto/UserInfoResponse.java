package pe.edu.vallegrande.vgmsuser.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para endpoints de comunicación entre microservicios
 * Representa la información básica del usuario necesaria para otros servicios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    
    /**
     * ID único del usuario en Keycloak (UUID)
     */
    private String id;
    
    /**
     * Email del usuario
     */
    private String email;
    
    /**
     * Lista de roles del usuario (ADMIN, DIRECTOR, TEACHER, AUXILIARY, SECRETARY)
     */
    private List<String> roles;
    
    /**
     * ID de la institución asignada (null para ADMIN)
     */
    private String institutionId;
    
    /**
     * Estado del usuario: A (Activo), I (Inactivo)
     */
    private String status;
    
    /**
     * Indica si el usuario tiene acceso al sistema
     * true si enabled=true y status=A
     */
    private boolean hasAccess;
}
