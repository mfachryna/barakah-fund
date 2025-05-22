package com.barakah.fund.auth_service.service.abstracts.user;

import org.keycloak.representations.idm.UserRepresentation;
import java.util.List;

public interface UserSearchService {
    List<UserRepresentation> findByUsername(String username);
    List<UserRepresentation> findByEmail(String email);
}