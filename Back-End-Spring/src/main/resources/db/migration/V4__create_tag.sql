CREATE TABLE tag (
    id   VARCHAR(36) NOT NULL,
    name VARCHAR(60) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE article_tag (
    article_id VARCHAR(36) NOT NULL,
    tag_id     VARCHAR(36) NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    -- ON DELETE CASCADE: apagar um artigo nao pode deixar vinculo orfao para
    -- trás. A tag em si sobrevive, porque pertence a outros artigos.
    CONSTRAINT fk_article_tag_article FOREIGN KEY (article_id) REFERENCES article (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_tag_tag     FOREIGN KEY (tag_id)     REFERENCES tag (id)     ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
