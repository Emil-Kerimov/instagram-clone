package com.kerimov.instagramclone.controller;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.request.CreateUserRequest;
import com.kerimov.instagramclone.request.UpdateUserRequest;
import com.kerimov.instagramclone.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

//TODO: delete Wrapper class
@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/users")
public class UserController {
    private final IUserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(){
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID userId){
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> createUser(@RequestPart("request") CreateUserRequest request,
                           @RequestPart(value = "file", required = false) MultipartFile file){
        UserDto createdUser =  userService.createUser(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> patchUpdateUser(@PathVariable UUID userId,
                                                   @RequestPart("request") UpdateUserRequest request,
                                                   @RequestPart(value = "file", required = false) MultipartFile newAvatar){
        UserDto updatedUser = userService.updateUser(userId, request, newAvatar);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUserById(@PathVariable UUID userId){
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }
}
