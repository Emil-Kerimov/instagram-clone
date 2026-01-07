package com.kerimov.instagramclone.controller;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.response.ApiResponse;
import com.kerimov.instagramclone.service.post.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse> getPost(@PathVariable UUID postId) {
        PostDto foundPost = postService.getPost(postId);
        return ResponseEntity.ok(new ApiResponse("success", foundPost));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse> createPost(@PathVariable UUID userId, @RequestParam String content, @RequestParam List<MultipartFile> images) {
        PostDto createdPost = postService.createPost(userId,content, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("created", createdPost));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse> updatePost(@PathVariable UUID postId, @RequestParam String content, @RequestParam List<MultipartFile> images, @RequestParam List<UUID> imagesToDeleteIds) {
        PostDto updatedPost = postService.updatePost(postId,content, images, imagesToDeleteIds);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse("updated", updatedPost));
    }
}
