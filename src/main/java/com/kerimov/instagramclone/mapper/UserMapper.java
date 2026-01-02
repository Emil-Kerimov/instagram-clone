package com.kerimov.instagramclone.mapper;

import com.kerimov.instagramclone.dto.UserDto;
import com.kerimov.instagramclone.models.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    List<UserDto> map(List<User> users);
}
