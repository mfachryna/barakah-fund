package com.barakah.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTokenService {
    
    private final RestTemplate restTemplate;
    
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    // Service credentials
    @Value("${app.service.client-id:account-service}")
    private String serviceClientId;
    
    @Value("${app.service.client-secret:your-service-secret}")
    private String serviceClientSecret;
    
    public String getServiceToken() {
        try {
            String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", serviceClientId);
            body.add("client_secret", serviceClientSecret);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    tokenUrl, request, TokenResponse.class);
            
            if (response.getBody() != null) {
                return response.getBody().getAccessToken();
            }
            
        } catch (Exception e) {
            log.error("Failed to get service token", e);
        }
        
        return null;
    }
    
    private static class TokenResponse {
        private String access_token;
        // ... other fields
        
        public String getAccessToken() { return access_token; }
        public void setAccess_token(String access_token) { this.access_token = access_token; }
    }
}