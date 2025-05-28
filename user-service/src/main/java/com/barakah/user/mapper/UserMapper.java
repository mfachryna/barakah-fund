package com.barakah.user.mapper;

import com.barakah.user.dto.CreateUserRequest;
import com.barakah.user.dto.UserResponse;
import com.barakah.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    
//    @Mapping(target = "userId", ignore = true)
//    @Mapping(target = "keycloakId", ignore = true)
//    @Mapping(target = "status", ignore = true)
//    @Mapping(target = "emailVerified", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
//    @Mapping(target = "lastLogin", ignore = true)
//    @Mapping(target = "lastLogin", ignore = true)
    UserResponse toResponse(User user);
    void fromResponse(UserResponse response, @MappingTarget User user);

    List<UserResponse> toResponseList(List<User> users);

//    @Mapping(target = "userId", ignore = true)
//    @Mapping(target = "keycloakId", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "password", ignore = true)
    void updateEntity(CreateUserRequest request, @MappingTarget User user);
}
