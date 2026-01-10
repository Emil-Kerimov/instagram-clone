package com.kerimov.instagramclone.service.user;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.request.CreateUserRequest;
import com.kerimov.instagramclone.request.UpdateUserRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    List<UserDto> getAllUsers();

    UserDto createUser(CreateUserRequest request, MultipartFile file);

    UserDto getUserById(UUID userId);

    UserDto updateUser(UUID userId, UpdateUserRequest request, MultipartFile newAvatar);

    void deleteUserById(UUID userId);
}
