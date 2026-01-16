package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.exceptions.FileStorageServiceException;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final TransactionTemplate transactionTemplate;

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

    @Override
    public PostDto createPost(UUID userId, String content, List<MultipartFile> images) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("you cant create a post because there is no user with id " + userId));

        Post newPost = null;
        List<String> savedKeys = new ArrayList<>();
        try {
            newPost = new Post();
            newPost.setCaption(content);
            saveImagesToStorage(images,savedKeys);
            addImagesToDB(savedKeys,newPost);
            newPost.setUser(user);
            postRepository.save(newPost);
        } catch (Exception e) {
            log.info("error while saving post or uploading images, trying to clean up");
            deletePostImagesFromStorage(savedKeys);
            throw e;
        }
        return postMapper.toDto(newPost);
    }

    @Override
    public PostDto updatePost(UUID postId, String newContent, List<MultipartFile> newImages, List<UUID> imagesToDeleteIds){
        //1 check if post exists
        if(!postRepository.existsById(postId)) throw new ResourceNotFoundException("post with id "+ postId + " is not exists");

        List<String> preSavedKeys = new ArrayList<>();
        try {
            //2 save list of MultipartFile images to minio and accumulate savedKeys
            saveImagesToStorage(newImages, preSavedKeys);
            return transactionTemplate.execute(status -> updatePostInDB(postId,newContent, preSavedKeys,imagesToDeleteIds));
        } catch (Exception e) { // delete pre-saved images if exception from storage after transaction
            log.error("cant update data in db, cleaning pre-saved data", e);
            deletePostImagesFromStorage(preSavedKeys);
            throw e;
        }
    }

    @Transactional
    @Override
    public void deletePostById(UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("There is no post with id "+ postId));
        List<PostImage> postImagesToDelete = post.getImages();
        List<String> keysToDelete = postImagesToDelete.stream().map(PostImage::getStorageKey).toList();
        postRepository.delete(post);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deletePostImagesFromStorage(keysToDelete);
            }

        });
    }

    private void saveImagesToStorage(List<MultipartFile> newImages, List<String> savedKeys) {
        if(newImages != null && !newImages.isEmpty()) {
            List<MultipartFile> validImages = newImages.stream()
                    .filter((image) -> !image.isEmpty() && image.getSize() > 0)
                    .toList();
            if (!validImages.isEmpty()) {
                for (MultipartFile file : validImages) {
                    String key = minioFileStorageService.upload(file);
                    savedKeys.add(key);
                }
                log.debug("uploaded {} from {} images to storage ",  savedKeys.size(), validImages.size());
            }
        }
    }

    private PostDto updatePostInDB(UUID postId, String newContent, List<String> preSavedKeys, List<UUID> imagesToDeleteIds) {
        // -- Start of transactionTemplate block, Start of transaction
        // get post from db
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("There is no post with id "+ postId));
        Optional.ofNullable(newContent).ifPresent(content -> post.setCaption(newContent));

        // delete images from DB and after transaction commit - from storage
        if(imagesToDeleteIds != null && !imagesToDeleteIds.isEmpty()){
            List<PostImage> imagesToDelete = post.getImages().stream().filter(image -> imagesToDeleteIds.contains(image.getId())).toList();
            List<String> keysToDelete = imagesToDelete.stream().map(PostImage::getStorageKey).toList();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deletePostImagesFromStorage(keysToDelete);
                    log.debug("deleted post images from storage with keys {}",  keysToDelete);
                }
            });
            post.getImages().removeAll(imagesToDelete);
        }

        //add images by presaved-keys to DB
        addImagesToDB(preSavedKeys, post);
        return postMapper.toDto(postRepository.save(post));
    }

    private void addImagesToDB(List<String> savedKeys, Post post) {
        if(savedKeys != null && !savedKeys.isEmpty()){
            List<PostImage> postImages = new ArrayList<>();
            for (String key : savedKeys) {
                PostImage postImage = new PostImage();
                postImage.setStorageKey(key);
                postImage.setPost(post);
                postImages.add(postImage);
            }
            post.getImages().addAll(postImages);
        }
    }

    private void deletePostImagesFromStorage(List<String> keysToDelete) {
        if(keysToDelete == null){ return;}
        for(String key : keysToDelete){
            try {
                minioFileStorageService.delete(key);
            } catch (FileStorageServiceException e) {
                log.error("Exception occurs while trying to delete file with key: {}. ", key, e);
            }
        }
    }
}
