package com.barakah.fund.auth_service.config;

public class KeycloakProperties {
    private final String authServerUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    
    public KeycloakProperties(String authServerUrl, String realm, String clientId, String clientSecret) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    public String getAuthServerUrl() {
        return authServerUrl;
    }
    
    public String getRealm() {
        return realm;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String getTokenUrl() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }
}