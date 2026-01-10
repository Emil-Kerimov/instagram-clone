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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final IMinIOFileStorageService minioFileStorageService;

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

    @Transactional
    @Override
    public UserDto createUser(CreateUserRequest request, MultipartFile file) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new AlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        String avatarKey;
        if(file == null){
            avatarKey = defaultAvatar;
        } else {
            avatarKey = minioFileStorageService.upload(file);
        }

            User createdUser =  userRepository.save(User.builder()
                .username(request.getUsername())
                .bio(request.getBio())
                .email(request.getEmail())
                .avatarKey(avatarKey)
                .password(request.getPassword())
                .build());
        return userMapper.toDto(createdUser);
    }

    @Transactional
    @Override
    public UserDto updateUser(UUID userId, UpdateUserRequest request, MultipartFile newAvatar) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User with id: "+ userId + " not found"));

        if(newAvatar != null &&  !newAvatar.isEmpty()){
            String oldKey = user.getAvatarKey();
            user.setAvatarKey(minioFileStorageService.upload(newAvatar));
            if(!oldKey.equals(defaultAvatar)){
                minioFileStorageService.delete(oldKey);
            }
        }

        if(request != null){
            userMapper.updateUserFromRequest(request,user);
        }
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public void deleteUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User with id: "+ userId + " not found"));
        String keyToDelete = user.getAvatarKey();
        userRepository.delete(user);
        minioFileStorageService.delete(keyToDelete);
    }
}
