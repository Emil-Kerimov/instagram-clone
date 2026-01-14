package com.kerimov.instagramclone.service.user;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.exceptions.AlreadyExistsException;
import com.kerimov.instagramclone.exceptions.ResourceNotFoundException;
import com.kerimov.instagramclone.mapper.UserMapper;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.request.CreateUserRequest;
import com.kerimov.instagramclone.request.UpdateUserRequest;
import com.kerimov.instagramclone.service.storage.IMinIOFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final IMinIOFileStorageService minioFileStorageService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.defaults.user-avatar}")
    private String defaultAvatar;

    @Override
    public List<UserDto> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    @Override
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User with id: "+ userId + " not found"));
        return userMapper.toDto(user);
    }

    @Override
    public UserDto createUser(CreateUserRequest request, MultipartFile file) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new AlreadyExistsException("User with email " + request.getEmail() + " already exists");
        } else if(userRepository.existsByUsername(request.getUsername())){
            throw new AlreadyExistsException("User with username " + request.getUsername() + " already exists");
        }

        User createdUser;
        String avatarKey = null;
        try {
            if(file == null){
                avatarKey = defaultAvatar;
            } else {
                avatarKey = minioFileStorageService.upload(file);
            }

            createdUser = userRepository.save(User.builder()
                .username(request.getUsername())
                .bio(request.getBio())
                .email(request.getEmail())
                .avatarKey(avatarKey)
                .password(request.getPassword())
                .build());
        } catch (Exception e) {
            log.info("error while saving user or uploading avatar, trying to clean up");
            deleteAvatarFromStorage(avatarKey);
            throw e;
        }
        return userMapper.toDto(createdUser);
    }

    @Override
    public UserDto updateUser(UUID userId, UpdateUserRequest request, MultipartFile newAvatar) {
        if(!userRepository.existsById(userId)){
            throw new ResourceNotFoundException("User with id: "+ userId + " not found");
        }

        //if user selected new avatar - upload it in storage before transaction
        String preUploadadAvatarKey = null;
        if(newAvatar != null &&  !newAvatar.isEmpty()){
            preUploadadAvatarKey = minioFileStorageService.upload(newAvatar);
        }

        User updatedUser;
        try {
            String finalPreUploadadAvatarKey = preUploadadAvatarKey;
            updatedUser = transactionTemplate.execute((status) -> {
                User user = userRepository.findById(userId).orElseThrow(
                        () -> new ResourceNotFoundException("User with id: "+ userId + " not found"));
                // update user data by request if it was sent
                if(request != null){
                    validateUpdatingUsernameUniqueness(request,user);
                    userMapper.updateUserFromRequest(request,user);
                }

                // if avatar was changed - save it key to DB and delete old image from storage
                if (newAvatar != null && finalPreUploadadAvatarKey != null) {
                    String oldKey = user.getAvatarKey();
                    user.setAvatarKey(finalPreUploadadAvatarKey);
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            deleteAvatarFromStorage(oldKey);
                        }
                    });
                }
                return userRepository.save(user);
            });
        } catch (Exception e) {
            deleteAvatarFromStorage(preUploadadAvatarKey);
            throw e;
        }

        return userMapper.toDto(updatedUser);
    }

    @Transactional
    @Override
    public void deleteUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User with id: "+ userId + " not found"));
        String keyToDelete = user.getAvatarKey();
        userRepository.delete(user);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteAvatarFromStorage(keyToDelete);
            }
        });
    }

    private void deleteAvatarFromStorage(String avatarKey) {
        if(avatarKey != null && !avatarKey.equals(defaultAvatar)){
            minioFileStorageService.delete(avatarKey);
        }
    }

    private void validateUpdatingUsernameUniqueness(UpdateUserRequest request, User user) {
        if(request.getUsername()!=null
                && !request.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyExistsException("User with name " + request.getUsername() + " already exists");
        }
    }
}
