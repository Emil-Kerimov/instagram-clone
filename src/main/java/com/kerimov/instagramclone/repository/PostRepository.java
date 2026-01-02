package com.kerimov.instagramclone.repository;

import com.kerimov.instagramclone.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

}
