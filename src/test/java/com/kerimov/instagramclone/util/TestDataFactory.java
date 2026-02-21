package com.kerimov.instagramclone.util;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.User;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public class TestDataFactory {
    public static Post createDefaultPost(){
        return Post
                .builder()
                    .id(UUID.randomUUID())
                    .caption("Default Caption")
                .build();
    }

    public static PostDto createDefaultPostDto(UUID postId, String caption){
        return PostDto.builder()
                .id(postId)
                .caption(caption)
                .build();
    }

    public static User createDefaultUser(){
        return User.builder()
                .id(UUID.randomUUID())
                .email("default@email.com")
                .password("password")
                .avatarKey("avatarKey")
                .build();
    }
    public static UserDto createDefaultUserDto(UUID userId){
        return UserDto.builder()
                .id(userId)
                .build();
    }
    public static MockMultipartFile createMockMultipartFile(){
        return new MockMultipartFile("file", "dummy_image.png", MediaType.IMAGE_PNG_VALUE, "test".getBytes());
    }
    public static List<MultipartFile> createMockMultipartFiles(){
        return List.of(createMockMultipartFile(), createMockMultipartFile());
    }
}
