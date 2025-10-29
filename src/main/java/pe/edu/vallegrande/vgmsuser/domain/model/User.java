package pe.edu.vallegrande.vgmsuser.domain.model;

import lombok.*;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Username is required")
    private String username;
    
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    // Campos opcionales para usuarios admin (pueden ser null)
    private String firstname;
    
    private String lastname;
    
    // Password es opcional ya que se genera automáticamente para usuarios admin
    private String password;
    
    private Set<String> roles;
    
    // Campos adicionales eliminados de MongoDB
    @NotNull(message = "Document type is required")
    private DocumentType documentType;
    
    @NotBlank(message = "Document number is required")
    private String documentNumber;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number format is invalid")
    private String phone;
    
    @Builder.Default
    private UserStatus status = UserStatus.A;
    
    // ID de la institución (opcional, se asigna después)
    private String institutionId;
}
