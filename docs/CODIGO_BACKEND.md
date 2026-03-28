# Backend (Spring Boot) - Documentacao de Codigo

## 1. Objetivo do modulo
O backend em `backend/` expone APIs REST para:
- perfil epidemiologico,
- ranking municipal/estadual,
- previsao de risco,
- pressao operacional,
- encaminhamento hospitalar,
- triagem,
- busca inteligente com IA.

## 2. Estrutura arquitetural (DDD + Clean)

```text
controller -> application(use cases) -> domain(policies/calculators)
                        |                     ^
                        v                     |
                 application.port -------- infrastructure.persistence
                        |
                        v
                 repository/model (JPA)
```

Tambem existe a camada `service` (compatibilidade/orquestracao) usada por parte dos controllers e adaptada para conviver com os novos use cases.

## 3. Mapa de codigo por camada

### 3.1 Bootstrap
- `VigisusApplication.java`: ponto de entrada Spring Boot.

### 3.2 Controllers (`controller/*`)
- `BrasilController.java`
- `BuscaController.java`
- `CacheController.java`
- `EncaminhamentoController.java`
- `HospitalController.java`
- `PerfilController.java`
- `PressaoOperacionalController.java`
- `PrevisaoRiscoController.java`
- `RankingController.java`
- `TriagemController.java`

Responsabilidade: adaptacao HTTP (request/response), validacao de borda e delegacao para servicos/use cases.

### 3.3 Application (use cases) (`application/*`)

#### Busca
- `application/busca/BuscaCompletaUseCase.java`

#### Encaminhamento
- `application/encaminhamento/ConsultarEncaminhamentoUseCase.java`
- `application/encaminhamento/ConsultarHospitaisCapitaisUseCase.java`
- `application/encaminhamento/ConsultarHospitaisUseCase.java`
- `application/encaminhamento/SelecionadorHospitaisProximos.java`

#### Epidemiologia
- `application/epidemiologia/ConsultarBrasilEpidemiologicoUseCase.java`
- `application/epidemiologia/ConsultarHistoricoEstadoUseCase.java`
- `application/epidemiologia/ConsultarPerfilEpidemiologicoUseCase.java`
- `application/epidemiologia/ConsultarRankingMunicipalUseCase.java`

#### Operacional
- `application/operacional/AvaliarPressaoOperacionalUseCase.java`
- `application/operacional/ConstruirContextoEpidemiologicoOperacional.java`
- `application/operacional/ConsultarProtocoloSurtoUseCase.java`
- `application/operacional/MescladorHospitaisReferencia.java`
- `application/operacional/MontadorContextoOperacional.java`
- `application/operacional/MontadorPrevisaoOperacional.java`

#### Risco
- `application/risco/ConsultarPrevisaoRiscoUseCase.java`
- `application/risco/ConsultarRiscoAgregadoUseCase.java`

#### Triagem
- `application/triagem/AvaliarTriagemUseCase.java`
- `application/triagem/ConsultarCatalogoTriagemUseCase.java`

#### Ports (abstracoes de dependencia)
- `application/port/CasoDenguePort.java`
- `application/port/ClimaPort.java`
- `application/port/MunicipioPort.java`
- `application/port/RedeAssistencialPort.java`

### 3.4 Domain (regras puras) (`domain/*`)

#### Encaminhamento
- `domain/encaminhamento/ClassificacaoPressaoSusPolicy.java`
- `domain/encaminhamento/NivelPressaoSus.java`       — Value Object imutavel
- `domain/encaminhamento/TipoLeito.java`

#### Epidemiologia
- `domain/epidemiologia/CalculadoraTendenciaEpidemiologica.java`
- `domain/epidemiologia/ClassificacaoEpidemiologicaPolicy.java`
- `domain/epidemiologia/ComparativoHistoricoEpidemiologicoPolicy.java`
- `domain/epidemiologia/IncidenciaPor100kHab.java`   — Value Object imutavel
- `domain/epidemiologia/JanelaEpidemiologicaQuatroSemanas.java`
- `domain/epidemiologia/SemanaEpidemiologica.java`

#### Geografia
- `domain/geografia/CalculadoraDistanciaGeografica.java`
- `domain/geografia/CatalogoGeograficoBrasil.java`
- `domain/geografia/CoIbge.java`

#### Operacional
- `domain/operacional/CalculadoraNivelAtencaoOperacional.java`
- `domain/operacional/CalculadoraTendenciaOperacional.java`
- `domain/operacional/ChecklistOperacionalPolicy.java`

#### Risco
- `domain/risco/CalculadoraRiscoClimatico.java`
- `domain/risco/ClassificacaoRiscoAgregadoPolicy.java`
- `domain/risco/ClassificacaoRiscoMunicipioPolicy.java`
- `domain/risco/ClassificacaoRiscoPolicy.java`
- `domain/risco/MetricasRiscoClimatico.java`
- `domain/risco/RiscoAltoDetectadoEvent.java`        — Domain Event
- `domain/risco/ScoreRisco.java`                     — Value Object imutavel

