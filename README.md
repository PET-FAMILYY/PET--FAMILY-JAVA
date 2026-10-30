# Pet Family API

> Backend RESTful para acompanhamento contínuo da saúde dos pets
> **Challenge CLYVO VET 2026 — FIAP | Java Advanced — Entrega 3**

🎥 **Vídeo de apresentação:** [https://youtu.be/YUA4fIXr-tg](https://youtu.be/YUA4fIXr-tg)

---
  
🌐 **API online (deploy):** [https://petfamily-api.onrender.com](https://petfamily-api.onrender.com)                         
❤️ **Health check:** [https://petfamily-api.onrender.com/actuator/health](https://petfamily-api.onrender.com/actuator/health)  
📖 **Swagger:** [https://petfamily-api.onrender.com/swagger-ui.html](https://petfamily-api.onrender.com/swagger-ui.html)

> ⏱️ A API roda no plano gratuito do Render e "dorme" após ~15 min sem uso. A primeira requisição depois disso pode levar até 1–2 minutos (cold start).

📱 **Repositório do app mobile:** [https://github.com/PET-FAMILYY/PET-FAMILY-MOBILE](https://github.com/PET-FAMILYY/PET-FAMILY-MOBILE)

---

## Sumário

1. [Problema](#problema)
2. [Solução](#solução)
3. [Testando: API rodando no banco Oracle](#testando-api-rodando-no-banco-oracle)
4. [Tecnologias](#tecnologias)
5. [Entidades e Relacionamentos](#entidades-e-relacionamentos)
6. [Segurança (Spring Security + JWT)](#segurança-spring-security--jwt)
7. [Flyway — versionamento de banco](#flyway--versionamento-de-banco)
8. [Fluxos de negócio (além do CRUD)](#fluxos-de-negócio-além-do-crud)
9. [Endpoints](#endpoints)
10. [Como Rodar](#como-rodar)
11. [Deploy online (Render)](#deploy-online-render)
12. [Contas de demonstração (perfil `dev`)](#contas-de-demonstração-perfil-dev)
13. [Exemplos de Payload](#exemplos-de-payload)
14. [Correspondência requisitos × implementação](#correspondência-requisitos--implementação)
15. [Changelog técnico — o que mudamos no backend Java (entrega 03)](#changelog-técnico--o-que-mudamos-no-backend-java-entrega-03)
16. [Integrantes](#integrantes)
17. [Estrutura do Projeto](#estrutura-do-projeto)

---

## Problema

Tutores de pets enfrentam dificuldades para manter a continuidade no cuidado veterinário: esquecem vacinas, perdem histórico de consultas, não possuem lembretes preventivos e não têm acesso fácil a orientações de saúde animal. O resultado é uma ruptura no cuidado que compromete o bem-estar dos animais.

## Solução

A Pet Family API centraliza e gerencia todas as informações relacionadas à saúde e ao bem-estar dos pets, com autenticação real e dois perfis de acesso:

- Autenticação por token (Bearer/JWT), com dois perfis: **TUTOR** e **VETERINARIO**
- Cadastro de tutores e múltiplos pets por tutor, com dados clínicos completos
- **Agendamento, cancelamento e atendimento de consultas** — fluxo completo com controle de disponibilidade
- **Cuidados preventivos com recorrência automática** — definidos pelo veterinário, confirmados pelo tutor
- Assistente de IA simulado para orientações de saúde animal (histórico persistido)
- Dashboard clínico com KPIs em tempo real (acesso do veterinário)
- Versionamento de banco de dados com **Flyway**
- **Linha do tempo do pet** — histórico de saúde unificado (consultas + cuidados preventivos + conversas com a IA) em uma única lista cronológica
- **Aviso diário de cuidados** — rotina agendada que identifica cuidados pendentes que vencem em até 3 dias
- **Proteção contra força bruta no login** — após 5 falhas seguidas o acesso é bloqueado temporariamente (HTTP 429)
- **PostgreSQL em produção**, Docker, CI no GitHub Actions e deploy online no Render


---

##  Testando: API rodando no banco Oracle

Esta seção explica como subir a API conectada ao **Oracle** (perfil `oracle`) e testar as **procedures e funções da package `PKG_PETFAMILY_S4`** chamadas pela aplicação. O Render e os testes automatizados continuam usando PostgreSQL/H2; o perfil `oracle` só é ativado quando você pede.

> No perfil `oracle` o **Flyway fica desligado**: o schema Oracle (tabelas, package e triggers) é mantido pelos scripts da pasta `sql_oracle` (`00_instalar.sql` e `V2__ajustes_compatibilidade_java.sql`). O Java não cria nem altera tabelas nesse perfil.

### 1. Pré-requisitos

- Schema Oracle instalado no usuário que a API vai usar (`00_instalar.sql` e depois o `V2__ajustes_compatibilidade_java.sql`).
- Um usuário **veterinário** no banco. Duas opções: subir com o perfil `demo` em um banco com a tabela `tutores` vazia (cria `veterinario@petfamily.com`), ou rodar o `08_usuario_demo_opcional.sql` (cria `vet.demo@petfamily.local`). Senha de ambos: `senha123`.
- Acesso de rede ao Oracle (no caso da FIAP, estar na rede da faculdade ou na VPN).
- Docker **ou** Java 17+ e Maven 3.8+.

### 2. Arquivo `.env.oracle` (exemplo)

Crie na raiz do projeto. **Nunca faça commit desse arquivo** (adicione `.env.*` e `!.env.example` ao `.gitignore`).

```env
SPRING_PROFILES_ACTIVE=oracle,demo
DB_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl
DB_USER=rmXXXXXX
DB_PASSWORD=sua-senha-oracle
JWT_SECRET=uma-chave-aleatoria-com-no-minimo-32-caracteres
CORS_ALLOWED_ORIGINS=*
```

| Variável | Para que serve |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `oracle` conecta no Oracle. `demo` carrega dados de demonstração (só se `tutores` estiver vazia). |
| `DB_URL` | URL JDBC. Com **SID**: `jdbc:oracle:thin:@HOST:1521:SID`. Com **service name**: `jdbc:oracle:thin:@//HOST:1521/SERVICE_NAME`. |
| `DB_USER` / `DB_PASSWORD` | Usuário e senha do Oracle. Se a senha tiver `$`, escreva `$$` ao usar Docker Compose. |
| `JWT_SECRET` | Segredo do token (mínimo 32 caracteres). |
| `EXPORT_DIR` *(opcional)* | Pasta onde o Java grava os JSON exportados (padrão `json`, relativa à pasta onde a API roda). |

### 3. Subir a API

**Opção A: Docker Compose.** Crie a pasta `json` (`mkdir json`) e o arquivo `docker-compose.oracle.yml` na raiz:

```yaml
services:
  api:
    build: .
    ports: ["9090:9090"]
    environment:
      SPRING_PROFILES_ACTIVE: oracle,demo
      DB_URL: ${DB_URL:?defina DB_URL no .env.oracle}
      DB_USER: ${DB_USER:?defina DB_USER no .env.oracle}
      DB_PASSWORD: ${DB_PASSWORD:?defina DB_PASSWORD no .env.oracle}
      JWT_SECRET: ${JWT_SECRET:?defina JWT_SECRET no .env.oracle}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-*}
      SWAGGER_ENABLED: "true"
      EXPORT_DIR: /app/json
    volumes:
      - ./json:/app/json
```

```bash
docker compose -f docker-compose.oracle.yml --env-file .env.oracle up --build --remove-orphans
```

> Variáveis definidas no terminal têm prioridade sobre o `--env-file`. Se aparecer `jdbc:postgresql` ou usuário errado, rode `unset DB_URL DB_USER DB_PASSWORD` antes.

**Opção B: Maven** (carregando o `.env.oracle` no terminal).

Git Bash / Linux / macOS:
```bash
set -a; source .env.oracle; set +a
mvn spring-boot:run
```

PowerShell:
```powershell
Get-Content .env.oracle | ForEach-Object {
  if ($_ -match '^\s*([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process') }
}
mvn spring-boot:run
```

Sinais de que deu certo no log: `HikariPool-1 - Added connection oracle.jdbc.driver.T4CConnection` e `Started PetFamilyApplication`. A API fica em **http://localhost:9090** e o Swagger em **http://localhost:9090/swagger-ui.html**.

### 4. Requisições de teste

Os endpoints `/oracle/**` existem **somente no perfil `oracle`** e exigem o perfil **VETERINARIO**. Os exemplos abaixo usam `curl` (Git Bash). No Swagger, faça login, clique em **Authorize** e cole o token.

**4.1. Login do veterinário e token**

```bash
curl -s -X POST http://localhost:9090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"veterinario@petfamily.com","senha":"senha123"}'
```

Use `vet.demo@petfamily.local` se o veterinário veio do `08_usuario_demo_opcional.sql`. Copie o campo `token` da resposta:

```bash
TOKEN=cole_o_token_aqui
```

**4.2. `pr_cadastrar_tutor`** → `201` com `{"id": N}`

```bash
curl -s -X POST http://localhost:9090/oracle/tutores \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Tutor Teste","email":"tutor.teste@petfamily.local","telefone":"11999990000"}'
```

**4.3. `pr_cadastrar_pet`** → `201` com `{"id": N}` (use o `id` do tutor)

```bash
curl -s -X POST http://localhost:9090/oracle/pets \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tutorId":TUTOR_ID,"nome":"Rex","especie":"Cachorro"}'
```

**4.4. `pr_agendar_consulta`** → `201` com `{"id": N}` (use o `id` do pet; o `horario` é **texto** `"HH:mm"`)

```bash
curl -s -X POST http://localhost:9090/oracle/consultas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"petId":PET_ID,"data":"2026-12-31","horario":"11:13","tipo":"ROTINA"}'
```

**4.5. Conflito de horário:** repita exatamente a requisição 4.4 → `409` ("Horario ja esta reservado.")

**4.6. `pr_atualizar_status`** → `204` (valores: `AGENDADA`, `REALIZADA`, `CANCELADA`)

```bash
curl -s -X PATCH http://localhost:9090/oracle/consultas/CONSULTA_ID/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"REALIZADA"}'
```

**4.7. Funções `fn_contar_pets` e `fn_status_valido`**

```bash
curl -s http://localhost:9090/oracle/tutores/TUTOR_ID/quantidade-pets -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:9090/oracle/status-valido?status=REALIZADA" -H "Authorization: Bearer $TOKEN"
```

**4.8. `pr_exportar_json`**: gera `json/tutores.json`, `json/consultas.json` e `json/interacoes_ia.json`

```bash
curl -s -X POST http://localhost:9090/oracle/exportar -H "Authorization: Bearer $TOKEN"
```

Para só visualizar, sem gravar arquivo: `GET /oracle/exportar/TUTORES` (também `CONSULTAS` e `INTERACOES_IA`).

### 5. Resultados esperados

| Teste | Resposta esperada |
|---|---|
| Cadastro de tutor, pet ou consulta | `201` + `{"id": N}` |
| Agendar o mesmo dia e horário duas vezes | `409 Conflito` |
| E-mail de tutor repetido | `409 Conflito` |
| `tutorId` ou `petId` inexistente | `404 Not Found` |
| Status inválido (ex.: `XYZ`) | `400 Operação Inválida` |
| `horario` fora do formato `"HH:mm"` | `400 Bad Request` |
| Usar token de TUTOR nos endpoints `/oracle/**` | `403 Acesso negado` |
| Sem token | `401 Não autenticado` |

Confira no Oracle o efeito das procedures:

```sql
SELECT id, data, TO_CHAR(horario,'YYYY-MM-DD HH24:MI:SS') horario, status
  FROM consultas ORDER BY id DESC FETCH FIRST 3 ROWS ONLY;
SELECT * FROM auditoria_consultas_s4 ORDER BY id DESC;   -- INSERT/UPDATE registrados pelo trigger
```

### 6. Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `Driver oracle.jdbc.OracleDriver claims to not accept jdbcUrl, jdbc:postgresql...` | `DB_URL` com a URL do PostgreSQL (variável antiga no terminal ou `.env.oracle` errado). |
| `ORA-01017: invalid username/password` | Usuário ou senha errados (ou `$` na senha sem escapar no Docker Compose). Teste a mesma senha no SQL Developer. |
| `ORA-12505` / `ORA-12514` | SID e service name trocados na `DB_URL`. |
| Timeout na conexão | Sem rede/VPN até o Oracle. |
| `MalformedInputException` no build | Arquivo `.properties` salvo fora de UTF-8. |
| Login do veterinário retorna 401 | `DataInitializer` não rodou (a tabela `tutores` já tinha dados). Use o `08_usuario_demo_opcional.sql`. |
| `404` em `/oracle/...` | Perfil `oracle` não está ativo. |
| `ORA-20xxx` no log | Erro de regra da package, convertido em 404 (`20004`), 409 (`20006`, `20009`) ou 400 (demais). |

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Web / Spring Data JPA | — |
| **Spring Security + JWT (jjwt)** | 0.11.5 |
| **Flyway** | (gerenciado pelo Spring Boot 3.2.5) |
| Spring Cache + Caffeine | — |
| Bean Validation | — |
| Lombok | — |
| SpringDoc OpenAPI / Swagger | 2.3.0 |
| Maven | — |
| PostgreSQL (produção) | 16 |
| H2 (dev e testes) | — |
| Docker / Docker Compose | — |
| Testcontainers (teste de migrations no PostgreSQL) | — |
| GitHub Actions (CI) | — |
| Render (deploy) | — |
| Spring Actuator (health check) | — |

---

## Entidades e Relacionamentos

```
Usuario (TUTOR|VETERINARIO) ──(0..1)── Tutor (1) ────── (N) Pet
                                                              │
                                                              ├── (N) Consulta ── (0..1) ConsultaSlot
                                                              ├── (N) Lembrete (cuidado preventivo)
                                                              └── (N) InteracaoIA
```

| Entidade | Campos Principais |
|---|---|
| Usuario | id, nome, email, senhaHash, role, tutorId (nullable) |
| Tutor | id, nome, email, telefone |
| Pet | id, nome, especie, raca, idade, peso, observacoesSaude, tutorId |
| Consulta | id, data, horario, tipoConsulta, status, observacoes, petId, realizadaEm, realizadaPor (auditoria do atendimento) |
| ConsultaSlot | id, data, horario (único), consultaId — modelo de disponibilidade |
| Lembrete | id, titulo, descricao, dataLembrete, tipo, status, recorrenciaDias, dataConclusao, criadoPor, concluidoPor, origemLembrete, petId |
| InteracaoIA | id, pergunta, resposta, dataHora, categoria, petId |

Um `Usuario` com role `TUTOR` está sempre vinculado 1:1 a um `Tutor` (criado junto no cadastro). Um `Usuario` com role `VETERINARIO` não tem `Tutor` associado — ele acessa dados clínicos de todos os pets.

---

## Segurança (Spring Security + JWT)

- Login com e-mail/senha (BCrypt), token **Bearer** com expiração (24h por padrão), assinatura HMAC-SHA256.
- **Sessão stateless** — sem cookie, sem `JSESSIONID`. CSRF fica desabilitado porque a proteção contra CSRF existe para o cenário "navegador envia cookie de sessão automaticamente"; como a API não usa cookie/sessão implícita, essa superfície de ataque não existe aqui.
- Cadastro público (`POST /auth/registrar`) sempre cria um usuário **TUTOR**. Não existe endpoint público para criar veterinário — a conta de demonstração é carregada pelo `DataInitializer` (perfil `dev`).
- **A identidade do tutor nunca vem do cliente.** Endpoints como `POST /pets` ignoram qualquer `tutorId` que venha no corpo; o tutor é sempre resolvido a partir do token (`SecurityUtils`/`UsuarioPrincipal`). Todo acesso a pet/consulta/cuidado é validado no service comparando o dono real com o tutor autenticado — inclusive quando o dado vem do cache (`@Cacheable`), a checagem de posse roda a cada chamada.
- Autorização por perfil com `@PreAuthorize` nos controllers + checagem de posse nos services (duas camadas).
- CORS liberado por variável de ambiente (`CORS_ALLOWED_ORIGINS`, padrão `*` em dev) — necessário porque o Expo roda em endereços variáveis (emulador, dispositivo físico, Expo Go).
- **Limite de tentativas de login:** após 5 falhas seguidas para o mesmo e-mail, novos logins retornam `429 Too Many Requests` por 15 minutos (`LoginAttemptService`).
- **Usuário atual centralizado:** os services recebem o usuário autenticado por `UsuarioAtual`, sem repetir consulta ao banco em cada operação.
- **H2 Console fora do ambiente online:** o perfil `prod` usa PostgreSQL e não expõe console de banco. O Swagger é controlado por `SWAGGER_ENABLED`.

### Variáveis de ambiente (sem segredos reais commitados)

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `JWT_SECRET` | *(fallback de dev embutido)* | Segredo HMAC do token. **Troque em qualquer ambiente real.** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Validade do token. |
| `CORS_ALLOWED_ORIGINS` | `*` | Lista separada por vírgula em produção. |
| `SPRING_PROFILES_ACTIVE` | `dev` | Ver seção Flyway/dados de demonstração. Valores: `dev` (H2 + dados demo), `prod` (PostgreSQL), `prod,demo` (PostgreSQL + dados demo), `test`. |
| `PORT` | `9090` | Porta HTTP. O Render injeta esse valor automaticamente. |
| `DB_URL` | — | URL JDBC completa (opcional; tem prioridade sobre `DB_HOST`). |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | — / `5432` / — | Dados do PostgreSQL (perfil `prod`). |
| `DB_USER` / `DB_PASSWORD` | — | Credenciais do PostgreSQL (perfil `prod`). |
| `SWAGGER_ENABLED` | `false` em `prod` | Liga/desliga Swagger UI e `/api-docs` em produção. |

> Em `prod`, `JWT_SECRET` é **obrigatório** (no Render é gerado automaticamente).

Exemplo (PowerShell, para rodar com segredo customizado):
```powershell
$env:JWT_SECRET = "uma-chave-bem-grande-e-aleatoria-so-sua"
mvn spring-boot:run
```

---

## Flyway — versionamento de banco

O schema é 100% gerenciado pelo Flyway (`src/main/resources/db/migration`). O Hibernate roda com `ddl-auto=validate`: ele **nunca** cria ou altera tabelas em runtime, só confere se as entidades batem com o schema migrado.

| Migration | Conteúdo |
|---|---|
| `V1__schema_inicial.sql` | Tabelas base: tutores, pets, consultas, lembretes, interacoes_ia (equivalente ao schema das entregas 1–2). |
| `V2__autenticacao_e_fluxos.sql` | Tabela `usuarios` (autenticação), tabela `consulta_slots` (modelo de disponibilidade) e colunas novas em `lembretes` para o fluxo de cuidado preventivo (recorrência, conclusão, responsáveis). |
| `V3__fk_origem_lembrete_set_null.sql` | FK de origem do lembrete recorrente passa a `ON DELETE SET NULL`. |
| `V4__indices_de_desempenho.sql` | Índices para as consultas mais frequentes. |
| `V5__regras_de_integridade.sql` | Constraints de integridade no banco. |
| `V6__auditoria_de_consultas.sql` | Colunas `realizada_em` e `realizada_por_usuario_id` para auditar quem atendeu a consulta. |
| `V7__fk_auditoria_set_null.sql` | FKs de auditoria (`criado_por`, `concluido_por`, `realizada_por`) com `ON DELETE SET NULL`, permitindo excluir uma conta sem quebrar o histórico. |

> O V1 existe em duas versões, uma por banco (`db/migration/h2` e `db/migration/postgresql`), escolhidas pelo placeholder `{vendor}`. V2–V7 ficam em `db/migration/common`. Para a próxima migration, use `V8__...` (a menção a `V3` abaixo é do texto original da entrega 3).

**Como criar uma nova migration:** adicione um arquivo `V3__descricao_curta.sql` em `src/main/resources/db/migration` (numeração sempre crescente) e rode a aplicação — o Flyway aplica automaticamente na subida. **Nunca edite uma migration já aplicada**; toda mudança de schema vira uma migration nova.

### Banco de dados

- **Desenvolvimento local:** H2 em **arquivo** (`./data/petfamily.mv.db`, ignorado pelo git) — os dados persistem entre reinicializações. `AUTO_SERVER=TRUE` permite abrir o H2 Console com a aplicação já rodando.
- **Testes automatizados:** H2 em **memória**, isolado (`src/test/resources/application-test.properties`, perfil `test`) — cada execução de teste começa de um banco limpo, migrado do zero.
- Não há conexão com banco externo/nuvem — isso é documentado aqui de propósito, para não passar a impressão de que existe algo além do H2 local. *(Válido para dev/testes; veja o item abaixo para produção.)*
- **Produção (Render) e Docker Compose:** **PostgreSQL** (perfil `prod`), com o schema criado pelo Flyway na subida (V1–V7). O `h2-console` não existe em produção.
- **Teste de migrations em PostgreSQL real:** `PostgresMigrationTest` (Testcontainers) executa as 7 migrations; é ignorado automaticamente se não houver Docker e roda no CI.

### Dados de demonstração

O `DataInitializer` roda **apenas no perfil `dev`** (`spring.profiles.active` já é `dev` por padrão em `application.properties`) e é idempotente: só popula dados se o banco estiver vazio. Para subir sem dados fictícios (ex.: simular "produção"), rode com outro perfil:
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

> Atualização: o `DataInitializer` também roda no perfil **`demo`**. No deploy online usamos `SPRING_PROFILES_ACTIVE=prod,demo` (PostgreSQL + dados de demonstração) para que a avaliação tenha dados prontos.

---

## Fluxos de negócio (além do CRUD)

### 1. Agendamento e atendimento de consultas

1. Tutor autenticado escolhe um **pet próprio**, tipo de consulta, data e horário (`POST /consultas/agendar`).
2. Backend valida: pet pertence ao tutor, data/horário no futuro, horário livre.
3. Disponibilidade é garantida por um **modelo de slot** (`consulta_slots`, `UNIQUE(data, horario)`) — a checagem de conflito é atômica no banco, então duas requisições concorrentes para o mesmo horário nunca conseguem reservar as duas.
4. Consulta nasce `AGENDADA`.
5. Veterinário registra o atendimento (`POST /consultas/{id}/realizar`) → `REALIZADA`. Transição feita com `UPDATE ... WHERE status = 'AGENDADA'` (compare-and-swap no banco), então uma segunda tentativa de "realizar" a mesma consulta é rejeitada com 409, mesmo sob concorrência.
6. Cancelamento (`POST /consultas/{id}/cancelar`) só é permitido enquanto `AGENDADA`; libera o slot para reagendamento.

Não existe `POST`/`PUT`/`DELETE` genérico em `/consultas` — só as três operações de negócio + leitura. Isso evita que alguém contorne as regras de transição.

### 2. Cuidado preventivo (evolução de "Lembrete")

1. Veterinário define um cuidado para um pet (`POST /lembretes`), com prazo e recorrência opcional em dias.
2. Tutor visualiza a agenda de cuidados dos próprios pets (`GET /lembretes/meus`).
3. Tutor confirma a execução (`POST /lembretes/{id}/concluir`).
4. Backend registra data de conclusão e responsável, de forma atômica (`UPDATE ... WHERE status = 'PENDENTE'`) — concluir duas vezes (inclusive em paralelo) é bloqueado com 409.
5. Se havia `recorrenciaDias`, a próxima ocorrência é criada **na mesma transação** da conclusão.
6. Cuidados `CANCELADO` (ação do veterinário) nunca podem ser concluídos. Um cuidado é considerado atrasado quando `status=PENDENTE` e a data já passou (`atrasado` no `LembreteResponse`).

### 3. Linha do tempo do pet (histórico de saúde unificado)

Antes, o histórico de um pet estava espalhado em três telas e três endpoints diferentes. Agora um único endpoint reúne tudo:

`GET /pets/{id}/linha-do-tempo`

1. O backend valida o acesso: **o tutor só vê a linha do tempo dos próprios pets**; o veterinário vê qualquer pet.
2. Reúne três fontes em uma única lista:
   - **`CONSULTA`** — consultas do pet (agendadas, realizadas ou canceladas), com data e horário.
   - **`CUIDADO`** — cuidados preventivos (vacinas, vermífugo etc.) com seu status (`PENDENTE`, `CONCLUIDO`, `CANCELADO`).
   - **`ASSISTENTE_IA`** — perguntas feitas ao assistente de IA e as respostas (a pergunta é resumida no título para a leitura ficar compacta).
3. Ordena tudo do evento **mais recente para o mais antigo**.
4. Retorna uma lista de `EventoSaudeResponse` (`tipo`, `quando`, `titulo`, `detalhe`, `status`), pronta para o app mobile renderizar como timeline sem precisar combinar dados.

Exemplo de resposta:
```json
[
  {
    "tipo": "CONSULTA",
    "quando": "2026-11-05T10:00:00",
    "titulo": "Vacinação",
    "detalhe": "Vacina V10 anual",
    "status": "AGENDADA"
  },
  {
    "tipo": "CUIDADO",
    "quando": "2026-10-01T00:00:00",
    "titulo": "Vermifugação",
    "detalhe": "Vermifugação trimestral",
    "status": "PENDENTE"
  },
  {
    "tipo": "ASSISTENTE_IA",
    "quando": "2026-09-20T14:32:10",
    "titulo": "O que fazer quando meu cachorro não quer comer?",
    "detalhe": "Resposta do assistente...",
    "status": null
  }
]
```

Implementação: `LinhaDoTempoService` (somente leitura, `@Transactional(readOnly = true)`) e `PetController#linhaDoTempo`.

### 4. Aviso diário de cuidados próximos

`LembreteNotificador` roda todo dia às **08:00** (`@Scheduled`, configurável por `petfamily.lembretes.cron`) e registra um aviso para cada cuidado `PENDENTE` que vence nos próximos **3 dias**, indicando pet, cuidado, data e e-mail do tutor. O canal de envio é simulado por log; trocar por e-mail ou push exige alterar apenas esse componente.

---

## Endpoints

### Autenticação
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/auth/registrar` | público | Cadastro — sempre cria TUTOR |
| POST | `/auth/login` | público | Login, retorna token Bearer |
| GET | `/auth/me` | autenticado | Dados do usuário do token |

### Tutores
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/tutores` | VETERINARIO | Listar (paginado, filtro `?nome=`) |
| GET | `/tutores/{id}` | próprio ou VET | Buscar por ID |
| PUT | `/tutores/{id}` | próprio TUTOR | Atualizar cadastro |
| DELETE | `/tutores/{id}` | próprio TUTOR | Excluir a própria conta |

### Pets
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/pets` | TUTOR | Cadastrar pet (dono = autenticado) |
| GET | `/pets` | autenticado | Tutor vê só os próprios; VET vê todos (`?tutorId=`, `?especie=`) |
| GET | `/pets/{id}` | dono ou VET | Buscar por ID |
| PUT | `/pets/{id}` | dono | Atualizar |
| DELETE | `/pets/{id}` | dono | Remover |
| GET | `/pets/{id}/linha-do-tempo` | dono ou VET | **Histórico de saúde unificado** (consultas + cuidados + IA), do mais recente ao mais antigo |

### Consultas
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/consultas/agendar` | TUTOR | Agendar (valida posse, futuro, disponibilidade) |
| POST | `/consultas/{id}/cancelar` | dono ou VET | Cancelar (só se AGENDADA) |
| POST | `/consultas/{id}/realizar` | VETERINARIO | Registrar atendimento (só se AGENDADA) |
| GET | `/consultas` | autenticado | Listar (`?status=`) — escopo por perfil |
| GET | `/consultas/{id}` | dono ou VET | Buscar por ID |
| GET | `/consultas/futuras` | VETERINARIO | Agenda clínica a partir de hoje |

### Cuidados preventivos (Lembretes)
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/lembretes` | VETERINARIO | Definir cuidado para um pet |
| GET | `/lembretes` | autenticado | Listar (`?status=`) — escopo por perfil |
| GET | `/lembretes/meus` | TUTOR | Agenda de cuidados dos próprios pets |
| GET | `/lembretes/{id}` | dono ou VET | Buscar por ID |
| GET | `/lembretes/pet/{petId}/pendentes` | dono ou VET | Pendentes de um pet |
| POST | `/lembretes/{id}/concluir` | TUTOR (dono) | Confirmar execução (+ recorrência) |
| POST | `/lembretes/{id}/cancelar` | VETERINARIO | Cancelar cuidado pendente |
| PUT | `/lembretes/{id}` | VETERINARIO | Editar dados (não altera status) |
| DELETE | `/lembretes/{id}` | VETERINARIO | Remover |

### Interações IA
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/interacoes-ia` | TUTOR | Enviar pergunta sobre um pet próprio |
| GET | `/interacoes-ia/pet/{petId}` | TUTOR (dono) | Histórico do pet |
| GET | `/interacoes-ia` | VETERINARIO | Listar todas (visão clínica) |
| GET | `/interacoes-ia/{id}` | VETERINARIO | Buscar por ID |

### Dashboard
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/dashboard/resumo` | VETERINARIO | KPIs gerais da plataforma |

### Infraestrutura
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/actuator/health` | público | Health check (usado pelo Render e pelo Docker) |

---

## Como Rodar

### Pré-requisitos
- Java 17+
- Maven 3.8+

### Via terminal
```bash
mvn spring-boot:run
```
A API sobe em **http://localhost:9090** (não 8080). Na primeira subida, o Flyway cria o schema e o `DataInitializer` carrega os dados de demonstração.

### Rodar os testes
```bash
mvn test
```
Inclui testes de contexto (Flyway + Security) e testes de integração de ponta a ponta (`FluxosDeNegocioIntegrationTest`) cobrindo login, isolamento entre tutores, agendamento/conflito/cancelamento/atendimento e conclusão/recorrência de cuidados — ver seção "Testes" abaixo.

> Atualização: a suíte agora tem 25 testes — contexto, integração de ponta a ponta e o `PostgresMigrationTest` (Testcontainers, ignorado se não houver Docker).

### Via Docker Compose (PostgreSQL, igual à produção)
Pré-requisito: Docker.
```bash
cp .env.example .env     # defina DB_PASSWORD e JWT_SECRET
docker compose up --build
```
- API: http://localhost:9090
- Health: http://localhost:9090/actuator/health → `{"status":"UP"}`
- Swagger: http://localhost:9090/swagger-ui.html

Para apagar o banco local e recomeçar: `docker compose down -v`.

---

## Deploy online (Render)

A aplicação está publicada em **https://petfamily-api.onrender.com**, com banco **PostgreSQL gerenciado** no Render.

| Item | Como foi feito |
|---|---|
| Infraestrutura como código | `render.yaml` (Blueprint): cria o banco `petfamily-db` e o serviço web `petfamily-api` |
| Build | `Dockerfile` multi-stage (Maven → JRE 17, usuário não-root) |
| Configuração | Variáveis de ambiente: `DB_*` vêm do banco, `JWT_SECRET` é gerado automaticamente |
| Perfil | `SPRING_PROFILES_ACTIVE=prod,demo` (PostgreSQL + dados de demonstração) |
| Health check | `/actuator/health` |
| Migrations | Flyway aplica V1–V7 automaticamente na subida |
| CI | GitHub Actions (`.github/workflows/ci.yml`): `mvn verify` + `docker build` a cada push |

**Para republicar:** basta dar `git push` na branch `main`; o Render faz o build e o deploy automaticamente.

**Teste rápido do ambiente online:**
```bash
curl https://petfamily-api.onrender.com/actuator/health
curl -X POST https://petfamily-api.onrender.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"veterinario@petfamily.com","senha":"senha123"}'
```

---


## Contas de demonstração (perfil `dev`)

| Perfil | E-mail | Senha |
|---|---|---|
| TUTOR | `pedro@petfamily.com` | `senha123` |
| TUTOR | `joao@petfamily.com` | `senha123` |
| TUTOR | `maria@petfamily.com` | `senha123` |
| TUTOR | `ana@petfamily.com` | `senha123` |
| **VETERINARIO** | `veterinario@petfamily.com` | `senha123` |

> Estas contas também existem no ambiente online (perfis `dev` e `demo`).

---

## Exemplos de Payload

### POST /auth/registrar
```json
{
  "nome": "Carlos Souza",
  "email": "carlos@email.com",
  "senha": "senha123",
  "telefone": "(11) 98765-4321"
}
```

### POST /consultas/agendar
```json
{
  "petId": 1,
  "tipoConsulta": "Vacinação",
  "data": "2026-06-15",
  "horario": "10:00",
  "observacoes": "Vacina V10 anual"
}
```

### POST /lembretes (definido pelo veterinário)
```json
{
  "titulo": "Vermifugação",
  "descricao": "Vermifugação trimestral do Rex",
  "dataLembrete": "2026-07-01",
  "tipo": "Preventivo",
  "petId": 1,
  "recorrenciaDias": 90
}
```

### POST /interacoes-ia
```json
{
  "pergunta": "O que fazer quando meu cachorro não quer comer?",
  "categoria": "Alimentação",
  "petId": 1
}
```

---

## Correspondência requisitos × implementação

| Requisito | Onde |
|---|---|
| Frontend funcional (30 pts) | App Expo integrado à API ([repositório mobile](https://github.com/PET-FAMILYY/PET-FAMILY-MOBILE)) |
| Flyway (20 pts) | `src/main/resources/db/migration/V1__*.sql`, `V2__*.sql`; `ddl-auto=validate` |
| Spring Security, 2 perfis, rotas protegidas (30 pts) | `security/*`, `@PreAuthorize` nos controllers + checagem de posse nos services |
| Funcionalidades além de CRUD (20 pts) | Agendamento/atendimento de consultas; cuidado preventivo com recorrência |
| Funcionalidades além de CRUD — adicionadas na entrega final | **Linha do tempo do pet**; aviso diário de cuidados; bloqueio de login por tentativas |
| Deploy online | Render + PostgreSQL gerenciado (`render.yaml`, `Dockerfile`) — URL no topo deste README |
| Flyway — atualização | 7 migrations (V1 por banco + V2–V7 comuns) |
| Qualidade / integração contínua | GitHub Actions, testes de integração e Testcontainers |

---

## Changelog técnico — o que mudamos no backend Java (entrega 03)

Lista objetiva de toda alteração no código Java, para facilitar a correção.



**Segurança** (`src/main/java/.../security/`)
- `JwtService.java` — gera/valida token JWT (HMAC-SHA256)
- `UsuarioPrincipal.java` — implementa `UserDetails`, carrega `usuarioId`/`tutorId`/`role`
- `CustomUserDetailsService.java`
- `JwtAuthenticationFilter.java` — lê `Authorization: Bearer`, popula o `SecurityContext`
- `SecurityConfig.java` — filter chain, CORS, CSRF desabilitado (com justificativa), regras de autorização
- `SecurityUtils.java` — helper para pegar o usuário/tutor autenticado nos services

**Entidades**
- `Usuario.java` — nova entidade (login), com `Role` enum `TUTOR`/`VETERINARIO`
- `ConsultaSlot.java` — modelo de disponibilidade (unique `data+horario`)

**Repositórios**
- `UsuarioRepository.java`
- `ConsultaSlotRepository.java`

**DTOs**
- `RegistroRequest`, `LoginRequest`, `LoginResponse`, `UsuarioResponse`
- `AgendarConsultaRequest`, `RealizarConsultaRequest`

**Exceções**
- `EmailJaCadastradoException`, `ConflitoOperacaoException`, `OperacaoInvalidaException`

**Service/Controller**
- `AuthService.java`, `AuthController.java` (`/auth/registrar`, `/auth/login`, `/auth/me`)

**Migrations Flyway**
- `db/migration/V1__schema_inicial.sql`
- `db/migration/V2__autenticacao_e_fluxos.sql`

**Testes**
- `FluxosDeNegocioIntegrationTest.java` (8 cenários via MockMvc)
- `src/test/resources/application-test.properties`

### Arquivos modificados

- **`pom.xml`** — adicionado `spring-boot-starter-security`, `jjwt-api/impl/jackson`, `flyway-core`, `spring-security-test`
- **`application.properties`** — porta 9090 documentada corretamente, H2 em arquivo, `ddl-auto=validate`, config Flyway, `jwt.secret`/`jwt.expiration-ms`, `cors.allowed-origins`, `spring.profiles.active=dev`
- **`Lembrete.java`** — campos novos: `recorrenciaDias`, `dataConclusao`, `criadoPor`, `concluidoPor`, `origemLembrete`
- **`ConsultaRepository.java`** — `findByPetTutorId(...)` e as queries atômicas `realizarSeAgendada`/`cancelarSeAgendada` (`@Modifying(clearAutomatically=true)`)
- **`LembreteRepository.java`** — `findByPetTutorId(...)`, `concluirSeAindaPendente`/`cancelarSeAindaPendente` (mesma técnica de CAS)
- **`ConsultaService.java`** — reescrito: `agendar`/`cancelar`/`realizar` no lugar do CRUD genérico, com checagem de posse
- **`LembreteService.java`** — reescrito: `criar` (só veterinário), `concluir` (com recorrência transacional), `cancelar`
- **`PetService.java`** — tutor nunca vem do payload, sempre do token; self-injection (`@Lazy`) para corrigir auto-invocação do `@Cacheable`
- **`TutorService.java`** — mesma correção de self-injection; `criar` removido (substituído por `/auth/registrar`)
- **`InteracaoIAService.java`** — checagem de posse do pet
- **`ConsultaController.java`, `LembreteController.java`, `PetController.java`, `TutorController.java`, `InteracaoIAController.java`, `DashboardController.java`** — `@PreAuthorize` por perfil; endpoints genéricos de escrita removidos/restritos em Consulta e Lembrete
- **`PetRequest.java`** — removido campo `tutorId`
- **`LembreteRequest.java`** — removido `status` (sempre nasce PENDENTE), adicionado `recorrenciaDias`
- **`ConsultaResponse.java`, `LembreteResponse.java`** — campos novos (`tutorId`, `recorrenciaDias`, `atrasado`, etc.)
- **`GlobalExceptionHandler.java`** — handlers para as exceções novas + `AccessDeniedException`/`BadCredentialsException`/405/404
- **`DataInitializer.java`** — `@Profile("dev")`, cria `Usuario` para cada tutor demo + 1 veterinário, reserva os slots das consultas AGENDADA; refatorado de um único método de ~250 linhas para 6 métodos privados focados (`criarTutoresDemo`, `criarUsuariosDemo`, `criarPetsDemo`, `criarConsultasDemo`, `criarCuidadosDemo`, `criarInteracoesDemo`)
- **`PetFamilyApplicationTests.java`** — `@ActiveProfiles("test")`
- **`SecurityUtils.java`** — adicionado `exigirTutorDono(tutorIdDoDono, mensagem)`, centralizando a checagem "VETERINARIO sempre passa; TUTOR só passa se for o dono", que estava duplicada em `PetService`, `TutorService`, `ConsultaService` e `LembreteService` (6 ocorrências do mesmo `if`)
- **`PetService.java`, `TutorService.java`, `ConsultaService.java`, `LembreteService.java`** — os métodos privados `verificarAcesso`/`verificarAcessoLeitura`/`verificarAcessoPet` (redundantes entre si) foram removidos; todos passaram a chamar `SecurityUtils.exigirTutorDono(...)`

### Entrega final — novidades

**Funcionalidades**
- `LinhaDoTempoService.java` + `EventoSaudeResponse.java` + `GET /pets/{id}/linha-do-tempo` — histórico de saúde unificado
- `LembreteNotificador.java` — aviso diário (08:00) de cuidados que vencem em até 3 dias
- `LoginAttemptService.java` + `TentativasExcedidasException.java` — bloqueio por 5 falhas (HTTP 429, janela de 15 min)

**Refatorações (redução de repetição)**
- `ErrorResponse.of(...)` — monta o corpo de erro em um só lugar (`GlobalExceptionHandler`)
- `@InvalidaCaches` + `CacheNames` — anotação composta que substitui blocos `@Caching(evict = {...})` repetidos
- `UsuarioAtual` — usuário autenticado resolvido uma vez e injetado nos services
- `AssistenteIASimulado` — respostas por tabela de regras, em vez de cadeia de `if`
- Token JWT passa a identificar o usuário por `usuarioId`

**Banco e infraestrutura**
- Migrations V3–V7 (integridade, índices, auditoria, FKs `ON DELETE SET NULL`); V1 separada por banco (`h2` e `postgresql`)
- Perfil `prod` com PostgreSQL via variáveis de ambiente (`application-prod.properties`); `spring.profiles.default=dev`; `server.port=${PORT:9090}`
- `Dockerfile`, `.dockerignore`, `docker-compose.yml`, `.env.example`
- `render.yaml` (Blueprint de deploy) e `.github/workflows/ci.yml`
- Spring Actuator (`/actuator/health`)

**Testes**
- `PostgresMigrationTest.java` (Testcontainers, ignorado sem Docker)

### Arquivo removido
- `ConsultaRequest.java` (DTO genérico do CRUD antigo — virou dead code depois do `ConsultaController` ser reescrito para as operações de negócio)
---

## Integrantes

| Nome | RM |
|---|---|
| Pedro Vaz Ferreira | 566551 |
| João Victor Luiz Oliveira Resende | 565139 |
| Vitor Dias dos Santos |  RM565422 |
| Felipe Kirschner Modesto | RM561810 |

---

## Estrutura do Projeto

```
PET--FAMILY-JAVA-main/
├── pom.xml
├── README.md
├── Dockerfile
├── docker-compose.yml
├── render.yaml
├── .env.example
├── .github/workflows/ci.yml
├── docs/
│   ├── cronograma.md
│   ├── arquitetura.md
│   ├── endpoints.md
│   └── postman_collection.json
├── PET-FAMILY-MOBILE-main/        ← app Expo (ver README próprio)
└── src/
    ├── main/
    │   ├── java/br/com/fiap/petfamily/
    │   │   ├── PetFamilyApplication.java
    │   │   ├── config/          (CacheConfig, CacheNames, InvalidaCaches, DataInitializer, OpenApiConfig)
    │   │   ├── controller/
    │   │   ├── dto/{request,response}/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   ├── security/        (JWT, filtros, SecurityConfig)
    │   │   └── service/
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/    (Flyway)
    └── test/
        ├── java/br/com/fiap/petfamily/
        └── resources/application-test.properties
```
