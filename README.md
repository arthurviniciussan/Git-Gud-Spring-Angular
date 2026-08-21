# GitGud

Blog de reviews de jogos. Um autor — eu —, e visitantes que leem sem criar conta.

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4, Maven |
| Banco | MySQL 8, migrations com Flyway |
| Frontend | Angular 21, SCSS |
| Testes | JUnit 5 + Testcontainers · Vitest |

---

## Rodando localmente

Pré-requisitos: **JDK 21**, **Node 22+** e **Docker**.

```bash
cp .env.example .env
```

Preencha as duas variáveis que não têm padrão — sem elas a aplicação se recusa a
subir, de propósito:

```bash
# Hash da sua senha (a senha em si nunca é escrita em disco)
docker run --rm httpd:2.4-alpine htpasswd -nbBC 12 admin 'SUA_SENHA_FORTE' | cut -d: -f2

# Segredo do JWT
openssl rand -base64 48
```

Depois, cada bloco em um terminal:

```bash
docker compose up -d db          # MySQL em localhost:3306

set -a; source .env; set +a      # backend lê as variáveis do ambiente
cd Back-End-Spring && ./mvnw spring-boot:run      # http://localhost:8080

cd Front-End-Angular && npm install && npm start  # http://localhost:4200
```

---

## Testes

```bash
cd Back-End-Spring  && ./mvnw verify   # unitários + integração com MySQL real
cd Front-End-Angular && npm test
```

Os testes de integração **não usam H2**: sobem um MySQL 8 real via Testcontainers,
na mesma versão do compose. As migrations são SQL de MySQL, e o
`ddl-auto: validate` só prova alguma coisa conferindo as entidades contra o banco
de verdade. O Testcontainers gerencia o próprio container — não é preciso ter o
`docker compose up` rodando.

---

## Autenticação

**Não existe cadastro.** O único usuário é criado na subida da aplicação a partir
de `GITGUD_ADMIN_EMAIL` e `GITGUD_ADMIN_PASSWORD_HASH`. Trocar de senha é trocar
a variável de ambiente e reiniciar.

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/api/auth/login` | público → `{name, email, role, token}` |
| `GET` | `/api/auth/session` | token válido → quem está logado |
| — | `/api/admin/**` | `ROLE_ADMIN` (reservado para os artigos) |

O token vai no header `Authorization: Bearer <token>` e vale 2 horas.

Senha errada e email inexistente respondem **exatamente o mesmo** `401` — não dá
para descobrir quais emails existem tentando logar. Após 5 tentativas falhas, o
IP fica bloqueado por 15 minutos.

### Erros

Todos no mesmo formato, em qualquer status:

```json
{ "message": "Credenciais invalidas.", "timestamp": "2026-08-21T00:00:00Z" }
```

---

## Estrutura

O backend é organizado **por módulo** e, dentro de cada módulo, **por camada**.
Um módulo novo (artigos, imagens) repete o mesmo desenho.

```
Back-End-Spring/src/main/java/com/arthur/gitgud/
  common/        infraestrutura compartilhada, sem regra de negócio
    dto/         ErrorResponse — corpo único de erro da API
    exception/   exceções base (400/404/409/429) + tratamento central
  config/        GitgudProperties — configuração validada na subida
  user/
    domain/      User, Role
    repository/
  auth/
    controller/  entrada HTTP
    service/     TokenService
    security/    filtro, CORS, autorização, limite de tentativas
    config/      AdminSeeder — cria o admin a partir do ambiente
    dto/

Back-End-Spring/src/main/resources/db/migration/   migrations Flyway (dono do schema)

Front-End-Angular/src/app/
  components/    reutilizáveis (PrimaryInput, DefaultLoginLayout)
  pages/         uma pasta por tela
  services/      acesso à API
  guards/        proteção de rota
  interceptors/  token e sessão expirada
  types/         contratos da API
```

**Como as exceções se encaixam.** `common/exception` define as bases; cada módulo
estende a adequada e o `RestExceptionHandler` trata só as bases. Quando o módulo
de artigos entrar, o tratamento central não muda.

---

## Convenções

- **O Flyway é dono do schema.** O Hibernate roda com `ddl-auto: validate` e
  nunca altera o banco.
- **Nenhum segredo no repositório.** Tudo por variável de ambiente, sem valor
  padrão para o que protege o site.
- Trabalho em branch, merge só com CI verde.
