package com.arthur.gitgud.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Categoria editorial: rpg, indie, souls-like. */
@Entity
@Table(name = "tag")
@Getter
@Setter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    /** Como aparece na tela: "Souls-like". */
    @Column(nullable = false, length = 60)
    private String name;

    /** Como aparece na URL: /tag/souls-like. */
    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    public static Tag of(String name) {
        Tag tag = new Tag();
        tag.setName(name.trim());
        tag.setSlug(Slug.of(name).value());
        return tag;
    }
}
