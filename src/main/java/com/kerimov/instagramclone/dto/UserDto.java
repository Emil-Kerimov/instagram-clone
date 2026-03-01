package com.kerimov.instagramclone.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String username;
    private String avatarKey;
}
