package pe.edu.vallegrande.vgmsuser.infraestructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente para consumir el microservicio de validación de instituciones
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstitutionValidationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.services.institution.base-url:http://michaellibarra-vg-ms-institution:8101}")
    private String institutionServiceBaseUrl;

    /**
     * Valida si una institución existe y está activa
     * 
     * @param institutionId ID de la institución a validar
     * @return Mono<InstitutionValidationResponse> con la información de validación
     */
    public Mono<InstitutionValidationResponse> validateInstitution(String institutionId) {
        log.info("Validating institution with ID: {}", institutionId);
        
        String url = institutionServiceBaseUrl + "/validate-institutions/" + institutionId;
        log.debug("Making request to: {}", url);
        
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(InstitutionValidationResponse.class)
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.info("Institution validation response - ID: {}, Exists: {}, Active: {}, Name: {}", 
                                institutionId, response.getExists(), response.getActive(), response.getName());
                        
                        if (response.getError() != null) {
                            log.warn("Institution validation error for ID {}: {}", institutionId, response.getError());
                        }
                    }
                })
                .doOnError(error -> log.error("Error validating institution {}: {}", institutionId, error.getMessage()))
                .onErrorReturn(InstitutionValidationResponse.builder()
                        .exists(false)
                        .active(false)
                        .error("Service communication error")
                        .build());
    }

    /**
     * Verifica si una institución existe y está activa (método de conveniencia)
     * 
     * @param institutionId ID de la institución a validar
     * @return Mono<Boolean> true si existe y está activa, false en caso contrario
     */
    public Mono<Boolean> isInstitutionActiveAndExists(String institutionId) {
        log.debug("Checking if institution {} exists and is active", institutionId);
        
        return validateInstitution(institutionId)
                .map(response -> {
                    boolean isValid = Boolean.TRUE.equals(response.getExists()) && 
                                     Boolean.TRUE.equals(response.getActive()) && 
                                     response.getError() == null;
                    
                    log.debug("Institution {} validation result: {}", institutionId, isValid);
                    return isValid;
                })
                .defaultIfEmpty(false);
    }

    /**
     * Obtiene el nombre de la institución si existe
     * 
     * @param institutionId ID de la institución
     * @return Mono<String> nombre de la institución o empty si no existe
     */
    public Mono<String> getInstitutionName(String institutionId) {
        log.debug("Getting institution name for ID: {}", institutionId);
        
        return validateInstitution(institutionId)
                .filter(response -> Boolean.TRUE.equals(response.getExists()) && response.getName() != null)
                .map(InstitutionValidationResponse::getName)
                .doOnNext(name -> log.debug("Institution {} name: {}", institutionId, name));
    }
}