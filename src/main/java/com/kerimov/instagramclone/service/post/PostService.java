package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.exceptions.ResourceNotFoundException;
import com.kerimov.instagramclone.mapper.PostMapper;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.PostImage;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.PostRepository;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.service.storage.IMinIOFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService implements IPostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final UserRepository userRepository;
    private final IMinIOFileStorageService minioFileStorageService;

    @Override
    public List<PostDto> getPosts(){
        return postMapper.toDtoList((postRepository.findAll()));
    }

    @Override
    public PostDto getPost(UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("There is no post with id "+ postId));
        return postMapper.toDto(post);
    }

    @Transactional
    @Override
    public PostDto createPost(UUID userId, String content, List<MultipartFile> images) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("you cant create a post because there is no user with id " + userId));

        Post newPost = new Post();
        newPost.setCaption(content);
        addImagesToPost(images, newPost);
        newPost.setUser(user);

        return postMapper.toDto((postRepository.save(newPost)));
    }

    @Transactional
    @Override
    public PostDto updatePost(UUID postId, String newContent, List<MultipartFile> newImages, List<UUID> imagesToDeleteIds){
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("There is no post with id "+ postId));
        Optional.ofNullable(newContent).ifPresent(content -> post.setCaption(newContent));

        List<String> ids = imagesToDeleteIds.stream().map(UUID::toString).toList();
        if(ids != null && !ids.isEmpty()){
            List<PostImage> imagesToDelete = post.getImages().stream().filter(image -> ids.contains(image.getId().toString())).toList();

            deletePostImagesFromStorage(imagesToDelete);
            post.getImages().removeAll(imagesToDelete);
            log.debug("deleted post images from storage. {}",  imagesToDelete);
            log.debug("post images after image deleting from storage {}",  post.getImages());
        }

        if(newImages != null && !newImages.isEmpty()){
            List<MultipartFile> validImages = newImages.stream()
                    .filter((image) -> !image.isEmpty() && image.getSize() > 0)
                    .toList();
            if (!validImages.isEmpty()) {
                addImagesToPost(validImages, post);
                log.debug("added {}",  validImages);
            }
        }

        return postMapper.toDto(postRepository.save(post));
    }

    @Transactional
    @Override
    public void deletePostById(UUID postId) {   // TODO: clean trash and defence in CUD operation to rollback fileStorage action
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("There is no post with id "+ postId));
        List<PostImage> postImagesToDelete = post.getImages();
        postRepository.delete(post);
        deletePostImagesFromStorage(postImagesToDelete);

    }
    private void deletePostImagesFromStorage(List<PostImage> imagesToDelete) {
        if(imagesToDelete == null){ return;}
        for(PostImage postImage : imagesToDelete){
            minioFileStorageService.delete(postImage.getStorageKey());
        }
    }

    private void addImagesToPost(List<MultipartFile> images, Post post) {
        List<PostImage> postImages = new ArrayList<>();
        for (MultipartFile image : images) {
            PostImage postImage = new PostImage();
            postImage.setStorageKey(minioFileStorageService.upload(image));
            postImage.setPost(post);
            postImages.add(postImage);
        }
        post.getImages().addAll(postImages);
    }
}
