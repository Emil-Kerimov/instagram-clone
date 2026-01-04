package com.kerimov.instagramclone.service.user;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.request.CreateUserRequest;

import java.util.List;

public interface IUserService {
    List<UserDto> getAllUsers();

    UserDto createUser(CreateUserRequest request);
}
