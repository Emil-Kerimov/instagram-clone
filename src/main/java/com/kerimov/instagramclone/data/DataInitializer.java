package com.kerimov.instagramclone.data;

import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.PostImage;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.PostRepository;
import com.kerimov.instagramclone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        if(userRepository.count() > 25) {
            log.info("Data might be already initialized");
            return;
        }
        List<User> users = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            User user = User.builder()
                    .username("testName" + i)
                    .password("testPassword" + i)
                    .email("testEmail" + i+ "@gmail.com")
                    .bio("Some text in testBio #" + i)
                    .avatarKey("fake avatarKey" + i)
                    .build();
            users.add(user);
        }
        userRepository.saveAll(users);

        List<Post> posts = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            Post post = new Post();
            post.setCaption("testCaption " + i);
            post.setUser(users.get(i % users.size()));

            List<PostImage>  images = new ArrayList<>();
            for(int j = 0; j < 10; j++) {
                PostImage postImage = new PostImage();
                postImage.setPost(post);
                postImage.setStorageKey("storageKey" +j + "for post" + j);
                images.add(postImage);
            }
            post.setImages(images);
            posts.add(post);
        }
        postRepository.saveAll(posts);
        log.info("Test data initialization completed successfully");
    }
}
