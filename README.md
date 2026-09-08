# Pet Family API

> Backend RESTful para acompanhamento contínuo da saúde dos pets
> **Challenge CLYVO VET 2026 — FIAP | Java Advanced — Entrega 3**

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

O app cliente é o **Pet Family Mobile** (Expo/React Native) — ver [`PET-FAMILY-MOBILE-main/README.md`](./PET-FAMILY-MOBILE-main/PET-FAMILY-MOBILE-main/README.md).

---

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
| H2 Database (arquivo local) | — |
| Lombok | — |
| SpringDoc OpenAPI / Swagger | 2.3.0 |
| Maven | — |

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
| Consulta | id, data, horario, tipoConsulta, status, observacoes, petId |
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

### Variáveis de ambiente (sem segredos reais commitados)

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `JWT_SECRET` | *(fallback de dev embutido)* | Segredo HMAC do token. **Troque em qualquer ambiente real.** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Validade do token. |
| `CORS_ALLOWED_ORIGINS` | `*` | Lista separada por vírgula em produção. |
| `SPRING_PROFILES_ACTIVE` | `dev` | Ver seção Flyway/dados de demonstração. |

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

**Como criar uma nova migration:** adicione um arquivo `V3__descricao_curta.sql` em `src/main/resources/db/migration` (numeração sempre crescente) e rode a aplicação — o Flyway aplica automaticamente na subida. **Nunca edite uma migration já aplicada**; toda mudança de schema vira uma migration nova.

### Banco de dados

- **Desenvolvimento local:** H2 em **arquivo** (`./data/petfamily.mv.db`, ignorado pelo git) — os dados persistem entre reinicializações. `AUTO_SERVER=TRUE` permite abrir o H2 Console com a aplicação já rodando.
- **Testes automatizados:** H2 em **memória**, isolado (`src/test/resources/application-test.properties`, perfil `test`) — cada execução de teste começa de um banco limpo, migrado do zero.
- Não há conexão com banco externo/nuvem — isso é documentado aqui de propósito, para não passar a impressão de que existe algo além do H2 local.

### Dados de demonstração

O `DataInitializer` roda **apenas no perfil `dev`** (`spring.profiles.active` já é `dev` por padrão em `application.properties`) e é idempotente: só popula dados se o banco estiver vazio. Para subir sem dados fictícios (ex.: simular "produção"), rode com outro perfil:
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

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

---

## Acessos

| Recurso | URL |
|---|---|
| API Base | http://localhost:9090 |
| Swagger UI | http://localhost:9090/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:9090/api-docs |
| H2 Console | http://localhost:9090/h2-console |

**H2 Console — Configurações (perfil dev, arquivo local):**
- JDBC URL: `jdbc:h2:file:./data/petfamily`
- Username: `sa`
- Password: *(vazio)*

---

## Contas de demonstração (perfil `dev`)

| Perfil | E-mail | Senha |
|---|---|---|
| TUTOR | `pedro@petfamily.com` | `senha123` |
| TUTOR | `joao@petfamily.com` | `senha123` |
| TUTOR | `maria@petfamily.com` | `senha123` |
| TUTOR | `ana@petfamily.com` | `senha123` |
| **VETERINARIO** | `veterinario@petfamily.com` | `senha123` |

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

## Roteiro de apresentação (alternando Tutor e Veterinário)

1. **Login como tutor** (`pedro@petfamily.com` / `senha123`) → tela Home mostra os pets, cuidados pendentes e próximas consultas.
2. Aba **Meus Pets** → cadastrar um novo pet (multi-pet por tutor).
3. Aba **Consulta** → agendar uma consulta futura para esse pet. Tentar agendar outra no mesmo horário mostra o erro de conflito.
4. Aba **Cuidados** → mostra os cuidados definidos pela clínica (ainda vazio para o pet novo).
5. **Logout** → login como veterinário (`veterinario@petfamily.com` / `senha123`).
6. Aba **Atendimentos** → localizar a consulta recém-agendada e clicar em "Realizar", preenchendo as observações do atendimento.
7. Aba **Cuidados** (veterinário) → criar um cuidado preventivo recorrente para o pet do tutor (ex.: a cada 90 dias).
8. Aba **Indicadores** → mostrar o dashboard clínico atualizado.
9. **Logout** → login novamente como o tutor.
10. Aba **Cuidados** → o novo cuidado aparece pendente; concluir → a próxima ocorrência é criada automaticamente na agenda.
11. Aba **Consulta** → a consulta agendada anteriormente agora aparece como Realizada.
12. Aba **Chat IA** → perguntar algo sobre o pet (ex.: "quais vacinas ele precisa?") e mostrar a resposta simulada, com histórico persistido.

---

## Testes executados

