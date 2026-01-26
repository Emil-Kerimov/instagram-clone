package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.exceptions.FileStorageServiceException;
import com.kerimov.instagramclone.exceptions.ResourceNotFoundException;
import com.kerimov.instagramclone.mapper.PostMapper;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.PostRepository;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.service.storage.IMinIOFileStorageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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

    private UUID existingPostId;
    private UUID existingUserId;
    private UUID nonExistingPostId;
    private UUID nonExistingUserId;

    private String mockContent = "Hello world! this is a new post content!";

    private Post mockPost;
    private PostDto mockPostDto;

    private User mockUser;
    private UserDto mockUserDto;

    private MultipartFile mockCorrectMultipartFile;
    private List<MultipartFile> mockCorrectMultipartFiles;


    @BeforeEach
    void setUp() {
        existingPostId = UUID.randomUUID();
        nonExistingPostId = UUID.randomUUID();

        mockPost = new Post();
        mockPost.setId(existingPostId);
        mockPost.setCaption("Caption");

        mockPostDto = new PostDto();
        mockPostDto.setCaption("Caption");
        mockPostDto.setId(existingPostId);


        mockCorrectMultipartFile = new MockMultipartFile(
                "file",
                "dummy_image.png",
                MediaType.IMAGE_PNG_VALUE,
                mockContent.getBytes()
        );
        mockCorrectMultipartFiles = List.of(mockCorrectMultipartFile,mockCorrectMultipartFile);
        mockUser = new User();
        existingUserId =  UUID.randomUUID();
        nonExistingUserId = UUID.randomUUID();
        mockUser.setId(existingUserId);
        mockUserDto = new UserDto();
        mockUserDto.setId(existingUserId);
    }

    @Nested
    @DisplayName("get post by id tests")
    class GetPostByIdTests {

        @Test
        @DisplayName("if Post exists then should return PostDto")
        void getPostShouldReturnPostDtoWhenPostExists(){
            // Arrange
            when(postRepository.findById(existingPostId)).thenReturn(Optional.of(mockPost));
            when(postMapper.toDto(mockPost)).thenReturn(mockPostDto);

            // Act
            PostDto result = postService.getPost(existingPostId);

            //Assert
            assertNotNull(result);
            assertEquals(result.getId(), existingPostId);
            assertEquals(result.getCaption(), mockPost.getCaption());

            verify(postRepository, times(1)).findById(existingPostId);
            verify(postMapper, times(1)).toDto(mockPost);
        }

        @Test
        @DisplayName("if Post doesnt exist then should throw ResourceNotFound Exception with correct msg")
        void getPostShouldThrowResourceNotFoundExceptionWhenPostDoesNotExist(){
            // Arrange
            when(postRepository.findById(nonExistingPostId)).thenReturn(Optional.empty());

            // Act Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> postService.getPost(nonExistingPostId));
            assertEquals("There is no post with id " + nonExistingPostId, exception.getMessage());

            verify(postRepository, times(1)).findById(nonExistingPostId);
            verifyNoInteractions(postMapper);

        }
    }

    @Nested
    @DisplayName("get all posts test")
    class GetAllPostsTests {

        @DisplayName("If there is no posts then should return empty list")
        @Test
        void getAllPostsShouldReturnEmptyListIfThereIsNoPost(){
            when(postRepository.findAll()).thenReturn(new ArrayList<>());

            assertTrue(postService.getPosts().isEmpty());
            verify(postRepository, times(1)).findAll();
            verify(postMapper,times(1)).toDtoList(new ArrayList<>());
        }

        @DisplayName("If there is one post then should return list with 1 postDto")
        @Test
        void getAllPostsShouldReturnListWithOneDtoIfThereIsOnePost(){
            // Arrange
            List<Post> mockPosts = List.of(mockPost);
            when(postRepository.findAll()).thenReturn(mockPosts);
            List<PostDto> mockPostsDto = List.of(mockPostDto);
            when(postMapper.toDtoList(mockPosts)).thenReturn(mockPostsDto);

            // Act
            List<PostDto> result = postService.getPosts();

            // Assert
            assertEquals(result.size(), mockPosts.size());
            assertNotNull(result);
            assertEquals(result.getFirst().getId(), mockPosts.getFirst().getId());
            assertEquals(1, result.size());
            assertEquals(result.getFirst().getCaption(), mockPosts.getFirst().getCaption());
            verify(postRepository, times(1)).findAll();
            verify(postMapper,times(1)).toDtoList(mockPosts);
        }

        @DisplayName("If there is more than one post then should return list with these postDtos")
        @Test
        void getAllPostsShouldReturnListWithCorrectNumberOfPosts(){
            // Arrange
            List<Post> mockPosts = List.of(mockPost, mockPost);
            when(postRepository.findAll()).thenReturn(mockPosts);
            List<PostDto> mockPostsDto = List.of(mockPostDto,  mockPostDto);
            when(postMapper.toDtoList(mockPosts)).thenReturn(mockPostsDto);

            // Act
            List<PostDto> result = postService.getPosts();

            // Assert
            assertEquals(result.size(), mockPosts.size());
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(postRepository, times(1)).findAll();
            verify(postMapper,times(1)).toDtoList(mockPosts);
        }
    }

    @Nested
    @DisplayName("Create Post Tests")
    class CreatePostTests{
        @Test
        @DisplayName("If all requirements are satisfied then should correctly create post and return dto")
        void createPostHappyPath(){
            when(userRepository.findById(existingUserId)).thenReturn(Optional.of(mockUser));
            when(minioFileStorageService.upload(any(MultipartFile.class))).thenReturn("savedKey");
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post saved_post = invocation.getArgument(0);
                saved_post.setUser(mockUser);
                saved_post.setId(existingPostId);
                return saved_post;
            });
            when(postMapper.toDto(any(Post.class))).thenAnswer(invocationOnMock -> {
                Post post =  invocationOnMock.getArgument(0);
                mockPostDto.setCaption(post.getCaption());
                mockPostDto.setId(post.getId());
                mockUserDto.setId(post.getUser().getId());
                mockPostDto.setUser(mockUserDto);
                return mockPostDto;
            });

            PostDto res = postService.createPost(existingUserId, mockContent, mockCorrectMultipartFiles);

            assertNotNull(res);
            assertEquals(existingUserId, res.getUser().getId());
            assertEquals(res.getCaption(), mockContent);

            verify(userRepository, times(1)).findById(existingUserId);
            verify(minioFileStorageService, times(2)).upload(mockCorrectMultipartFile);
            verify(postRepository, times(1)).save(any(Post.class));
            verify(postMapper,times(1)).toDto(any(Post.class));

        }

        @Test
        @DisplayName("If there is no user for who request to create post then should throw exception with correct msg")
        void createPostShouldThrowExceptionIfUserNotFound(){
            when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> {postService.createPost(nonExistingUserId, mockContent, mockCorrectMultipartFiles);
            });
            assertEquals("you cant create a post because there is no user with id " + nonExistingUserId, exception.getMessage());
            verify(userRepository, times(1)).findById(nonExistingUserId);
            verifyNoInteractions(minioFileStorageService);
            verifyNoInteractions(postRepository);
            verifyNoInteractions(postMapper);
        }

        @Test
        @DisplayName("If uploads fails midway then should throw exception with correct msg and clean up trash(storage rollback)")
        void createPostShouldThrowExceptionAndCleanUpIfCantUploadToStorage(){
            when(userRepository.findById(existingUserId)).thenReturn(Optional.of(mockUser));
            when(minioFileStorageService.upload(any(MultipartFile.class)))
                    .thenReturn("savedKey-1")
                    .thenThrow(new FileStorageServiceException("loading file to MinIO is not possible"));

            FileStorageServiceException exception = assertThrows(FileStorageServiceException.class,
                    () -> {postService.createPost(existingUserId, mockContent, mockCorrectMultipartFiles);
                    });
            assertNotNull(exception);
            assertEquals("loading file to MinIO is not possible", exception.getMessage());

            verify(userRepository, times(1)).findById(existingUserId);
            verify(minioFileStorageService, times(2)).upload(any(MultipartFile.class));
            verify(minioFileStorageService, times(1)).delete("savedKey-1");

            verifyNoInteractions(postRepository);
            verifyNoInteractions(postMapper);
        }

        @Test
        @DisplayName("If saving to DB fails then should throw exception and clean up trash(storage rollback)")
        void createPostShouldThrowExceptionAndCleanUpIfCantSaveToDb(){
            when(userRepository.findById(existingUserId)).thenReturn(Optional.of(mockUser));
            when(minioFileStorageService.upload(any(MultipartFile.class)))
                    .thenReturn("savedKey-1")
                    .thenReturn("savedKey-2");
            when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> {postService.createPost(existingUserId, mockContent, mockCorrectMultipartFiles);
                    });
            assertNotNull(exception);
            verify(userRepository, times(1)).findById(existingUserId);
            verify(minioFileStorageService, times(2)).upload(any(MultipartFile.class));
            verify(minioFileStorageService, times(1)).delete("savedKey-1");
            verify(minioFileStorageService, times(1)).delete("savedKey-2");
            verifyNoInteractions(postMapper);
        }

    }
}