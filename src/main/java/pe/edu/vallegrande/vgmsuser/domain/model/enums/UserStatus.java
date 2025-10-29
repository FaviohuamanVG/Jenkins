package pe.edu.vallegrande.vgmsuser.domain.model.enums;

public enum UserStatus {
    A("A"),             // Active
    I("I"),             // Inactive
    PENDING("PENDING"),
    SUSPENDED("SUSPENDED");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
