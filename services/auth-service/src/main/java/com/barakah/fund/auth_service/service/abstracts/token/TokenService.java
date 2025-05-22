package com.barakah.fund.auth_service.service.abstracts.token;

import java.util.Map;

public interface TokenService {

    /**
     * Authenticate a user with username and password
     * 
     * @param username The username
     * @param password The password
     * @return Map containing authentication result and tokens if successful
     */
    Map<String, String> authenticate(String username, String password);
    
    /**
     * Refresh an access token using a refresh token
     * 
     * @param refreshToken The refresh token
     * @return Map containing the new tokens
     */
    Map<String, Object> refreshToken(String refreshToken);
    
    /**
     * Validate an access token
     * 
     * @param accessToken The access token to validate
     * @return true if the token is valid
     */
    boolean validateToken(String accessToken);
    
    /**
     * Check if a token has a specific permission
     * 
     * @param accessToken The access token
     * @param resourceId The resource ID
     * @param scopeName The scope name
     * @return true if the token has the permission
     */
    boolean checkPermission(String accessToken, String resourceId, String scopeName);
}