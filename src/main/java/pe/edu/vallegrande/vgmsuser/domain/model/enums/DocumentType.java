package pe.edu.vallegrande.vgmsuser.domain.model.enums;

public enum DocumentType {
    DNI("DNI"),
    PASSPORT("PASSPORT"),
    CARNET_EXTRANJERIA("CARNET_EXTRANJERIA"),
    RUC("RUC");

    private final String value;

    DocumentType(String value) {
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
