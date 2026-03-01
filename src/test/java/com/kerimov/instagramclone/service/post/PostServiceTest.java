package com.kerimov.instagramclone.service.post;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.exceptions.FileStorageServiceException;
import com.kerimov.instagramclone.exceptions.ResourceNotFoundException;
import com.kerimov.instagramclone.mapper.PostMapper;
import com.kerimov.instagramclone.models.Post;
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
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Nested
    @DisplayName("get post by id tests")
    class GetPostByIdTests {

        @Test
        @DisplayName("if Post exists then should return its PostDto")
        void getPostShouldReturnPostDtoWhenPostExists(){
            // Arrange
            Post defaultPost = TestDataFactory.createDefaultPost();
            UUID defaultPostId = defaultPost.getId();
            PostDto defaultPostDto = TestDataFactory.createDefaultPostDto(defaultPost);
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
            given(postRepository.findById(nonExistingId)).willReturn(Optional.empty());

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
            given(postRepository.findAll()).willReturn(new ArrayList<>());

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
            PostDto postDto = TestDataFactory.createDefaultPostDto(post);
            List<PostDto> mockPostsDto = List.of(postDto,  postDto);
            given(postMapper.toDtoList(mockPosts)).willReturn(mockPostsDto);
            given(postRepository.findAll()).willReturn(mockPosts);

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
            //Given
            User user =  TestDataFactory.createDefaultUser();
            String content = "Test content";
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(minioFileStorageService.upload(any(MultipartFile.class))).willReturn("savedKey");
            given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
                Post saved_post = invocation.getArgument(0);
                saved_post.setUser(user);
                saved_post.setId(user.getId());
                return saved_post;
            });
            given(postMapper.toDto(any(Post.class))).willAnswer(invocationOnMock -> {
                Post post =  invocationOnMock.getArgument(0);
                return TestDataFactory.mapPostToDto(post);
            });

            //When
            PostDto res = postService.createPost(user.getId(), content, files);

            //Then
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
            given(userRepository.findById(nonExistingId)).willReturn(Optional.empty());

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
            //given
            User user =  TestDataFactory.createDefaultUser();
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            String savedKey = "savedKey";
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(minioFileStorageService.upload(any(MultipartFile.class)))
                    .willReturn(savedKey)
                    .willThrow(new FileStorageServiceException("loading file to MinIO is not possible"));

            //when, then
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
            // Given
            User user = TestDataFactory.createDefaultUser();
            String savedKey = "savedKey";
            String savedKey2 = "savedKey2";
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(minioFileStorageService.upload(any(MultipartFile.class)))
                    .willReturn(savedKey)
                    .willReturn(savedKey2);
            given(postRepository.save(any(Post.class))).willThrow(new RuntimeException());

            // When, Then
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
        @DisplayName("If we delete one image and add 2 new to post with 2 images, then should return postDto with 3 images")
        void updatePostHappyPathTest(){
            //Given
            Post post = TestDataFactory.createPostWithImages(2);
            UUID idToDelete = post.getImages().getFirst().getId();
            UUID postId = post.getId();
            String content = "mock content";
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            given(postRepository.existsById(postId)).willReturn(true);
            given(minioFileStorageService.upload(any(MultipartFile.class))).willReturn("savedKey");
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(postRepository.save(any(Post.class))).willReturn(post);
            given(postMapper.toDto(any(Post.class))).willAnswer(invocationOnMock -> {
                Post receivedPost =  invocationOnMock.getArgument(0);
                return TestDataFactory.mapPostToDto(receivedPost);
            });
            given(transactionTemplate.execute(any())).willAnswer(invocationOnMock -> {
                TransactionSynchronizationManager.initSynchronization();

                try {
                    TransactionCallback callback = invocationOnMock.getArgument(0);
                    return callback.doInTransaction(new SimpleTransactionStatus());
                } finally {
                    TransactionSynchronizationManager.clearSynchronization();
                }
            });

            //When
            PostDto res = postService.updatePost(postId, content, files, List.of(idToDelete));

            //Then
            assertNotNull(res);
            assertEquals(postId, res.getId());
            assertEquals(res.getCaption(), content);
            assertEquals(3, res.getImages().size());

            verify(postRepository, times(1)).findById(postId);
            verify(postRepository, times(1)).existsById(postId);
            verify(minioFileStorageService, times(2)).upload(any(MultipartFile.class));
            verify(postRepository, times(1)).save(any(Post.class));
            verify(postMapper,times(1)).toDto(any(Post.class));
        }
        @Test
        @DisplayName("when trying to update NonExisting Post Should Throw ResourceNotFoundException")
        void updateNonExistingPostShouldThrowResourceNotFoundException(){
            UUID nonExistingId = UUID.randomUUID();

            ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, () ->postService.updatePost(nonExistingId,"new content",List.of(),List.of()));

            assertNotNull(e);

        }

        @Test
        @DisplayName("when exceptions occurs while uploading one of the images to storage, should throw exception and clean up")
        void exceptionDuringSavingToStorageShouldThrowExceptionAndCleanUp(){
            //Given
            Post post = TestDataFactory.createPostWithImages(2);
            UUID idToDelete = post.getImages().getFirst().getId();
            UUID postId = post.getId();
            String content = "mock content";
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();
            given(postRepository.existsById(postId)).willReturn(true);
            given(minioFileStorageService.upload(any(MultipartFile.class)))
                    .willReturn("savedKey")
                    .willThrow(FileStorageServiceException.class);

            assertThrows(FileStorageServiceException.class, () -> postService.updatePost(postId, content, files, List.of(idToDelete)));
            verify(postRepository, times(1)).existsById(postId);
            verify(minioFileStorageService, times(1)).delete("savedKey");

        }

        @Test
        @DisplayName("when exceptions occurs while saving post to DB, should throw exception and clean up")
        void exceptionDuringSavingToDbShouldThrowExceptionAndCleanUp(){
            //Given
            Post post = TestDataFactory.createPostWithImages(2);
            UUID idToDelete = post.getImages().getFirst().getId();
            UUID postId = post.getId();
            String content = "mock content";
            List<MultipartFile> files = TestDataFactory.createMockMultipartFiles();

            given(postRepository.existsById(postId)).willReturn(true);
            given(minioFileStorageService.upload(any(MultipartFile.class)))
                    .willReturn("savedKey")
                    .willReturn("savedKey2");
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(postRepository.save(any(Post.class))).willThrow(RuntimeException.class);
            given(transactionTemplate.execute(any())).willAnswer(invocationOnMock -> {
                TransactionSynchronizationManager.initSynchronization();
                try {
                    TransactionCallback callback = invocationOnMock.getArgument(0);
                    return callback.doInTransaction(new SimpleTransactionStatus());
                } finally {
                    TransactionSynchronizationManager.clearSynchronization();
                }
            });

            assertThrows(RuntimeException.class, () -> postService.updatePost(postId, content, files, List.of(idToDelete)));

            verify(postRepository, times(1)).existsById(postId);
            verify(minioFileStorageService, times(2)).delete(any(String.class));

        }
    }

    @Nested
    @DisplayName("Delete post Tests")
    class DeletePostTests{
        @Test
        @DisplayName("when trying to delete post with non-existing ID should throw RessourceNotFoundException")
        void attemptToDeleteNonExistingPostShouldThrowResourceNotFoundException(){
            UUID postId = UUID.randomUUID();
            given(postRepository.findById(postId)).willThrow(ResourceNotFoundException.class);

            assertThrows(ResourceNotFoundException.class, () -> postService.deletePostById(postId));

            verify(postRepository, times(1)).findById(postId);
        }

        @Test
        @DisplayName("when trying to delete post with 2 images should successfully delete exactly 2 images")
        void deletePostHappyPath(){
            Post post = TestDataFactory.createPostWithImages(2);
            UUID postId = post.getId();
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            TransactionSynchronizationManager.initSynchronization();
                try {
                    postService.deletePostById(postId);
                    TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
                } finally {
                    TransactionSynchronizationManager.clearSynchronization();
                }

            verify(postRepository, times(1)).findById(postId);
            verify(minioFileStorageService, times(2)).delete(any(String.class));
        }
    }
}