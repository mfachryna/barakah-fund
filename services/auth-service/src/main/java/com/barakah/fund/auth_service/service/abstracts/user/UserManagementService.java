package com.barakah.fund.auth_service.service.abstracts.user;

import org.keycloak.representations.idm.UserRepresentation;
import jakarta.ws.rs.core.Response;

public interface UserManagementService {
    Response createUser(UserRepresentation user);
    void setPassword(String userId, String password);
    void assignRole(String userId, String roleName);
    String extractUserIdFromResponse(Response response);
    UserRepresentation prepareUserRepresentation(String username, String email, String phoneNumber);
}