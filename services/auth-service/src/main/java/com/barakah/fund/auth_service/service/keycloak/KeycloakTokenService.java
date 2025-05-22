package com.barakah.fund.auth_service.service.keycloak;

import com.barakah.fund.auth_service.config.KeycloakProperties;
import com.barakah.fund.auth_service.exception.AuthenticationException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakTokenService {

    private final AuthzClient authzClient;
    private final KeycloakProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public KeycloakTokenService(AuthzClient authzClient, KeycloakProperties properties) {
        this.authzClient = authzClient;
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }
    public Map<String, String> authenticate(String username, String password) {

        Map<String, String> result = new HashMap<>();

        try {
            org.keycloak.representations.AccessTokenResponse tokenResponse
                    = authzClient.obtainAccessToken(username, password);

            if (tokenResponse != null && tokenResponse.getToken() != null) {

                result.put("access_token", tokenResponse.getToken());
                result.put("refresh_token", tokenResponse.getRefreshToken());
                result.put("expires_in", String.valueOf(tokenResponse.getExpiresIn()));
                result.put("refresh_expires_in", String.valueOf(tokenResponse.getRefreshExpiresIn()));
                result.put("token_type", tokenResponse.getTokenType());
                result.put("status", "success");
            } else {

                result.put("status", "error");
                result.put("message", "No token received from Keycloak");
            }
        } catch (Exception e) {

            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }
    public Map<String, Object> refreshToken(String refreshToken) {

        try {
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", properties.getClientId());
            requestBody.add("client_secret", properties.getClientSecret());
            requestBody.add("refresh_token", refreshToken);
            requestBody.add("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    properties.getTokenUrl(), request, Map.class);

            return response.getBody();
        } catch (HttpClientErrorException e) {

            throw new AuthenticationException("Failed to refresh token: " + e.getMessage());
        }
    }
    public boolean validateToken(String accessToken) {
        try {
            TokenIntrospectionResponse introspectionResponse
                    = authzClient.protection().introspectRequestingPartyToken(accessToken);

            boolean isActive = introspectionResponse.getActive();

            return isActive;
        } catch (Exception e) {

            return false;
        }
    }
    public boolean checkPermission(String accessToken, String resourceId, String scopeName) {

        try {
            AuthorizationRequest request = new AuthorizationRequest();
            request.addPermission(resourceId, scopeName);

            AuthorizationResponse response = authzClient.authorization(accessToken).authorize(request);

            boolean hasPermission = response.getToken() != null;

            return hasPermission;
        } catch (Exception e) {

            return false;
        }
    }
}
