# Pet Family API

> Backend RESTful para acompanhamento contínuo da saúde dos pets  
> **Challenge CLYVO VET 2026 — FIAP | Java Advanced — 1º Sprint**

---

## Problema

Tutores de pets enfrentam dificuldades para manter a continuidade no cuidado veterinário: esquecem vacinas, perdem histórico de consultas, não possuem lembretes preventivos e não têm acesso fácil a orientações de saúde animal. O resultado é uma ruptura no cuidado que compromete o bem-estar dos animais.

## Solução

A **Pet Family API** centraliza e persiste todos os dados de saúde dos pets em um backend robusto, permitindo:

- Cadastrar tutores e seus pets com dados clínicos completos
- Registrar e acompanhar o histórico de consultas veterinárias
- Criar lembretes preventivos (vacinação, vermifugação, retornos)
- Interagir com um assistente de IA simulado para orientações de saúde animal
- Monitorar métricas clínicas via dashboard
- A Pet Family API centraliza e gerencia todas as informações relacionadas à saúde e ao bem-estar dos pets em um backend robusto, moderno e organizado, permitindo:
Cadastrar tutores e seus pets com dados clínicos completos;
Registrar e acompanhar o histórico de consultas veterinárias;
Criar lembretes preventivos, como vacinação, vermifugação, exames e retornos;
Interagir com um assistente de IA simulado para orientações básicas sobre saúde animal;
Monitorar métricas clínicas e informações importantes por meio de um dashboard.
Além disso, a solução busca facilitar o acompanhamento da saúde dos animais, melhorar a comunicação entre tutores e clínicas veterinárias e incentivar cuidados preventivos de forma prática, eficiente e acessível.
A aplicação foi desenvolvida utilizando Java e Spring Boot, seguindo os princípios da Programação Orientada a Objetos (POO), arquitetura RESTful e boas práticas de desenvolvimento, garantindo organização, escalabilidade e facilidade de manutenção.

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Web | — |
| Spring Data JPA | — |
| Spring Cache + Caffeine | — |
| Bean Validation | — |
| H2 Database | — |
| Lombok | — |
| SpringDoc OpenAPI / Swagger | 2.3.0 |
| Maven | — |

---

## Entidades e Relacionamentos

```
Tutor (1) ────── (N) Pet
                      │
                      ├── (N) Consulta
                      ├── (N) Lembrete
                      └── (N) InteracaoIA
```

| Entidade | Campos Principais |
|---|---|
| Tutor | id, nome, email, telefone |
| Pet | id, nome, especie, raca, idade, peso, observacoesSaude, tutorId |
| Consulta | id, data, horario, tipoConsulta, status, observacoes, petId |
| Lembrete | id, titulo, descricao, dataLembrete, tipo, status, petId |
| InteracaoIA | id, pergunta, resposta, dataHora, categoria, petId |

---

## Endpoints

### Tutores
| Método | Rota | Descrição |
|---|---|---|
| POST | `/tutores` | Cadastrar tutor |
| GET | `/tutores` | Listar (paginado, filtro `?nome=`) |
| GET | `/tutores/{id}` | Buscar por ID |
| PUT | `/tutores/{id}` | Atualizar |
| DELETE | `/tutores/{id}` | Remover |

### Pets
| Método | Rota | Descrição |
|---|---|---|
| POST | `/pets` | Cadastrar pet |
| GET | `/pets` | Listar (`?tutorId=`, `?especie=`, paginado) |
| GET | `/pets/{id}` | Buscar por ID |
| PUT | `/pets/{id}` | Atualizar |
| DELETE | `/pets/{id}` | Remover |

### Consultas
| Método | Rota | Descrição |
|---|---|---|
| POST | `/consultas` | Registrar consulta |
| GET | `/consultas` | Listar (`?status=AGENDADA\|REALIZADA\|CANCELADA`) |
| GET | `/consultas/{id}` | Buscar por ID |
| GET | `/consultas/futuras` | Consultas a partir de hoje |
| PUT | `/consultas/{id}` | Atualizar |
| DELETE | `/consultas/{id}` | Remover |

