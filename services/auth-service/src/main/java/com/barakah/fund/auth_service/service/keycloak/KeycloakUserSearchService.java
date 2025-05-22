package com.barakah.fund.auth_service.service.keycloak;

import com.barakah.fund.auth_service.config.KeycloakConfig;
import com.barakah.fund.auth_service.service.abstracts.user.UserSearchService;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakUserSearchService implements UserSearchService {
    
    private final KeycloakConfig keycloakConfig;
    
    @Value("${keycloak.realm}")
    private String realm;

    @Autowired
    public KeycloakUserSearchService(KeycloakConfig keycloakConfig) {
        this.keycloakConfig = keycloakConfig;
    }

    @Override
    public List<UserRepresentation> findByUsername(String username) {
        return keycloakConfig.keycloak().realm(realm).users().searchByUsername(username, true);
    }

    @Override
    public List<UserRepresentation> findByEmail(String email) {
        return keycloakConfig.keycloak().realm(realm).users().searchByEmail(email, true);
    }
}