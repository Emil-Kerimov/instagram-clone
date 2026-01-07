package com.kerimov.instagramclone.mapper;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.dto.PostImageDto;
import com.kerimov.instagramclone.models.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PostImageMapper.class, UserMapper.class})
public interface PostMapper {
    List<PostDto> toDtoList(List<Post> posts);

    PostDto toDto(Post post);
}

