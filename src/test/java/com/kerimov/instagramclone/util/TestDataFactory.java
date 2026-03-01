package com.kerimov.instagramclone.util;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.dto.PostImageDto;
import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.PostImage;
import com.kerimov.instagramclone.models.User;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestDataFactory {
    public static Post createDefaultPost(){
        return Post
                .builder()
                    .id(UUID.randomUUID())
                    .caption("Default Caption")
                    .user(createDefaultUser())
                .build();
    }

    public static Post createPostWithImages(int count){
        Post post = createDefaultPost();
        List<PostImage> postImages = new ArrayList<>();
        for(int i=0;i<count;i++){
            postImages.add(PostImage
                    .builder()
                            .id(UUID.randomUUID())
                            .storageKey("Key"+i)
                            .post(post)
                    .build());
        }
        post.setImages(postImages);
        return post;
    }

    public static PostDto createDefaultPostDto(Post post){
        return PostDto.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .user(createDefaultUserDto(post.getUser()))
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
    public static UserDto createDefaultUserDto(User user){
        return UserDto.builder()
                .id(user.getId())
                .build();
    }
    public static MockMultipartFile createMockMultipartFile(){
        return new MockMultipartFile("file", "dummy_image.png", MediaType.IMAGE_PNG_VALUE, "test".getBytes());
    }
    public static List<MultipartFile> createMockMultipartFiles(){
        return List.of(createMockMultipartFile(), createMockMultipartFile());
    }

    public static PostDto mapPostToDto(Post post){
        PostDto postDto = createDefaultPostDto(post);
        List<PostImageDto> images = new ArrayList<>();
        for(PostImage image : post.getImages()){
            PostImageDto imageDto = new PostImageDto();
            imageDto.setId(image.getId());
            imageDto.setUrl(image.getStorageKey());
            images.add(imageDto);
        }
        postDto.setImages(images);
        return postDto;
    }
}
