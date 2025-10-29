package pe.edu.vallegrande.vgmsuser.domain.model.dto;

import lombok.*;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.DocumentType;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.Role;
import pe.edu.vallegrande.vgmsuser.domain.model.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompleteUserResponseDto {
    
    private String id;
    private String keycloakId;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private Set<String> roles;
    private DocumentType documentType;
    private String documentNumber;
    private String phone;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
