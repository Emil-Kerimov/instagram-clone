package com.kerimov.instagramclone.service;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.mapper.UserMapper;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.request.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserDto> getAllUsers() {
        return userMapper.map(userRepository.findAll());
    }

    public User createUser(CreateUserRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){ throw new RuntimeException("msg");}

        return userRepository.save(User.builder()
                .username(request.getUsername())
                .bio(request.getBio())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .password(request.getPassword())
                .build());
    }
}
