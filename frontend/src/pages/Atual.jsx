import { Suspense, lazy, useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import HeaderAlerta from "../components/HeaderAlerta";
import OQueFazerAgora from "../components/OQueFazerAgora";
import KpiCards from "../components/KpiCards";
import StatusRapido from "../components/StatusRapido";
import {
  buscarMunicipio,
  buscarRankingEstado,
  buscarHistoricoEstado,
  buscarBrasil,
  buscarHospitaisBrasilAgregado,
  buscarHospitaisEstadoRegiao,
  buscarRiscoBrasil,
  buscarRiscoEstado,
} from "../services/api";

const CurvaEpidemiologica = lazy(() => import("../components/CurvaEpidemiologica"));
const RiscoFuturo = lazy(() => import("../components/RiscoFuturo"));
const MapaEstado = lazy(() => import("../components/MapaEstado"));
const MapaHospitais = lazy(() => import("../components/MapaHospitais"));
const ResumoIa = lazy(() => import("../components/ResumoIa"));

const ANO_ATUAL = new Date().getFullYear();

function SectionTitle({ icone, titulo }) {
  return (
    <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
      {icone} {titulo}
    </h2>
  );
}

function SectionSkeleton({ linhas = 3 }) {
  return (
    <div className="bg-white rounded-xl p-4 animate-pulse space-y-3" role="status" aria-live="polite">
      <span className="sr-only">Carregando secao</span>
      {Array.from({ length: linhas }).map((_, i) => (
        <div
          key={i}
          className="h-4 bg-gray-200 rounded"
          style={{ width: `${70 + (i % 3) * 10}%` }}
        />
      ))}
    </div>
  );
}

function SectionErro() {
  return (
    <div className="bg-gray-100 border border-gray-200 rounded-xl p-4 text-sm text-gray-400 text-center" role="alert">
      Dados indisponiveis
    </div>
  );
}

function mapHospital(hospital) {
  return {
    nome: hospital.noFantasia || hospital.nome || "Hospital sem nome",
    leitosSus: hospital.qtLeitosSus ?? hospital.leitosSus ?? null,
    telefone: hospital.nuTelefone || hospital.telefone || null,
    distanciaKm: hospital.distanciaKm ?? null,
    servicoInfectologia: hospital.servicoInfectologia ?? false,
    nuLatitude: hospital.nuLatitude ?? null,
    nuLongitude: hospital.nuLongitude ?? null,
  };
}

function foiRequisicaoCancelada(error) {
  return (
    error?.name === "AbortError" ||
    error?.name === "CanceledError" ||
    error?.code === "ERR_CANCELED"
  );
}

export default function Atual() {
  const [searchParams, setSearchParams] = useSearchParams();

  const hasUrlParams = !!searchParams.get("municipio") || !!searchParams.get("uf");
  const municipioParam = searchParams.get("municipio");
  const ufParam = searchParams.get("uf") || "MG";
  const doencaParam = searchParams.get("doenca") || "dengue";

  const [dados, setDados] = useState(null);
  const [rankingEstado, setRankingEstado] = useState([]);
  const [rankingLoading, setRankingLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState(null);
  const [geoState, setGeoState] = useState(hasUrlParams ? "city" : "detecting");

  const requestRef = useRef({ id: 0, controller: null });

  const iniciarRequisicao = () => {
    requestRef.current.controller?.abort();

    const controller = new AbortController();
    const id = requestRef.current.id + 1;
    requestRef.current = { id, controller };

    setLoading(true);
    setErro(null);
    setDados(null);
    setRankingEstado([]);
    setRankingLoading(false);

    return { id, signal: controller.signal };
  };

  const ehRequisicaoAtual = (id) => requestRef.current.id === id;

  const finalizarRequisicao = (id) => {
    if (ehRequisicaoAtual(id)) {
      setLoading(false);
    }
  };

  useEffect(() => () => requestRef.current.controller?.abort(), []);

  useEffect(() => {
    if (hasUrlParams) {
      setGeoState("city");
      return;
    }

    if (geoState === "city") {
      setGeoState("detecting");
    }
  }, [hasUrlParams, geoState]);

  useEffect(() => {
    if (hasUrlParams) {
      return;
    }

    if (!navigator.geolocation) {
      setGeoState("brasil");
      return;
    }

    let ignorar = false;
    const timer = setTimeout(() => setGeoState("brasil"), 5000);

    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        if (ignorar) {
          return;
        }
        clearTimeout(timer);

        try {
          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${coords.latitude}&lon=${coords.longitude}&format=json`
          );
          const data = await response.json();
          const city = data.address?.city || data.address?.town || data.address?.municipality;
          const uf = (data.address?.["ISO3166-2-lvl4"] || "").replace("BR-", "");

          if (city && uf) {
            setSearchParams({ municipio: city, uf, doenca: "dengue" }, { replace: true });
            setGeoState("city");
          } else {
            setGeoState("brasil");
          }
        } catch {
          setGeoState("brasil");
        }
      },
      () => {
        if (ignorar) {
          return;
        }
        clearTimeout(timer);
        setGeoState("brasil");
      },
      { timeout: 4500, maximumAge: 60_000 }
    );

    return () => {
      ignorar = true;
      clearTimeout(timer);
    };
  }, [hasUrlParams, setSearchParams]);

  useEffect(() => {
    if (hasUrlParams) {
      if (municipioParam) {
        carregarMunicipio(municipioParam, ufParam, doencaParam);
        return;
      }
      if (ufParam === "BR") {
        carregarBrasil(doencaParam, true);
        return;
      }
      if (ufParam) {
        carregarEstado(ufParam, doencaParam);
      }
      return;
    }

    carregarBrasil(doencaParam, false);
  }, [municipioParam, ufParam, doencaParam, hasUrlParams]); // eslint-disable-line react-hooks/exhaustive-deps

  const carregarMunicipio = async (municipio, uf, doenca) => {
    const { id, signal } = iniciarRequisicao();
    let requisicaoFinalizada = false;
    const finalizarUmaVez = () => {
      if (!requisicaoFinalizada) {
        requisicaoFinalizada = true;
        finalizarRequisicao(id);
      }
    };

    try {
      const resp = await buscarMunicipio(municipio, uf, doenca, ANO_ATUAL, signal);
      if (!ehRequisicaoAtual(id)) {
        return;
      }

      setRankingLoading(true);
      setDados(resp);
      finalizarUmaVez();

      const ufFinal = resp?.perfil?.uf || uf;
      try {
        const ranking = await buscarRankingEstado(ufFinal, ANO_ATUAL, doenca, signal);
        if (ehRequisicaoAtual(id)) {
          setRankingEstado(ranking?.ranking || []);
        }
      } catch (error) {
        if (!foiRequisicaoCancelada(error) && ehRequisicaoAtual(id)) {
          setRankingEstado([]);
        }
      } finally {
        if (ehRequisicaoAtual(id)) {
          setRankingLoading(false);
        }
      }
    } catch (error) {
      if (!foiRequisicaoCancelada(error) && ehRequisicaoAtual(id)) {
        setErro("Municipio nao encontrado. Verifique o nome e o estado.");
      }
    } finally {
      finalizarUmaVez();
    }
  };

  const carregarBrasil = async (doenca, sincronizarUrl = true) => {
    const { id, signal } = iniciarRequisicao();
    let requisicaoFinalizada = false;
    const finalizarUmaVez = () => {
      if (!requisicaoFinalizada) {
        requisicaoFinalizada = true;
        finalizarRequisicao(id);
      }
    };

    try {
      if (sincronizarUrl && searchParams.get("uf") !== "BR") {
        setSearchParams({ uf: "BR", doenca }, { replace: true });
      }

      const resp = await buscarBrasil(doenca, ANO_ATUAL, signal);
      if (!ehRequisicaoAtual(id)) {
        return;
      }

      setDados({
        perfil: {
          municipio: "Brasil",
          uf: "BR",
          total: resp.totalCasos,
          incidencia: resp.incidencia,
          classificacao: resp.classificacao,
          tendencia: resp.tendencia,
          semanas: resp.semanas,
          semanasAnoAnterior: resp.semanasAnoAnterior,
          doenca,
          ano: ANO_ATUAL,
        },
        textoIa: resp.textoIa,
        risco: null,
        encaminhamento: { hospitais: [] },
        estadosPiores: resp.estadosPiores,
        municipiosPiores: resp.municipiosPiores,
      });
      setRankingEstado(resp.estadosPiores || []);
      finalizarUmaVez();

      const [riscoResult, hospitaisResult] = await Promise.allSettled([
        buscarRiscoBrasil(signal),
        buscarHospitaisBrasilAgregado(signal),
      ]);

      if (!ehRequisicaoAtual(id)) {
        return;
      }

      const riscoAtualizado = riscoResult.status === "fulfilled" ? riscoResult.value : null;
      const hospitaisAtualizados = hospitaisResult.status === "fulfilled" ? hospitaisResult.value : [];

      setDados((atual) => {
        if (!atual) {
          return atual;
        }

        return {
          ...atual,
          risco: riscoAtualizado,
          encaminhamento: { hospitais: (hospitaisAtualizados || []).map(mapHospital) },
        };
      });
    } catch (error) {
      if (!foiRequisicaoCancelada(error) && ehRequisicaoAtual(id)) {
        setErro("Erro ao carregar dados do Brasil.");
      }
    } finally {
      finalizarUmaVez();
    }
  };

  const carregarEstado = async (uf, doenca) => {
    const { id, signal } = iniciarRequisicao();
    let requisicaoFinalizada = false;
    const finalizarUmaVez = () => {
      if (!requisicaoFinalizada) {
        requisicaoFinalizada = true;
        finalizarRequisicao(id);
      }
    };

    try {
      const [perfilEstado, hospitais, riscoAg] = await Promise.all([
        buscarHistoricoEstado(uf, ANO_ATUAL, doenca, signal),
        buscarHospitaisEstadoRegiao(uf, signal),
        buscarRiscoEstado(uf, signal),
      ]);
      if (!ehRequisicaoAtual(id)) {
        return;
      }

      setDados({
        perfil: {
          municipio: perfilEstado?.municipio || uf,
          uf,
          total: perfilEstado?.total || 0,
          incidencia: perfilEstado?.incidencia || 0,
          classificacao: perfilEstado?.classificacao || "SEM_DADO",
          tendencia: perfilEstado?.tendencia || "ESTAVEL",
          semanas: perfilEstado?.semanas || [],
          semanasAnoAnterior: perfilEstado?.semanasAnoAnterior || [],
          doenca,
          ano: ANO_ATUAL,
        },
        textoIa:
          perfilEstado?.textoIa ||
          `Estado ${uf} registrou ${perfilEstado?.total || 0} casos de ${doenca} em ${ANO_ATUAL}.`,
        risco: riscoAg,
        encaminhamento: { hospitais: (hospitais || []).map(mapHospital) },
      });
      setRankingLoading(true);
      finalizarUmaVez();

      try {
        const ranking = await buscarRankingEstado(uf, ANO_ATUAL, doenca, signal);
        if (ehRequisicaoAtual(id)) {
          setRankingEstado(ranking?.ranking || []);
        }
      } catch (error) {
        if (!foiRequisicaoCancelada(error) && ehRequisicaoAtual(id)) {
          setRankingEstado([]);
        }
      } finally {
        if (ehRequisicaoAtual(id)) {
          setRankingLoading(false);
        }
      }
    } catch (error) {
      if (!foiRequisicaoCancelada(error) && ehRequisicaoAtual(id)) {
        setErro("Estado nao encontrado. Verifique.");
      }
    } finally {
      finalizarUmaVez();
    }
  };

  const perfil = dados?.perfil || {};
  const hoje = new Date();
  const em14dias = new Date(hoje.getTime() + 14 * 24 * 60 * 60 * 1000);
  const formatarData = (data) =>
    data.toLocaleDateString("pt-BR", { day: "2-digit", month: "short" });

  const perfilMapped = dados
    ? { ...perfil, totalCasos: perfil.total, incidencia100k: perfil.incidencia }
    : null;

  const encaminhamentoMapped = dados?.encaminhamento
    ? {
        ...dados.encaminhamento,
        hospitais: (dados.encaminhamento.hospitais || []).map(mapHospital),
      }
    : null;

  const renderizarSkeletonOuErro = (linhas = 3) => {
    if (loading || !dados) {
      return <SectionSkeleton linhas={linhas} />;
    }
    if (erro) {
      return <SectionErro />;
    }
    return null;
  };

  return (
    <div className="min-h-screen vigi-page">
      <TopNav loading={loading} />
      <main id="main-content" tabIndex={-1}>
        {erro && (
          <div className="max-w-4xl mx-auto px-4 py-4" aria-live="assertive">
            <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600" role="alert">
              {erro}
            </div>
          </div>
        )}

        <div className="bg-white border-b border-gray-100 px-6 py-2 flex items-center justify-between">
          <p className="text-xs text-gray-400" aria-live="polite">
            {perfil?.municipio
              ? `${perfil.municipio} - ${perfil.uf} - ${perfil.doenca}`
              : "Carregando..."}
          </p>
          <span className="text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded-full font-medium">
            Atual e Previsao
          </span>
        </div>

        {!hasUrlParams && geoState === "detecting" && (
          <div className="max-w-6xl mx-auto px-6 pt-3" aria-live="polite">
            <p className="text-xs text-gray-500">
              Detectando sua localizacao em segundo plano. Exibindo Brasil enquanto isso.
            </p>
          </div>
        )}

        {(() => {
          const fallback = renderizarSkeletonOuErro(2);
          if (fallback) {
            return <div className="px-6 py-5">{fallback}</div>;
          }
          return <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />;
        })()}

        <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">
          {(() => {
            const fallback = renderizarSkeletonOuErro(5);
            if (fallback) {
              return fallback;
            }
            return (
              <OQueFazerAgora
                perfil={perfilMapped}
                textoIa={dados?.textoIa}
                ranking={rankingEstado}
                risco={dados?.risco}
              />
            );
          })()}

          {(() => {
            const fallback = renderizarSkeletonOuErro(2);
            if (fallback) {
              return fallback;
            }
            return <StatusRapido perfil={perfilMapped} risco={dados?.risco} />;
          })()}

          {(() => {
            const fallback = renderizarSkeletonOuErro(2);
            if (fallback) {
              return fallback;
            }
            return <KpiCards perfil={perfilMapped} risco={dados?.risco} />;
          })()}

          <section aria-label="Evolucao de casos">
            <SectionTitle icone="Curva" titulo="Evolucao dos casos" />
            {(() => {
              const fallback = renderizarSkeletonOuErro(5);
              if (fallback) {
                return fallback;
              }
              return (
                <Suspense fallback={<SectionSkeleton linhas={5} />}>
                  <CurvaEpidemiologica perfil={perfil} />
                </Suspense>
              );
            })()}
          </section>

          <section aria-label="Risco climatico">
            <SectionTitle
              icone="Clima"
              titulo={`Risco climatico - ${formatarData(hoje)} a ${formatarData(em14dias)}`}
            />
            {(() => {
              const fallback = renderizarSkeletonOuErro(3);
              if (fallback) {
                return fallback;
              }
              return (
                <Suspense fallback={<SectionSkeleton linhas={3} />}>
                  <RiscoFuturo risco={dados?.risco} />
                </Suspense>
              );
            })()}
          </section>

          <section aria-label="Mapa regional">
            <SectionTitle icone="Mapa" titulo="Distribuicao regional" />
            {(() => {
              if (rankingLoading && !rankingEstado.length) {
                return <SectionSkeleton linhas={4} />;
              }
              const fallback = renderizarSkeletonOuErro(4);
              if (fallback) {
                return fallback;
              }
              return (
                <Suspense fallback={<SectionSkeleton linhas={4} />}>
                  <MapaEstado
                    uf={perfil?.uf}
                    nivel={perfil?.uf === "BR" ? "brasil" : "estado"}
                    coIbgeDestaque={perfil?.coIbge}
                    ranking={rankingEstado}
                    risco={dados?.risco}
                  />
                </Suspense>
              );
            })()}
          </section>

          <section aria-label="Mapa e lista de hospitais">
            <SectionTitle icone="Hospitais" titulo="Infraestrutura disponivel" />
            {(() => {
              const fallback = renderizarSkeletonOuErro(3);
              if (fallback) {
                return fallback;
              }
              return (
                <Suspense fallback={<SectionSkeleton linhas={3} />}>
                  <MapaHospitais perfil={perfilMapped} encaminhamento={encaminhamentoMapped} />
                </Suspense>
              );
            })()}
          </section>

          <section aria-label="Resumo operacional">
            <SectionTitle icone="Resumo" titulo="Resumo operacional" />
            {(() => {
              const fallback = renderizarSkeletonOuErro(4);
              if (fallback) {
                return fallback;
              }
              return (
                <Suspense fallback={<SectionSkeleton linhas={4} />}>
                  <ResumoIa
                    textoIa={dados?.textoIa}
                    perfil={perfilMapped}
                    ranking={rankingEstado}
                    risco={dados?.risco}
                  />
                </Suspense>
              );
            })()}
          </section>
        </div>
      </main>
    </div>
  );
}
