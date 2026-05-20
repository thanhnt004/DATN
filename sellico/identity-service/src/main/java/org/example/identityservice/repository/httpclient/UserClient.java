package org.example.identityservice.repository.httpclient;

import org.example.identityservice.dto.request.UserProfileCreationRequest;
import org.example.identityservice.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {
    @PostMapping(value = "/internal/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<UserProfileResponse> createProfile(@RequestBody UserProfileCreationRequest request);

    @GetMapping(value = "/internal/v1/user-name/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    String getUserName(@PathVariable("identifier") String identifier);

    @GetMapping(value = "/internal/v1/users/by-auth/{authId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<UserProfileResponse> getUserByAuthId(@PathVariable("authId") UUID authId);
}
