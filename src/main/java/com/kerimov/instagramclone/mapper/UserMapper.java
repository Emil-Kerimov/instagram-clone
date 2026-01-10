package com.kerimov.instagramclone.mapper;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.request.UpdateUserRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class UserMapper {
    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.external.url}")
    private String url;

    public abstract List<UserDto> toDtoList(List<User> users);
    public UserDto toDto(User user){

        if(user == null){
            return null;
        }

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        String fullUrl = UriComponentsBuilder.fromUriString(url)
                .pathSegment(bucketName)
                .pathSegment(user.getAvatarKey())
                .build().toUriString();
        userDto.setAvatarKey(fullUrl);
        return userDto;
    }

    public abstract void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