### Lembretes
| Método | Rota | Descrição |
|---|---|---|
| POST | `/lembretes` | Criar lembrete |
| GET | `/lembretes` | Listar (`?status=PENDENTE\|CONCLUIDO\|CANCELADO`) |
| GET | `/lembretes/{id}` | Buscar por ID |
| GET | `/lembretes/pet/{petId}/pendentes` | Pendentes de um pet |
| PUT | `/lembretes/{id}` | Atualizar |
| DELETE | `/lembretes/{id}` | Remover |

### Interações IA
| Método | Rota | Descrição |
|---|---|---|
| POST | `/interacoes-ia` | Enviar pergunta ao assistente |
| GET | `/interacoes-ia` | Listar todas (paginado) |
| GET | `/interacoes-ia/{id}` | Buscar por ID |
| GET | `/interacoes-ia/pet/{petId}` | Histórico do pet |

### Dashboard
| Método | Rota | Descrição |
|---|---|---|
| GET | `/dashboard/resumo` | KPIs gerais da plataforma |

---

## Como Rodar

### Pré-requisitos
- Java 17+
- Maven 3.8+
- IntelliJ IDEA (recomendado)

### No IntelliJ IDEA

1. **Abrir o projeto:**  
   `File > Open` → selecionar a pasta `java-backend`

2. **Aguardar download das dependências** (Maven sync automático)

3. **Habilitar anotações do Lombok:**  
   `Settings > Build > Compiler > Annotation Processors > Enable annotation processing`

4. **Executar:**  
   Abrir `PetFamilyApplication.java` e clicar no botão ▶ Run

### Via terminal

```bash
cd java-backend
mvn spring-boot:run
```

---

## Acessos

| Recurso | URL |
|---|---|
| API Base | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| H2 Console | http://localhost:8080/h2-console |

**H2 Console — Configurações:**
- JDBC URL: `jdbc:h2:mem:petfamilydb`
- Username: `sa`
- Password: *(vazio)*

---

## Exemplos de Payload

### POST /tutores
```json
{
  "nome": "Carlos Souza",
  "email": "carlos@email.com",
  "telefone": "(11) 98765-4321"
}
```

### POST /pets
```json
{
  "nome": "Rex",
  "especie": "Cachorro",
  "raca": "Labrador",
  "idade": 4,
  "peso": 32.0,
  "observacoesSaude": "Sem alergias. Castrado.",
  "tutorId": 1
}
```

### POST /consultas
```json
{
  "data": "2026-06-15",
  "horario": "10:00",
  "tipoConsulta": "Vacinação",
  "status": "AGENDADA",
  "observacoes": "Vacina V10 anual",
  "petId": 1
}
```

### POST /lembretes
```json
{
  "titulo": "Vermifugação",
  "descricao": "Vermifugação trimestral do Rex",
  "dataLembrete": "2026-07-01",
  "tipo": "Preventivo",
  "status": "PENDENTE",
  "petId": 1
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

### GET /dashboard/resumo — Resposta
```json
{
  "totalTutores": 4,
  "totalPets": 5,
  "totalConsultas": 6,
  "totalLembretesPendentes": 4,
  "totalInteracoesIA": 3,
  "taxaAdesaoPreventiva": 33.3
}
```

### Paginação e Ordenação
```
GET /pets?page=0&size=10&sort=nome,asc
GET /consultas?status=AGENDADA&page=0&size=5&sort=data,asc
GET /lembretes?status=PENDENTE&page=0&size=10
```

---

## Integrantes

| Nome | RM |
|---|---|
| Pedro Vaz Ferreira | 566551 | 
| João Victor Luiz Oliveira Resende | 565139 |

---

## Estrutura do Projeto

```
java-backend/
├── pom.xml
├── README.md
├── docs/
│   ├── cronograma.md
│   ├── arquitetura.md
│   ├── endpoints.md
│   └── postman_collection.json
└── src/
    └── main/
        ├── java/br/com/fiap/petfamily/
        │   ├── PetFamilyApplication.java
        │   ├── config/
        │   │   ├── CacheConfig.java
        │   │   ├── DataInitializer.java
        │   │   └── OpenApiConfig.java
        │   ├── controller/
        │   ├── dto/
        │   │   ├── request/
        │   │   └── response/
        │   ├── entity/
        │   ├── exception/
        │   ├── repository/
        │   └── service/
        └── resources/
            └── application.properties
```
