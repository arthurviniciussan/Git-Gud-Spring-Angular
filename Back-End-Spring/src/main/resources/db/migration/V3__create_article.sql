CREATE TABLE article (
    id               VARCHAR(36)  NOT NULL,
    -- Endereco publico. UNIQUE porque duas URLs iguais apontariam para artigos
    -- diferentes; o service desambigua com sufixo antes de chegar aqui.
    slug             VARCHAR(160) NOT NULL,
    title            VARCHAR(160) NOT NULL,
    -- Vira a meta description: o limite existe para o Google nao truncar.
    summary          VARCHAR(300) NOT NULL,
    -- A fonte da verdade e o markdown; o html e derivado e ja sanitizado.
    content_markdown LONGTEXT     NOT NULL,
    content_html     LONGTEXT     NOT NULL,
    cover_image_url  VARCHAR(500) NULL,
    game             VARCHAR(120) NULL,
    score            DECIMAL(3,1) NULL,
    status           VARCHAR(20)  NOT NULL,
    published_at     DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_article_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- A home e a consulta mais frequente do blog: publicados, do mais recente.
CREATE INDEX idx_article_status_published_at ON article (status, published_at DESC);
