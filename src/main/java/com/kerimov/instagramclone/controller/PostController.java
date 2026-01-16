package com.kerimov.instagramclone.controller;

import com.kerimov.instagramclone.dto.PostDto;
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
    public ResponseEntity<List<PostDto>> getPosts() {
        List<PostDto> posts = postService.getPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable UUID postId) {
        PostDto foundPost = postService.getPost(postId);
        return ResponseEntity.ok(foundPost);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<PostDto> createPost(@PathVariable UUID userId, @RequestParam String content, @RequestParam List<MultipartFile> images) {
        PostDto createdPost = postService.createPost(userId,content, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostDto> patchUpdatePost(@PathVariable UUID postId,
                                                   @RequestParam(required = false) String content,
                                                   @RequestParam(required = false) List<MultipartFile> images,
                                                   @RequestParam(required = false) List<UUID> imagesToDeleteIds) {
        PostDto updatedPost = postService.updatePost(postId,content, images, imagesToDeleteIds);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        postService.deletePostById(postId);
        return ResponseEntity.noContent().build();
    }
}
