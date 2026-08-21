-- Estado que o antigo `ddl-auto: update` produzia. Existe para que bancos
-- criados antes do Flyway continuem validos: com baseline-on-migrate ligado,
-- esses bancos entram como baseline e esta migration e apenas registrada.
CREATE TABLE IF NOT EXISTS users (
    id       VARCHAR(36) NOT NULL,
    name     VARCHAR(255),
    email    VARCHAR(255),
    password VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
