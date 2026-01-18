package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.mapper.PostMapper;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.repository.PostRepository;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.service.storage.IMinIOFileStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Tests")
class PostServiceTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IMinIOFileStorageService minioFileStorageService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PostService postService;

    @Nested
    @DisplayName("get post by id tests")
    class GetPostByIdTests {

        @DisplayName("if Post exists then return PostDto")
        @Test
        void getPostShouldReturnPostDtoWhenPostExists(){
            // Arrange
            UUID postId = UUID.randomUUID();
            Post mockPost = new Post();
            mockPost.setId(postId);
            mockPost.setCaption("Caption");

            PostDto mockPostDto = new PostDto();
            mockPostDto.setCaption("Caption");
            mockPostDto.setId(postId);

            when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(postMapper.toDto(mockPost)).thenReturn(mockPostDto);

            // Act
            PostDto result = postService.getPost(postId);

            //Assert
            Assertions.assertNotNull(result);
            Assertions.assertEquals(result.getId(), mockPost.getId());
            Assertions.assertEquals(result.getCaption(), mockPostDto.getCaption());

            verify(postRepository,times(1)).findById(postId);
            verify(postMapper,times(1)).toDto(mockPost);
        }
    }
}