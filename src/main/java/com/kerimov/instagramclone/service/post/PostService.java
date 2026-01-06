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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PostService implements IPostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final UserRepository userRepository;
    private final IMinIOFileStorageService minioFileStorageService;

    @Override
    public List<PostDto> getPosts(){
        return postMapper.map((postRepository.findAll()));
    }

    @Override
    public PostDto getPost(UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new ResourceNotFoundException("There is no post with id "+ postId));
        return postMapper.map(post);
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

        return postMapper.map((postRepository.save(newPost)));
    }


    private void addImagesToPost(List<MultipartFile> images, Post newPost) {
        List<PostImage> postImages = new ArrayList<>();
        for (MultipartFile image : images) {
            PostImage postImage = new PostImage();
            postImage.setUrl(minioFileStorageService.upload(image));
            postImage.setPost(newPost);
            postImages.add(postImage);
        }
        newPost.setImages(postImages);
    }
}
