# Como Demonstrar o MVP do VígiSUS

Guia passo a passo para demonstrar as funcionalidades do VígiSUS no Swagger UI.

---

## Passo 1 — Suba o projeto

```bash
# Sobe o banco de dados PostgreSQL em segundo plano
docker-compose up -d

# Inicia o backend com o perfil de demonstração (carrega dados automaticamente)
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

> O perfil `dev` configura `ddl-auto: create-drop` e aciona o **DataLoader**, que insere
> automaticamente o município de Lavras, três hospitais e 104 semanas de casos de dengue
> (2023 e 2024). A carga é idempotente — pode ser executada várias vezes sem duplicar dados.

---

## Passo 2 — Abra o Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Passo 3 — Demonstre na ordem

### a. Perfil Epidemiológico

```
GET /api/perfil/3131307?doenca=dengue&ano=2024
```

**O que mostra:** histórico de casos de dengue em Lavras (MG) para 2024, incidência por
100 mil habitantes e a classificação **EPIDEMIA** (incidência > 300/100 k).

---

### b. Previsão de Risco Climático

```
GET /api/previsao-risco/3131307
```

**O que mostra:** score de risco calculado com base em temperatura, umidade e precipitação
obtidos em tempo real da API Open-Meteo, com classificação de **BAIXO** a **MUITO_ALTO**.

---

### c. Encaminhamento de Pacientes

```
GET /api/encaminhar?municipio=3131307&condicao=dengue&gravidade=grave
```

**O que mostra:** lista de hospitais com leitos de UTI (`tp_leito=81`) ordenados pela
distância em quilômetros a partir de Lavras, com nome, telefone e número de leitos SUS
disponíveis.

---

### d. Busca Inteligente com IA

```
POST /api/busca
Content-Type: application/json

{
  "pergunta": "dengue em Lavras MG 2024"
}
```

**O que mostra:** integração completa — o serviço identifica a intenção da pergunta,
consulta o perfil epidemiológico e retorna uma resposta em linguagem natural gerada pela IA.

> **Pré-requisito:** configure a variável de ambiente `VIGISUS_IA_API_KEY` com uma chave
> válida da OpenAI antes de iniciar o backend.

---

## Referência rápida

| Recurso        | URL                                                                         |
|----------------|-----------------------------------------------------------------------------|
| Swagger UI     | <http://localhost:8080/swagger-ui.html>                                     |
| API Docs (JSON)| <http://localhost:8080/v3/api-docs>                                         |
| Frontend       | <http://localhost:5173>                                                     |
| PostgreSQL     | `localhost:5432` · database `vigisus` · user `vigisus` · senha `vigisus123` |
