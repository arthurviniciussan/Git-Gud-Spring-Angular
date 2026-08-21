package com.arthur.gitgud.article.dto;

import com.arthur.gitgud.article.domain.Tag;

public record TagResponse(String name, String slug) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getName(), tag.getSlug());
    }
}
