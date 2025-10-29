package pe.edu.vallegrande.vgmsuser.infraestructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del endpoint de validación de instituciones
 * GET /validate-institutions/{institutionId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionValidationResponse {
    
    /**
     * Nombre de la institución (null si no existe)
     */
    private String name;
    
    /**
     * Indica si la institución existe
     */
    private Boolean exists;
    
    /**
     * Indica si la institución está activa
     */
    private Boolean active;
    
    /**
     * Estado de la institución ("A" = Activo, null si no existe)
     */
    private String status;
    
    /**
     * Mensaje de error (presente cuando hay error de formato)
     */
    private String error;
}