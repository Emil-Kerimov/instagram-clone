package com.kerimov.instagramclone.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PostDto {
    private UUID id;
    private String caption;
    private LocalDateTime timestamp;
    private List<PostImageDto> images = new ArrayList<>();
    private UserDto user;
}
