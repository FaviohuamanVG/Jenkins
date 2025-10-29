package pe.edu.vallegrande.vgmsuser.domain.model.enums;

public enum PasswordStatus {
    TEMPORARY,     // Contraseña temporal (DNI)
    PERMANENT,     // Contraseña permanente (cambiada por el usuario)
    EXPIRED        // Contraseña expirada
}
