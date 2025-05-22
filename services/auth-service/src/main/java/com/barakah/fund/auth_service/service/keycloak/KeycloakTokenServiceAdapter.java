package com.barakah.fund.auth_service.service.keycloak;

import com.barakah.fund.auth_service.service.abstracts.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KeycloakTokenServiceAdapter implements TokenService {

    private final KeycloakTokenService tokenService;
    
    @Autowired
    public KeycloakTokenServiceAdapter(KeycloakTokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    @Override
    public Map<String, String> authenticate(String username, String password) {
        return tokenService.authenticate(username, password);
    }
    
    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }
    
    @Override
    public boolean validateToken(String accessToken) {
        return tokenService.validateToken(accessToken);
    }
    
    @Override
    public boolean checkPermission(String accessToken, String resourceId, String scopeName) {
        return tokenService.checkPermission(accessToken, resourceId, scopeName);
    }
}