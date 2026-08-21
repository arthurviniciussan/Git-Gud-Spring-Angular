-- O blog passa a ter um unico autor: o admin, semeado por variavel de ambiente.
-- O cadastro publico (POST /auth/register) foi removido nesta mesma mudanca.

-- Contas criadas pelo cadastro publico nao conseguem mais logar e, se tiverem
-- email nulo ou repetido, impediriam a constraint UNIQUE mais abaixo.
-- Removemos apenas as linhas inutilizaveis; contas validas sao preservadas.
DELETE u FROM users u
    INNER JOIN users mais_antigo
    ON u.email = mais_antigo.email
   AND u.id > mais_antigo.id;

DELETE FROM users WHERE email IS NULL OR email = '' OR password IS NULL;

UPDATE users SET name = 'admin' WHERE name IS NULL OR name = '';

-- Papel do usuario. Guardado como texto (@Enumerated(EnumType.STRING)): com
-- ordinal, reordenar o enum um dia remapearia silenciosamente quem e admin.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN';

ALTER TABLE users MODIFY name     VARCHAR(120) NOT NULL;
ALTER TABLE users MODIFY email    VARCHAR(180) NOT NULL;
-- Um hash BCrypt tem 60 caracteres; 72 deixa folga sem virar campo de texto.
ALTER TABLE users MODIFY password VARCHAR(72) NOT NULL;

-- Sem esta constraint, dois registros com o mesmo email fazem findByEmail
-- estourar NonUniqueResultException.
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