#### Triagem
- `domain/triagem/CalculadoraScoreTriagem.java`
- `domain/triagem/PriorizacaoTriagemPolicy.java`

### 3.5 Infraestrutura / Persistencia

#### Adapters (`infrastructure/persistence/*`)
- `CasoDengueJpaAdapter.java`
- `MunicipioJpaAdapter.java`
- `RedeAssistencialJpaAdapter.java`

#### Repositories (`repository/*`)
- `CasoDengueRepository.java`
- `EstabelecimentoRepository.java`
- `LeitoRepository.java`
- `MunicipioRepository.java`
- `ServicoEspecializadoRepository.java`

#### Entidades (`model/*`)
- `CasoDengue.java`
- `Estabelecimento.java`
- `Leito.java`
- `Municipio.java`
- `ServicoEspecializado.java`

### 3.6 DTOs (`dto/*`)
Principais contratos de API e integracoes:
- Busca: `BuscaRequest`, `BuscaResponse`, `BuscaCompletaResponse`.
- Epidemiologia: `PerfilEpidemiologicoResponse`, `RankingResponse`, `RankingMunicipioDTO`, `BrasilEpidemiologicoResponse`, `SemanaDTO`.
- Risco: `PrevisaoRiscoResponse`, `PrevisaoDiariaDTO`, `RiscoDiarioDTO`.
- Operacional: `PressaoOperacionalRequest`, `PressaoOperacionalResponse`, `EncaminhamentoResponse`.
- Triagem: `TriagemRequest`, `TriagemResponse`.
- Integracoes externas: `dto/openmeteo/*`, `dto/openai/*`.

### 3.7 Services (`service/*`)
Camada de orquestracao/compatibilidade:
- `BrasilEpidemiologicoService`
- `ClimaService`
- `EncaminhamentoService`
- `EstadoHistoricoService`
- `IaService`, `IaServiceImpl`, `IaServiceGeminiImpl`, `IaServiceFallback`
- `MunicipioService`
- `PerfilEpidemiologicoService`
- `PressaoOperacionalService`
- `PrevisaoRiscoService`
- `RankingService`
- `RiscoAgregadoService`
- `TextoAnaliticoHelper`
- `TriagemService`

### 3.8 Configuracoes (`config/*`)
- `AppConfig.java`
- `CacheConfig.java`
- `CorsConfig.java`
- `DataLoader.java`
- `GeminiConfig.java`
- `IaConfig.java`
- `SecurityConfig.java`

### 3.9 Excecoes (`exception/*`)
- `VigisusException.java`                — base abstrata de todas as excecoes do dominio
- `RecursoNaoEncontradoException.java`   — HTTP 404 (recurso nao encontrado)
- `MunicipioNotFoundException.java`      — HTTP 404 (especializacao para municipio)
- `DadosInsuficientesException.java`     — HTTP 422 (dados insuficientes para calculo)
- `ExternalApiException.java`            — HTTP 502 (falha em API externa: Gemini, Open-Meteo)
- `GlobalExceptionHandler.java`          — handler centralizado com ErrorResponse record

## 4. Como o backend foi construido
- Regras complexas foram isoladas em classes de dominio para facilitar teste unitario.
- Casos de uso orquestram dependencias por interfaces (`port`), reduzindo acoplamento.
- Adaptadores concretos JPA conectam as portas ao banco relacional.
- Controllers ficam finos, priorizando responsabilidade unica.
- IA foi projetada com fallback/estrategia para manter disponibilidade.

## 5. Testes e qualidade

### 5.1 Suite de testes
Cobertura de testes em:
- dominio (`src/test/.../domain/*`),
- use cases (`src/test/.../application/*`),
- controllers (`src/test/.../controller/*`),
- services/config/exception.

### 5.2 Cobertura observada
Fonte: `backend/target/site/jacoco/jacoco.csv`
- line coverage: **91.87%**
- branch coverage: **80%** (apos exclusao de codigo gerado pelo Lombok via `lombok.config`)

Nota: o branch coverage anterior de 23.16% incluia falsamente branches dos metodos
gerados pelo Lombok (equals/hashCode/toString/builder) em 60+ classes de DTOs e models.
Com `lombok.addLombokGeneratedAnnotation = true`, o JaCoCo exclui esses metodos
automaticamente, revelando a cobertura real das regras de negocio.

## 6. Pontos fortes para apresentacao
- separacao clara de camadas;
- regras de negocio testadas em unidade;
- arquitetura preparada para troca de adaptadores (DB/API externa);
- base pronta para evolucao incremental com baixo risco de regressao.
