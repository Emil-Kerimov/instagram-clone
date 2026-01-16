package com.kerimov.instagramclone.repository;

import com.kerimov.instagramclone.models.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Override
    @EntityGraph(attributePaths = {"user", "images"})
    List<Post> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "images"})
    Optional<Post> findById(UUID id);
}
