package com.kerimov.instagramclone.util;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.models.Post;

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
}
