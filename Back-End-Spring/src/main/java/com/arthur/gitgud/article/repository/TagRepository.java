package com.arthur.gitgud.article.repository;

import com.arthur.gitgud.article.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {

    Optional<Tag> findBySlug(String slug);

    List<Tag> findAllByOrderByNameAsc();
}
