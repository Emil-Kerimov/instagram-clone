package com.kerimov.instagramclone.service.user;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.mapper.UserMapper;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.request.CreateUserRequest;
import com.kerimov.instagramclone.service.storage.IMinIOFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
        return userMapper.map(userRepository.findAll());
    }

    @Transactional
    @Override
    public UserDto createUser(CreateUserRequest request, MultipartFile file) {
        if(userRepository.existsByEmail(request.getEmail())){ throw new RuntimeException("msg");}

        String imageUrl;
        if(file.isEmpty()){
            imageUrl = minioFileStorageService.getFileUrl(defaultAvatar);
        } else {
            imageUrl = minioFileStorageService.upload(file);
        }

            User createdUser =  userRepository.save(User.builder()
                .username(request.getUsername())
                .bio(request.getBio())
                .email(request.getEmail())
                .imageUrl(imageUrl)
                .password(request.getPassword())
                .build());
        return userMapper.map(createdUser);
    }
}
