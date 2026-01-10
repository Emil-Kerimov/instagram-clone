package com.kerimov.instagramclone.service.user;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.request.CreateUserRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface IUserService {
    List<UserDto> getAllUsers();

    UserDto createUser(CreateUserRequest request, MultipartFile file);

    UserDto getUserById(UUID userId);
}
