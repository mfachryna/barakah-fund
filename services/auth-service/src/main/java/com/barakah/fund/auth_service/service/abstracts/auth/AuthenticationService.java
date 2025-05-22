package com.barakah.fund.auth_service.service.abstracts.auth;

import java.util.Map;

public interface AuthenticationService {
    Map<String, String> authenticate(String username, String password);
    Map<String, Object> refreshToken(String refreshToken);
    boolean validateToken(String accessToken);
}