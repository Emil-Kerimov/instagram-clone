package com.kerimov.instagramclone.mapper;

import com.kerimov.instagramclone.dto.PostDto;
import com.kerimov.instagramclone.models.Post;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {
    List<PostDto> map(List<Post> posts);
    PostDto map(Post posts);
}

