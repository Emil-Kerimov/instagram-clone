package com.kerimov.instagramclone.service.user;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.mapper.UserMapper;
import com.kerimov.instagramclone.models.User;
import com.kerimov.instagramclone.repository.UserRepository;
import com.kerimov.instagramclone.request.CreateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getAllUsers() {
        return userMapper.map(userRepository.findAll());
    }

    @Transactional
    @Override
    public UserDto createUser(CreateUserRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){ throw new RuntimeException("msg");}

        User createdUser = userRepository.save(User.builder()
                .username(request.getUsername())
                .bio(request.getBio())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .password(request.getPassword())
                .build());
        return userMapper.map(createdUser);
    }
}
