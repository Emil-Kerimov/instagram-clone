package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
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

    @Transactional
    @Override
    public PostDto createPost(UUID userId, String content, List<MultipartFile> images) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("you are not a user!"));
        List<PostImage> postImages = new ArrayList<>();
        Post newPost = new Post();

        for (MultipartFile image : images) {
            PostImage postImage = new PostImage();
            postImage.setUrl(minioFileStorageService.upload(image));
            postImage.setPost(newPost);
            postImages.add(postImage);
        }

        newPost.setCaption(content);
        newPost.setImages(postImages);
        newPost.setUser(user);

        return postMapper.map((postRepository.save(newPost)));
    }
}
