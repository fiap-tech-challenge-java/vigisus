# Hackathon SUS — Resumo Técnico

## Vigisus: Plataforma de Vigilância Epidemiológica do SUS

### Descrição do Projeto

O **Vigisus** é uma plataforma pública de vigilância epidemiológica do SUS que integra dados abertos do DATASUS, IBGE e Open-Meteo para oferecer:

- **Histórico de dengue** por município, com visualização temporal e geográfica.
- **Previsão de risco climático** combinando dados meteorológicos e sazonalidade epidemiológica.
- **Encaminhamento inteligente de pacientes** com base na capacidade instalada dos estabelecimentos de saúde (CNES).

---

### Fontes de Dados

| Fonte | Dados | Formato |
|-------|-------|---------|
| DATASUS / SINAN | Notificações de dengue | DBC/CSV |
| DATASUS / CNES | Estabelecimentos de saúde | DBC/CSV |
| IBGE | Municípios, populações | JSON (API REST) |
| Open-Meteo | Temperatura, precipitação | JSON (API REST) |

---

### Arquitetura

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  data-pipeline  │────▶│    PostgreSQL    │◀────│    backend      │
│  (Python)       │     │    (banco)       │     │  (Spring Boot)  │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                          │ REST API
                                                 ┌────────▼────────┐
                                                 │    frontend     │
                                                 │    (React)      │
                                                 └─────────────────┘
```

### Stack Tecnológico

- **Backend**: Java 17 + Spring Boot 3 + Spring Data JPA + PostgreSQL
- **Frontend**: React 18 + React Router + Recharts
- **Data Pipeline**: Python 3.11 + requests + ftplib
- **Infraestrutura**: Docker + Docker Compose

---

### Como Executar

```bash
# Subir toda a stack
docker-compose up --build

# Rodar apenas o pipeline de dados
cd data-pipeline
python sus_completo.py SP 2024-01-01 2024-12-31

# Backend (desenvolvimento)
cd backend
mvn spring-boot:run

# Frontend (desenvolvimento)
cd frontend
npm install && npm start
```

---

### Endpoints Principais da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/dengue/{municipioId}` | Histórico de dengue por município |
| GET | `/api/estabelecimentos/{municipioId}` | Estabelecimentos de saúde próximos |
| GET | `/api/risco-climatico/{municipioId}` | Previsão de risco climático |

---

### Equipe

Projeto desenvolvido para o **FIAP Tech Challenge** — Hackathon SUS.
