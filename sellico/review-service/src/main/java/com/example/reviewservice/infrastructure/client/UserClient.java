package com.example.reviewservice.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/v1/users/batch")
    ApiResponse<List<UserPublicInfo>> getUsersByIds(@RequestParam("ids") List<UUID> ids);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class UserPublicInfo {
        private UUID id;
        private String username;
        private String fullName;
        private String avatarUrl;
    }
}
