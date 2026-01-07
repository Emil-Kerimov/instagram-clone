package com.kerimov.instagramclone.mapper;

import com.kerimov.instagramclone.dto.PostImageDto;
import com.kerimov.instagramclone.models.Post;
import com.kerimov.instagramclone.models.PostImage;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class PostImageMapper {
    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.external.url}")
    private String url;

    public PostImageDto toDto(PostImage image){
        if(image == null){
            return null;
        }

        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setId(image.getId());
        String fullUrl = UriComponentsBuilder.fromUriString(url)
                .pathSegment(bucketName)
                .pathSegment(image.getStorageKey())
                .build().toUriString();
        postImageDto.setUrl(fullUrl);
        return postImageDto;
    }
    public abstract List<PostImageDto> toDtoList(List<PostImage> images);
}
