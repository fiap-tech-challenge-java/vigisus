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

- **Backend**: Java 17 + Spring Boot 3.2 + Spring Data JPA + PostgreSQL + Flyway
- **Cache**: Caffeine (perfil, ranking, risco, clima)
- **IA**: Google Gemini com fallback determinístico (sem dependência de chave externa)
- **Frontend**: React 18 + React Router + Tailwind + Chart.js + Leaflet
- **Data Pipeline**: Python 3.11 + requests + ftplib + pandas
- **Arquitetura**: DDD + Clean Architecture + SOLID + Ports & Adapters
- **Testes**: JUnit 5 + Mockito + MockMvc | 91.87% line coverage / 80% branch coverage (JaCoCo)
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
| POST | `/api/busca` | Busca por linguagem natural com IA |
| GET | `/api/perfil/{coIbge}` | Perfil epidemiológico do município |
| GET | `/api/previsao-risco/{coIbge}` | Previsão de risco climático municipal |
| GET | `/api/ranking` | Ranking de municípios por incidência |
| POST | `/api/triagem/avaliar` | Triagem orientativa de paciente |
| GET | `/api/encaminhar` | Encaminhamento para hospital com leito disponível |
| POST | `/api/operacional/pressao` | Análise de pressão operacional |
| GET | `/api/brasil/dashboard` | Dashboard agregado nacional |
| GET | `/api/actuator/health` | Health check da API |

Documentação completa: `http://localhost:8080/swagger-ui.html`

---

### Equipe

Projeto desenvolvido para o **FIAP Tech Challenge** — Hackathon SUS.
