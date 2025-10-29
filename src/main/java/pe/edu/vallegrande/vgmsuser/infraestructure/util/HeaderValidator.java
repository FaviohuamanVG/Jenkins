package pe.edu.vallegrande.vgmsuser.infraestructure.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

public class HeaderValidator {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeaderValidationResult {
        private String userId;
        private List<String> userRoles;
        private String institutionId;
    }

    public static HeaderValidationResult validateHeaders(ServerHttpRequest request) {
        String userIdHeader = request.getHeaders().getFirst("X-User-Id");
        String userRolesHeader = request.getHeaders().getFirst("X-User-Roles");
        String institutionIdHeader = request.getHeaders().getFirst("X-Institution-Id");

        // Validar headers obligatorios
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header X-User-Id is required");
        }

        if (userRolesHeader == null || userRolesHeader.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header X-User-Roles is required");
        }

        // Parsear User ID (mantenemos como String porque puede no ser UUID)
        String userId = userIdHeader.trim();

        // Parsear roles
        List<String> userRoles = Arrays.asList(userRolesHeader.split(","));
        for (int i = 0; i < userRoles.size(); i++) {
            userRoles.set(i, userRoles.get(i).trim().toUpperCase());
        }

        // Parsear Institution ID (puede ser null)
        String institutionId = null;
        if (institutionIdHeader != null && !institutionIdHeader.trim().isEmpty() && !institutionIdHeader.trim().equalsIgnoreCase("null")) {
            institutionId = institutionIdHeader.trim();
        }

        return HeaderValidationResult.builder()
                .userId(userId)
                .userRoles(userRoles)
                .institutionId(institutionId)
                .build();
    }

    public static boolean validateAdminAccess(List<String> userRoles) {
        return userRoles.contains("ADMIN");
    }

    public static boolean validateDirectorAccess(List<String> userRoles) {
        return userRoles.contains("DIRECTOR");
    }

    public static boolean validatePersonalAccess(List<String> userRoles) {
        return userRoles.contains("TEACHER") || 
               userRoles.contains("AUXILIARY") || 
               userRoles.contains("SECRETARY");
    }

    public static HeaderValidationResult validateAdminRole(ServerHttpRequest request) {
        HeaderValidationResult headers = validateHeaders(request);
        
        if (!validateAdminAccess(headers.getUserRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required");
        }
        
        return headers;
    }

    public static HeaderValidationResult validateDirectorRole(ServerHttpRequest request) {
        HeaderValidationResult headers = validateHeaders(request);
        
        if (!validateDirectorAccess(headers.getUserRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Director role required");
        }
        
        // Director debe tener X-Institution-Id
        if (headers.getInstitutionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Director users must have Institution-Id");
        }
        
        return headers;
    }

    public static HeaderValidationResult validatePersonalRole(ServerHttpRequest request) {
        HeaderValidationResult headers = validateHeaders(request);
        
        if (!validatePersonalAccess(headers.getUserRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Personal role required");
        }
        
        // Personal debe tener X-Institution-Id
        if (headers.getInstitutionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Personal users must have Institution-Id");
        }
        
        return headers;
    }
}
