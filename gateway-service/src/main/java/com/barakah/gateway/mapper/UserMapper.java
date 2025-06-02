package com.barakah.gateway.mapper;

import com.barakah.common.proto.v1.PageRequest;
import com.barakah.gateway.dto.user.*;
import com.barakah.user.proto.v1.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class UserMapper {

    public UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth().isEmpty() ? null :
                        LocalDate.parse(user.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE))
                .address(user.getAddress())
                .status(user.getStatus().toString())
                .createdAt(user.getCreatedAt().isEmpty() ? null :
                        LocalDateTime.parse(user.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .updatedAt(user.getUpdatedAt().isEmpty() ? null :
                        LocalDateTime.parse(user.getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))

                .build();
    }

    public CreateUserRequest toGrpcCreateRequest(CreateUserRequestDto dto) {
        CreateUserRequest.Builder builder = CreateUserRequest.newBuilder()
                .setUsername(dto.getUsername())
                .setEmail(dto.getEmail())
                .setFirstName(dto.getFirstName())
                .setLastName(dto.getLastName())
                .setPassword(dto.getPassword());

        if (dto.getPhoneNumber() != null) {
            builder.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getDateOfBirth() != null) {
            builder.setDateOfBirth(dto.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (dto.getAddress() != null) {
            builder.setAddress(dto.getAddress());
        }

        return builder.build();
    }

    public UpdateUserRequest toGrpcUpdateRequest(String userId, UpdateUserRequestDto dto) {
        UpdateUserRequest.Builder builder = UpdateUserRequest.newBuilder();

        if (dto.getEmail() != null) {
            builder.setEmail(dto.getEmail());
        }
        if (dto.getFirstName() != null) {
            builder.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            builder.setLastName(dto.getLastName());
        }
        if (dto.getPhoneNumber() != null) {
            builder.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getDateOfBirth() != null) {
            builder.setDateOfBirth(dto.getDateOfBirth().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (dto.getAddress() != null) {
            builder.setAddress(dto.getAddress());
        }

        return builder.build();
    }

    public PageRequest toPageRequest(Pageable pageable) {
        PageRequest.Builder builder = PageRequest.newBuilder()
                .setPage(pageable.getPageNumber())
                .setSize(pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                builder.setSort(order.getProperty());
                builder.setDirection(order.getDirection().name());
            });
        }

        return builder.build();
    }
}