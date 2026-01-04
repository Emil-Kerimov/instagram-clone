package com.kerimov.instagramclone.controller;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.request.CreateUserRequest;
import com.kerimov.instagramclone.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/users")
public class UserController {
    private final IUserService userService;

    @GetMapping
    public List<UserDto> getAllUsers(){
        return userService.getAllUsers();
    }

    @PostMapping
    public UserDto createUser(@RequestBody CreateUserRequest request){
        return userService.createUser(request);
    }
}