- `mvn test` — 8 testes, 0 falhas: contexto Spring (Flyway + Security) e o fluxo completo via `MockMvc`:
  - Login com senha errada → 401; rota protegida sem token → 401.
  - Isolamento entre dois tutores (um não acessa pet do outro) → 403.
  - Agendamento no passado → 400; agendamento duplicado no mesmo horário → 409.
  - Atendimento (realizar) e transições inválidas (realizar/cancelar consulta já finalizada) → 409.
  - Cuidado só pode ser criado por veterinário; tutor de outro pet não pode concluir → 403.
  - Conclusão de cuidado, bloqueio de dupla conclusão (409) e criação automática da próxima ocorrência recorrente.
- Testado manualmente via `curl` contra a aplicação rodando (login, `/auth/me`, isolamento de pets, agendar/conflito/passado/realizar/duplicar, cancelar+reagendar no mesmo horário, criar cuidado/concluir/recorrência/duplicar, `/dashboard/resumo` restrito a veterinário, 401/403/404/405 consistentes).
- Auditoria adicional (rodando a aplicação, não só lendo código): exclusão da própria conta do tutor cascateia corretamente até `Usuario` e `Pet` (login com a conta excluída passa a falhar); veterinário não consegue criar pet (403); veterinário consegue cancelar consulta de um tutor (200); pet inexistente retorna 404 (não 500); tutor não consegue editar pet de outro tutor (403).
- Migrations e persistência: reiniciar a aplicação com o mesmo arquivo H2 mantém os dados e o Flyway relata "Schema is up to date" (sem recriar nada).
- Mobile: **type-check** (`npx tsc --noEmit`) passando sem erros, inclusive após os refactors de DRY. A execução em dispositivo/emulador real não pôde ser feita neste ambiente (sem display/dispositivo disponível) — ver roteiro manual no README do mobile. O bundling via Metro (`npx expo export`) também não pôde ser validado por uma incompatibilidade de ferramental do ambiente (Node 22), então **não há confirmação visual de que as telas renderizam sem erro em runtime** — esse é o maior ponto de incerteza desta entrega.

### Revisão de qualidade (SOLID/DRY) feita após a auditoria inicial

- `DataInitializer.loadData()` era um único método de ~250 linhas fazendo tudo — quebrado em 6 métodos privados, cada um com uma responsabilidade.
- A checagem "veterinário sempre passa; tutor só passa se for o dono" estava duplicada, com o mesmo `if`, em `PetService`, `TutorService`, `ConsultaService` e `LembreteService` (6 ocorrências) — centralizada em `SecurityUtils.exigirTutorDono(...)`.
- No mobile, a validação de formato de data/horário (`isDataValida`/`isHorarioValido`) estava duplicada entre `appointment.tsx` e `vet-cuidados.tsx` — extraída para `src/utils/validators.ts`.
- Essa não foi uma varredura linha-a-linha de 100% dos arquivos — foram corrigidas as duplicações/métodos grandes mais evidentes encontrados numa auditoria direcionada.

---

## Correspondência requisitos × implementação

| Requisito | Onde |
|---|---|
| Frontend funcional (30 pts) | App Expo integrado à API (ver README do mobile) |
| Flyway (20 pts) | `src/main/resources/db/migration/V1__*.sql`, `V2__*.sql`; `ddl-auto=validate` |
| Spring Security, 2 perfis, rotas protegidas (30 pts) | `security/*`, `@PreAuthorize` nos controllers + checagem de posse nos services |
| Funcionalidades além de CRUD (20 pts) | Agendamento/atendimento de consultas; cuidado preventivo com recorrência |

---

## Changelog técnico — o que mudou no backend Java (entrega 3)

Lista objetiva de toda alteração no código Java, para facilitar a correção comparando com as entregas 1–2.

### Arquivos novos

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

### Arquivo removido
- `ConsultaRequest.java` (DTO genérico do CRUD antigo — virou dead code depois do `ConsultaController` ser reescrito para as operações de negócio)

### Bugs reais encontrados corrigindo em runtime (não só lendo código)

- H2 `AUTO_SERVER=TRUE` + `DB_CLOSE_ON_EXIT=FALSE` incompatíveis entre si — a aplicação não subia.
- `LazyInitializationException` em vários endpoints de listagem por falta de `@Transactional` nos métodos de leitura dos services.
- Resposta de `realizar`/`concluir` retornando dado obsoleto — um `UPDATE` em massa (`@Modifying`) não atualiza o contexto de persistência do Hibernate; corrigido com `clearAutomatically=true`.
- `/auth/me` retornando 500 em vez de 401 sem token — estava (erroneamente) na lista de rotas públicas do filtro JWT.
- Método HTTP não suportado retornando 500 em vez de 405 — faltava handler específico no `GlobalExceptionHandler`.

---

## Integrantes

| Nome | RM |
|---|---|
| Pedro Vaz Ferreira | 566551 |
| João Victor Luiz Oliveira Resende | 565139 |

---

## Estrutura do Projeto

```
PET--FAMILY-JAVA-main/
├── pom.xml
├── README.md
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
    │   │   ├── config/          (CacheConfig, DataInitializer, OpenApiConfig)
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
