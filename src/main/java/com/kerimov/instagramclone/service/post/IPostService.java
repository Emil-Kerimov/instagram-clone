package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IPostService {
    List<PostDto> getPosts();

    PostDto createPost(UUID userId, String content, List<MultipartFile> images);
}
