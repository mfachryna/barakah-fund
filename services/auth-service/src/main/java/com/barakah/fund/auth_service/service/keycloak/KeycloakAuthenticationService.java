package com.barakah.fund.auth_service.service.keycloak;

import com.barakah.fund.auth_service.service.abstracts.auth.AuthenticationService;
import com.barakah.fund.auth_service.service.abstracts.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KeycloakAuthenticationService implements AuthenticationService {
    
    private final TokenService tokenService;

    @Autowired
    public KeycloakAuthenticationService(TokenService tokenService) {
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
}