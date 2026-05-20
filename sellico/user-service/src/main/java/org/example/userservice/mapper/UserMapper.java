package org.example.userservice.mapper;

import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.entity.User;
import org.example.userservice.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") // Để có thể @Autowired vào Service
public interface UserMapper {

    // Map từ 2 Entity -> 1 DTO
    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.authId", target = "authId")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.updatedAt", target = "updatedAt")
    @Mapping(source = "profile.fullName", target = "fullName")
    @Mapping(source = "profile.gender", target = "gender")
    @Mapping(source = "profile.dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "profile.avatarUrl", target = "avatarUrl")
    UserProfileResponse toResponse(User user, UserProfile profile);
}
