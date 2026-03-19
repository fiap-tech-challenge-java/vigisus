# 🏥 VígiSUS

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot"/>
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql"/>
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker"/>
  <img src="https://img.shields.io/badge/Dados-Abertos_SUS-009C3B?style=for-the-badge"/>
</p>

<p align="center">
  <strong>Plataforma pública de vigilância epidemiológica do SUS</strong><br/>
  Histórico de dengue · Previsão de risco climático · Encaminhamento inteligente de pacientes<br/>
  Dados abertos do DATASUS, IBGE e Open-Meteo — sem login, sem cadastro, zero input de funcionário
</p>

---

## 📋 Sumário

- [O Problema](#-o-problema)
- [A Solução](#-a-solução)
- [Diferencial](#-diferencial)
- [Funcionalidades](#-funcionalidades)
- [Arquitetura](#-arquitetura)
- [Dados Públicos Utilizados](#-dados-públicos-utilizados)
- [Como Rodar](#-como-rodar)
- [Endpoints da API](#-endpoints-da-api)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Time](#-time)
- [Limitações](#-limitações-honestas)

---

## 🚨 O Problema

O Brasil tem um dos sistemas de vigilância epidemiológica mais completos do mundo. O SINAN registra todos os casos de dengue. O CNES registra cada hospital, cada leito, cada especialidade. O IBGE tem a população de cada município.

**O problema: esses dados existem mas estão presos.**

| Persona | Dor real |
|---|---|
| **Gestor municipal** | Elabora relatório epidemiológico manualmente, copiando dado por dado do TabNet. Leva dias. |
| **Médico da UPA** | Paciente com dengue grave precisando de UTI. Liga para a central de regulação, fica em espera, descobre que o leito "disponível" já está ocupado. |
| **Secretário de saúde** | Não sabe em tempo útil se o município está entrando numa epidemia. |
| **Cidadão** | Não tem como saber se há surto no bairro, nem onde buscar atendimento especializado. |

> Em 2024, o Brasil registrou mais de **6 milhões de casos** de dengue e mais de **6.000 mortes** — o pior ano da história. A maioria dos municípios pequenos não tem epidemiologista.

---

## 💡 A Solução

O VígiSUS transforma dados públicos do SUS em informação útil para qualquer pessoa, em linguagem simples, sem depender de nenhum funcionário para alimentar o sistema.

O usuário digita uma pergunta:

```
"casos de dengue em Lavras MG em 2025"
"qual hospital mais próximo para internar um paciente com dengue grave"
"qual o risco de dengue em Lavras nas próximas 2 semanas"
```

E recebe:

- 📊 Histórico real de casos por semana epidemiológica
- 🌡️ Previsão de risco baseada em clima + sazonalidade histórica
- 🏥 Hospitais com leitos SUS, distância e telefone
- 🤖 Texto explicativo gerado por IA em linguagem simples

---

## 🎯 Diferencial

| O que existe hoje | O que o VígiSUS entrega |
|---|---|
| Dashboard só para Rio e SP, por grandes equipes | API REST + front para **qualquer município** do Brasil |
| Dado epidemiológico isolado | Cruzamento SINAN + IBGE + clima + CNES |
| Relatório estático em PDF | JSON consumível por qualquer sistema |
| Exige epidemiologista para operar | **Linguagem natural** — qualquer pessoa acessa |
| Só mostra o passado | **Previsão de risco** para as próximas 2 semanas |
| Regulação por telefone | Encaminhamento por dados — hospital mais próximo com a estrutura necessária |

> **Princípio central:** nenhum funcionário precisa preencher nada. O sistema funciona 100% com dados que já existem em fontes públicas.

---

## ✅ Funcionalidades

### 1. Consulta epidemiológica
- Histórico de casos por doença, município e período
- Curva epidemiológica semanal com gráfico interativo
- Incidência por 100 mil habitantes
- Ranking do município vs média estadual vs média nacional

### 2. Previsão de risco
- Cruzamento do padrão sazonal histórico (SINAN) com previsão climática (Open-Meteo)
- Score de risco: 🟢 Baixo · 🟡 Moderado · 🟠 Alto · 🔴 Muito Alto
- Base científica: temperatura > 25°C + chuva acima do normal = favorável ao *Aedes aegypti*

### 3. Encaminhamento inteligente
- Hospital mais próximo com leito SUS disponível para a condição clínica
- Filtro por tipo de leito (clínico, UTI adulto, UTI pediátrica)
- Filtro por serviço especializado (infectologia, neurologia etc.)
- Distância calculada por fórmula de Haversine — sem API de mapas

### 4. Busca por linguagem natural (IA)
- Usuário digita em português livre
- IA interpreta a intenção, consulta a API e narra os dados
- A IA nunca inventa dado — só narra o que o banco já tem

---

## 🏗️ Arquitetura

```
┌──────────────────────────────────────────────────────────────┐
│                     FONTES DE DADOS                          │
│  FTP DATASUS    OpenDataSUS    IBGE API     Open-Meteo API   │
│  (SINAN/CNES)   (CSV dengue)  (municípios)  (clima)         │
└──────────┬──────────┬──────────────┬──────────┬─────────────┘
           │          │              │          │
           ▼          ▼              ▼          ▼
┌──────────────────────────────────────────────────────────────┐
│             JOB DE INGESTÃO — Spring Batch                   │
│  SINAN/CNES: 1× por semana (FTP)                             │
│  IBGE/Clima: tempo real (API REST)                           │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                     PostgreSQL                               │
│  casos_dengue · estabelecimentos · leitos                    │
│  servicos · municipios · populacao                           │
└──────────────┬────────────────────────┬──────────────────────┘
               ▼                        ▼
┌──────────────────────┐   ┌────────────────────────────────┐
│   API REST           │   │   Serviço de IA                │
│   Spring Boot        │◄──│   Claude / OpenAI              │
│                      │   │                                │
│   /perfil            │   │   recebe: contexto com dados   │
│   /previsao-risco    │   │   retorna: texto explicativo   │
│   /encaminhar        │   │   nunca inventa dado           │
│   /hospitais         │   └────────────────────────────────┘
└──────────┬───────────┘
           ▼
┌──────────────────────────────────────────────────────────────┐
│                   FRONT-END — React                          │
│   Busca natural · Gráficos (Chart.js) · Mapa (Leaflet.js)   │
│   100% público · sem login · sem cadastro                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 📦 Dados Públicos Utilizados

### SINAN — FTP DATASUS

```
ftp.datasus.gov.br  (anonymous, sem senha)
/dissemin/publicos/SINAN/DADOS/FINAIS/   → DENGBR{AA}.dbc
/dissemin/publicos/SINAN/DADOS/PRELIM/   → DENGBR{AA}.dbc (ano atual)
```

### CNES — FTP DATASUS

```
/dissemin/publicos/CNES/200508_/Dados/ST/  → ST{UF}{AAMM}.dbc  (estabelecimentos)
/dissemin/publicos/CNES/200508_/Dados/LT/  → LT{UF}{AAMM}.dbc  (leitos)
/dissemin/publicos/CNES/200508_/Dados/SR/  → SR{UF}{AAMM}.dbc  (serviços)
```

### IBGE — API REST (tempo real, sem auth)

```
https://servicodados.ibge.gov.br/api/v1/localidades/municipios/{cod}
https://servicodados.ibge.gov.br/api/v3/agregados/6579/...  (população)
```

### Open-Meteo — API REST (tempo real, sem auth)

```
https://api.open-meteo.com/v1/forecast   (clima atual + 16 dias)
https://api.open-meteo.com/v1/archive    (histórico desde 1940)
```

---

## 🚀 Como Rodar

### Pré-requisitos

- Java 17+
- Node 18+
- Docker e Docker Compose
- Python 3.10+ (para pipeline de ingestão)

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/vigisus.git
cd vigisus
```

### 2. Suba o banco com Docker

```bash
docker-compose up -d postgres
```

### 3. Execute o pipeline de dados

```bash
cd data-pipeline
pip install pysus pandas requests tabulate
python cnes_download_converter.py   # baixa e converte CNES
python sus_completo.py              # baixa SINAN + valida APIs
```

### 4. Suba o back-end

```bash
cd backend
./mvnw spring-boot:run
# Swagger disponível em: http://localhost:8080/swagger-ui.html
```

### 5. Suba o front-end

```bash
cd frontend
npm install
npm run dev
# Disponível em: http://localhost:5173
```

### Ou suba tudo com Docker Compose

```bash
docker-compose up --build
```

---

## 📡 Endpoints da API

### `GET /api/perfil/{codigo_ibge}`

```json
{
  "municipio": "Lavras",
  "uf": "MG",
  "populacao": 102000,
  "doenca": "dengue",
  "ano": 2024,
  "total_casos": 1842,
  "incidencia_100k": 1805.0,
  "classificacao": "EPIDEMIA",
  "semanas": [
    { "semana_epi": 1, "casos": 12 },
    { "semana_epi": 2, "casos": 28 }
  ],
  "comparativo_estado": {
    "media_mg": 980.0,
    "posicao_ranking": 47
  }
}
```

### `GET /api/previsao-risco/{codigo_ibge}`

```json
{
  "municipio": "Lavras",
  "score": 7,
  "classificacao": "MUITO ALTO",
  "fatores": [
    "Temperatura prevista 29.4°C nos próximos 14 dias",
    "Probabilidade de chuva 72% — acima da média histórica"
  ],
  "texto_ia": "Lavras está entrando em condições climáticas..."
}
```

### `GET /api/encaminhar`

```
?municipio=313130&condicao=dengue&gravidade=grave
```

```json
{
  "municipio_origem": "Lavras",
  "tipo_leito_buscado": "UTI Adulto (tipo 81)",
  "hospitais": [
    {
      "nome": "Hospital Regional de Varginha",
      "distancia_km": 98.3,
      "leitos_uti_sus": 12,
      "servico_infectologia": true,
      "telefone": "(35)3690-2500"
    }
  ]
}
```

### `POST /api/busca`

```json
{ "pergunta": "dengue em Lavras 2024" }
```

```json
{
  "interpretacao": { "doenca": "dengue", "municipio": 3131307, "ano": 2024 },
  "texto_ia": "Em 2024, Lavras registrou 1.842 casos de dengue...",
  "dados": { }
}
```

> Documentação completa disponível em `/swagger-ui.html` após subir o back-end.

---

## 📁 Estrutura do Projeto

```
vigisus/
├── backend/
│   ├── src/main/java/br/com/vigisus/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── integration/        ← clientes IBGE, Open-Meteo, IA
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── services/
│   └── package.json
├── data-pipeline/
│   ├── cnes_download_converter.py
│   ├── cnes_ftp_download.py
│   └── sus_completo.py
├── docs/
│   └── hackathon-sus-resumo.md
├── docker-compose.yml
└── README.md
```

---

## 👥 Time

| Nome | Responsabilidade |
|---|---|
| Dev 1 | Job de ingestão — Spring Batch, download SINAN/CNES |
| Dev 2 | API REST — Spring Boot, endpoints principais |
| Dev 3 | Serviço de IA — integração Claude/OpenAI |
| Dev 4 | Front-end — React, Chart.js, Leaflet.js |
| Dev 5 | Banco + infra + Swagger + documentação |

---

## ⚠️ Limitações Honestas

**O CNES mostra capacidade, não ocupação em tempo real**
O CNES informa quantos leitos o hospital *tem*. A confirmação de disponibilidade no momento do encaminhamento deve ser feita por telefone — número que a plataforma já fornece.

**O SINAN tem defasagem de 2 a 4 semanas**
Os dados epidemiológicos não são em tempo real. A previsão de risco usa dados climáticos em tempo real (Open-Meteo) para compensar essa defasagem.

**A previsão é baseada em regras, não em ML**
O modelo usa evidência científica publicada sobre correlação clima-dengue. ML é o próximo passo natural no roadmap.

---

## 📚 Referências

| Fonte | URL |
|---|---|
| DATASUS / SINAN | http://datasus.saude.gov.br |
| FTP DATASUS | ftp://ftp.datasus.gov.br/dissemin/publicos/ |
| OpenDataSUS | https://dadosabertos.saude.gov.br |
| IBGE Localidades API | https://servicodados.ibge.gov.br |
| Open-Meteo | https://open-meteo.com |
| pysus | https://github.com/AlertaDengue/PySUS |

---

<p align="center">
  Desenvolvido para o <strong>Hackathon FIAP Pós Tech — Fase 5</strong><br/>
  Tema: Inovação para otimização de atendimento no SUS
</p>
