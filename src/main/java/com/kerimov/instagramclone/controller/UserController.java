package com.kerimov.instagramclone.controller;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.request.CreateUserRequest;
import com.kerimov.instagramclone.response.ApiResponse;
import com.kerimov.instagramclone.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/users")
public class UserController {
    private final IUserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllUsers(){
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse("Successful", users));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable UUID userId){
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(new ApiResponse("Found successful", user));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto createUser(@RequestPart("request") CreateUserRequest request,
                           @RequestPart(value = "file", required = false) MultipartFile file){
        return userService.createUser(request, file);
    }
}
