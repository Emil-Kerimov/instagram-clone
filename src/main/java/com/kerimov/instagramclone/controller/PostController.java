package com.kerimov.instagramclone.controller;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.service.post.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/posts")
public class PostController {
    private final IPostService postService;

    @GetMapping
    public List<PostDto> getPosts() {
        return postService.getPosts();
    }

    @PostMapping("/{userId}")
    public PostDto createPost(@PathVariable UUID userId, @RequestParam String content, @RequestParam List<MultipartFile> images) {
        return postService.createPost(userId,content, images);
    }
}
