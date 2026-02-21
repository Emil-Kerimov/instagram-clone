package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.dto.PostImageDto;
import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.exceptions.FileStorageServiceException;
import com.kerimov.instagramclone.exceptions.ResourceNotFoundException;
import com.kerimov.instagramclone.mapper.PostMapper;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.PostImage;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.PostRepository;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.service.storage.IMinIOFileStorageService;
import com.kerimov.instagramclone.util.TestDataFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
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
    private UUID nonExistingUserId;

    private String bucketName = "images";

    private String url = "http://localhost:9000";

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

        PostImage postImageOneMock = new PostImage(); //Builder
        postImageOneMock.setId(UUID.randomUUID());
        postImageOneMock.setStorageKey("Key1");
        postImageOneMock.setPost(mockPost);
        PostImage postImageSecondMock = new PostImage();
        postImageSecondMock.setId(UUID.randomUUID());
        postImageSecondMock.setStorageKey("Key2");
        postImageSecondMock.setPost(mockPost);



        mockPost.setImages(new ArrayList<>(List.of(postImageOneMock,postImageSecondMock)));
        PostImageDto imageDtoOneMock = new PostImageDto();
        imageDtoOneMock.setId(UUID.randomUUID());
        imageDtoOneMock.setUrl(getFullUrl(postImageOneMock.getStorageKey()));
        PostImageDto imageDtoSecondMock = new PostImageDto();
        imageDtoSecondMock.setId(UUID.randomUUID());
        imageDtoSecondMock.setUrl(getFullUrl(postImageSecondMock.getStorageKey()));
        mockPostDto.setImages(List.of(imageDtoOneMock,imageDtoSecondMock));
    }
    String getFullUrl(String key){
        return UriComponentsBuilder.fromUriString(url)
                .pathSegment(bucketName)
                .pathSegment(key)
                .build().toUriString();
    }

    @Nested
    @DisplayName("get post by id tests")
    class GetPostByIdTests {

        @Test
        @DisplayName("if Post exists then should return its PostDto")
        void getPostShouldReturnPostDtoWhenPostExists(){
            // Arrange
            Post defaultPost = TestDataFactory.createDefaultPost();
            UUID defaultPostId = defaultPost.getId();
            PostDto defaultPostDto = TestDataFactory.createDefaultPostDto(defaultPostId, defaultPost.getCaption());
            given(postRepository.findById(defaultPostId)).willReturn(Optional.of(defaultPost));
            given(postMapper.toDto(defaultPost)).willReturn(defaultPostDto);

            // Act
            PostDto result = postService.getPost(defaultPostId);

            //Assert
            assertNotNull(result);
            assertEquals(result.getId(), defaultPostId);
            assertEquals(result.getCaption(), defaultPost.getCaption());

            verify(postRepository, times(1)).findById(defaultPostId);
            verify(postMapper, times(1)).toDto(defaultPost);
        }

        @Test
        @DisplayName("if Post doesnt exist then should throw ResourceNotFound Exception with correct msg")
        void getPostShouldThrowResourceNotFoundExceptionWhenPostDoesNotExist(){
            // Arrange
            UUID nonExistingId = UUID.randomUUID();
            when(postRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // Act Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getPost(nonExistingId));

            verify(postRepository, times(1)).findById(nonExistingId);
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

        @DisplayName("getAllPosts should return list with existing postDTOs")
        @Test
        void getAllPostsShouldReturnListWithCorrectNumberOfPosts(){
            // Arrange
            Post post = TestDataFactory.createDefaultPost();
            List<Post> mockPosts = List.of(post, post);
            when(postRepository.findAll()).thenReturn(mockPosts);

            PostDto postDto = TestDataFactory.createDefaultPostDto(post.getId(), post.getCaption());
            List<PostDto> mockPostsDto = List.of(postDto,  postDto);
            when(postMapper.toDtoList(mockPosts)).thenReturn(mockPostsDto);

            // Act
            List<PostDto> result = postService.getPosts();

            // Assert
            assertNotNull(result);
            assertEquals(result.size(), mockPosts.size());
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
            User user =  TestDataFactory.createDefaultUser();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(minioFileStorageService.upload(any(MultipartFile.class))).thenReturn("savedKey");
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post saved_post = invocation.getArgument(0);
                saved_post.setUser(user);
                saved_post.setId(user.getId());
                return saved_post;
            });
            when(postMapper.toDto(any(Post.class))).thenAnswer(invocationOnMock -> {
                Post post =  invocationOnMock.getArgument(0);
                PostDto postDto = TestDataFactory.createDefaultPostDto(post.getId(), post.getCaption());
                UserDto userDto = TestDataFactory.createDefaultUserDto(post.getUser().getId());
                postDto.setUser(userDto);
                return postDto;
            });
            String content = "Test content";

            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            PostDto res = postService.createPost(user.getId(), content, files);

            assertNotNull(res);
            assertEquals(user.getId(), res.getUser().getId());
            assertEquals(res.getCaption(), content);

            verify(userRepository, times(1)).findById(user.getId());
            verify(minioFileStorageService, times(2)).upload(any(MultipartFile.class));
            verify(postRepository, times(1)).save(any(Post.class));
            verify(postMapper,times(1)).toDto(any(Post.class));

        }

        @Test
        @DisplayName("If there is no user for who request to create post then should throw ResourceNotFound exception")
        void createPostShouldThrowExceptionIfUserNotFound(){
            UUID nonExistingId = UUID.randomUUID();
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> {postService.createPost(nonExistingId, "mock content", files);
            });
            verify(userRepository, times(1)).findById(nonExistingId);
            verifyNoInteractions(minioFileStorageService);
            verifyNoInteractions(postRepository);
            verifyNoInteractions(postMapper);
        }

        @Test
        @DisplayName("If uploads fails midway because of storage then should throw exception with correct msg and clean up trash(storage rollback)")
        void createPostShouldThrowExceptionAndCleanUpIfCantUploadToStorage(){
            User user =  TestDataFactory.createDefaultUser();
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            String savedKey = "savedKey";
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(minioFileStorageService.upload(any(MultipartFile.class)))
                    .thenReturn(savedKey)
                    .thenThrow(new FileStorageServiceException("loading file to MinIO is not possible"));

            FileStorageServiceException exception = assertThrows(FileStorageServiceException.class,
                    () -> {postService.createPost(user.getId(), "mockContent", files);
                    });
            assertNotNull(exception);

            verify(userRepository, times(1)).findById(user.getId());
            verify(minioFileStorageService, times(2)).upload(any(MultipartFile.class));
            verify(minioFileStorageService, times(1)).delete(savedKey);

            verifyNoInteractions(postRepository);
            verifyNoInteractions(postMapper);
        }

        @Test
        @DisplayName("If saving to DB fails then should throw exception and clean up trash(storage rollback)")
        void createPostShouldThrowExceptionAndCleanUpIfCantSaveToDb(){
            User user = TestDataFactory.createDefaultUser();
            String savedKey = "savedKey";
            String savedKey2 = "savedKey2";
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(minioFileStorageService.upload(any(MultipartFile.class)))
                    .thenReturn(savedKey)
                    .thenReturn(savedKey2);
            when(postRepository.save(any(Post.class))).thenThrow(new RuntimeException());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> {postService.createPost(user.getId(), "mockContent", files);
                    });
            assertNotNull(exception);
            verify(userRepository, times(1)).findById(user.getId());
            verify(minioFileStorageService, times(2)).upload(any(MultipartFile.class));
            verify(minioFileStorageService, times(1)).delete(savedKey);
            verify(minioFileStorageService, times(1)).delete(savedKey2);
            verifyNoInteractions(postMapper);
        }
    }

    @Nested
    @DisplayName("Update Post Tests")
    class UpdatePostTests {
        @Test
        @DisplayName("If all requirements are satisfied then should correctly update post and return dto")
        void updatePostHappyPathTest(){
            when(postRepository.existsById(existingPostId)).thenReturn(true);
            when(minioFileStorageService.upload(any(MultipartFile.class))).thenReturn("savedKey");
            when(postRepository.findById(existingPostId)).thenReturn(Optional.of(mockPost));

            when(postRepository.save(any(Post.class))).thenReturn(mockPost);
            when(postMapper.toDto(any(Post.class))).thenAnswer(invocationOnMock -> {
                Post post =  invocationOnMock.getArgument(0);
                mockPostDto.setCaption(post.getCaption());
                mockPostDto.setId(post.getId());
                List<PostImageDto> images = new ArrayList<>();
                for(PostImage image : post.getImages()){
                    PostImageDto imageDto = new PostImageDto();
                    imageDto.setId(image.getId());
                    imageDto.setUrl(getFullUrl(image.getStorageKey()));
                    images.add(imageDto);
                }
                mockPostDto.setImages(images);
                return mockPostDto;
            });
            when(transactionTemplate.execute(any())).thenAnswer(invocationOnMock -> {
                TransactionSynchronizationManager.initSynchronization();

                try {
                    TransactionCallback callback = invocationOnMock.getArgument(0);
                    return callback.doInTransaction(new SimpleTransactionStatus());
                } finally {
                    TransactionSynchronizationManager.clearSynchronization();
                }
            });

            UUID idToDelete = mockPost.getImages().getFirst().getId();
            PostDto res = postService.updatePost(existingPostId, mockContent, mockCorrectMultipartFiles, List.of(idToDelete));

            assertNotNull(res);
            assertEquals(existingPostId, res.getId());
            assertEquals(res.getCaption(), mockContent);
            assertEquals(3, res.getImages().size());

            verify(postRepository, times(1)).findById(existingPostId);
            verify(postRepository, times(1)).existsById(existingPostId);
            verify(minioFileStorageService, times(2)).upload(mockCorrectMultipartFile);
            verify(postRepository, times(1)).save(any(Post.class));
            verify(postMapper,times(1)).toDto(any(Post.class));
        }


    }
}