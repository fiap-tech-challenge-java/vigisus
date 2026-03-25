import { Suspense, lazy, useEffect, useRef, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import KpiCards from "../components/KpiCards";
import KpisHistorico from "../components/KpisHistorico";
import {
  buscarMunicipio,
  buscarRankingEstado,
  buscarBrasil,
  buscarHistoricoEstado,
} from "../services/api";
import { gerarTextoNarrativoPadrao } from "../utils/iaInsights";

const ComparacaoAnual = lazy(() => import("../components/ComparacaoAnual"));
const CurvaEpidemiologica = lazy(() => import("../components/CurvaEpidemiologica"));
const MapaEstado = lazy(() => import("../components/MapaEstado"));
const TabelaRanking = lazy(() => import("../components/TabelaRanking"));
const IAHistorico = lazy(() => import("../components/IAHistorico"));

function SectionTitle({ icone, titulo }) {
  return (
    <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
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

function foiRequisicaoCancelada(error) {
  return (
    error?.name === "AbortError" ||
    error?.name === "CanceledError" ||
    error?.code === "ERR_CANCELED"
  );
}

const ANO_ATUAL = new Date().getFullYear();
const ANO_HISTORICO_PADRAO = ANO_ATUAL - 1;

export default function Historico() {
  const [searchParams, setSearchParams] = useSearchParams();

  const municipioParam = searchParams.get("municipio");
  const ufParam = searchParams.get("uf") || "BR";
  const anoParam = Number(searchParams.get("ano")) || ANO_HISTORICO_PADRAO;
  const doencaParam = searchParams.get("doenca") || "dengue";

  const [dados, setDados] = useState(null);
  const [rankingEstado, setRankingEstado] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);

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
    if (!searchParams.get("ano")) {
      const params = new URLSearchParams(searchParams);
      params.set("ano", String(ANO_HISTORICO_PADRAO));
      setSearchParams(params, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  useEffect(() => {
    carregar(municipioParam, ufParam, doencaParam, anoParam);
  }, [municipioParam, ufParam, doencaParam, anoParam]); // eslint-disable-line react-hooks/exhaustive-deps

  const carregar = async (municipio, uf, doenca, ano) => {
    const { id, signal } = iniciarRequisicao();

    try {
      if (municipio) {
        const resp = await buscarMunicipio(municipio, uf, doenca, ano, signal);
        if (!ehRequisicaoAtual(id)) {
          return;
        }

        const ranking = await buscarRankingEstado(resp?.perfil?.uf || uf, ano, doenca, signal);
        if (!ehRequisicaoAtual(id)) {
          return;
        }

        setDados(resp);
        setRankingEstado(ranking?.ranking || []);
        return;
      }

      if ((uf || "").toUpperCase() === "BR") {
        const resp = await buscarBrasil(doenca, ano, signal);
        if (!ehRequisicaoAtual(id)) {
          return;
        }

        const perfilBrasil = {
          coIbge: "00",
          municipio: "Brasil",
          uf: "BR",
          doenca,
          ano,
          total: resp?.totalCasos || 0,
          incidencia: resp?.incidencia || 0,
          classificacao: resp?.classificacao || "SEM_DADO",
          tendencia: resp?.tendencia || "ESTAVEL",
          semanas: resp?.semanas || [],
          semanasAnoAnterior: resp?.semanasAnoAnterior || [],
        };

        setDados({
          perfil: perfilBrasil,
          textoIa:
            resp?.textoIa ||
            gerarTextoNarrativoPadrao({
              perfil: {
                ...perfilBrasil,
                totalCasos: perfilBrasil.total,
                incidencia100k: perfilBrasil.incidencia,
              },
              ranking: resp?.estadosPiores || [],
            }),
        });
        setRankingEstado(resp?.estadosPiores || []);
        return;
      }

      const [ranking, perfilEstado] = await Promise.all([
        buscarRankingEstado(uf, ano, doenca, signal),
        buscarHistoricoEstado(uf, ano, doenca, signal),
      ]);
      if (!ehRequisicaoAtual(id)) {
        return;
      }

      const lista = ranking?.ranking || [];
      const perfilLocal = {
        coIbge: uf,
        municipio: `Estado ${uf}`,
        uf,
        doenca,
        ano,
        total: perfilEstado?.total || 0,
        incidencia: perfilEstado?.incidencia || 0,
        classificacao: perfilEstado?.classificacao || "SEM_DADO",
        tendencia: perfilEstado?.tendencia || "ESTAVEL",
        semanas: perfilEstado?.semanas || [],
        semanasAnoAnterior: perfilEstado?.semanasAnoAnterior || [],
      };

      setDados({
        perfil: perfilLocal,
        textoIa: gerarTextoNarrativoPadrao({
          perfil: {
            ...perfilLocal,
            totalCasos: perfilLocal.total,
            incidencia100k: perfilLocal.incidencia,
          },
          ranking: lista,
        }),
      });
      setRankingEstado(lista);
    } catch (error) {
      if (!foiRequisicaoCancelada(error) && ehRequisicaoAtual(id)) {
        setErro("Nao foi possivel carregar os dados historicos.");
      }
    } finally {
      finalizarRequisicao(id);
    }
  };

  if (anoParam >= ANO_ATUAL) {
    const params = new URLSearchParams({ uf: ufParam, doenca: doencaParam });
    if (municipioParam) {
      params.append("municipio", municipioParam);
    }
    return <Navigate to={`/atual?${params}`} replace />;
  }

  if (loading) {
    return (
      <div className="min-h-screen vigi-page">
        <TopNav loading />
        <main id="main-content" tabIndex={-1} className="min-h-screen flex items-center justify-center">
          <p className="text-gray-400 text-sm animate-pulse" aria-live="polite">
            Carregando dados historicos...
          </p>
        </main>
      </div>
    );
  }

  if (erro) {
    return (
      <div className="min-h-screen vigi-page">
        <TopNav />
        <main id="main-content" tabIndex={-1} className="max-w-4xl mx-auto px-6 py-10">
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600" role="alert">
            {erro}
          </div>
        </main>
      </div>
    );
  }

  if (!dados) {
    return null;
  }

  const perfil = dados.perfil || {};
  const perfilMapped = {
    ...perfil,
    totalCasos: perfil.total,
    incidencia100k: perfil.incidencia,
  };

  return (
    <div className="min-h-screen vigi-page">
      <TopNav loading={loading} />
      <main id="main-content" tabIndex={-1}>
        <header className="bg-slate-700 text-white px-6 py-4">
          <div className="max-w-6xl mx-auto flex items-center justify-between">
            <div>
              <p className="text-xs text-slate-300 uppercase tracking-wider mb-0.5">
                Modo Historico
              </p>
              <h1 className="text-lg font-bold">
                {perfil?.municipio} - {perfil?.uf}
              </h1>
              <p className="text-sm text-slate-300">
                {perfil?.doenca?.charAt(0).toUpperCase() + perfil?.doenca?.slice(1)} - {perfil?.ano || anoParam}
              </p>
            </div>
            <div className="text-right hidden sm:block">
              <p className="text-xs text-slate-400">Total de casos</p>
              <p className="text-2xl font-black">
                {perfil?.total?.toLocaleString("pt-BR") ?? "-"}
              </p>
              <p className="text-xs text-slate-300">{perfil?.classificacao}</p>
            </div>
          </div>
        </header>

        <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">
          <section aria-label="Analise historica por IA">
            <div className="mb-4 border-l-4 border-blue-300 pl-3">
              <SectionTitle icone="IA" titulo="Analise de IA - periodo historico" />
            </div>
            <Suspense fallback={<SectionSkeleton linhas={4} />}>
              <IAHistorico
                textoIa={dados?.textoIa}
                ano={perfil?.ano || anoParam}
                perfil={perfilMapped}
                ranking={rankingEstado}
              />
            </Suspense>
          </section>

          <KpisHistorico perfil={perfilMapped} />
          <KpiCards perfil={perfilMapped} modoHistorico />

          <section aria-label="Comparacao anual">
            <div className="mb-4 border-l-4 border-blue-300 pl-3">
              <SectionTitle icone="Dados" titulo="Resumo anual selecionado" />
            </div>
            <Suspense fallback={<SectionSkeleton linhas={4} />}>
              <ComparacaoAnual
                coIbge={perfil?.coIbge}
                uf={perfil?.uf}
                doenca={perfil?.doenca || doencaParam}
                escopo={municipioParam ? "municipio" : ((perfil?.uf || ufParam) === "BR" ? "brasil" : "estado")}
                anoBase={Number(perfil?.ano || anoParam)}
                totalAnoSelecionado={perfil?.total}
              />
            </Suspense>
          </section>

          <section aria-label="Curva de casos">
            <div className="mb-4 border-l-4 border-blue-300 pl-3">
              <SectionTitle icone="Curva" titulo="Evolucao dos casos" />
            </div>
            <Suspense fallback={<SectionSkeleton linhas={5} />}>
              <CurvaEpidemiologica perfil={perfil} />
            </Suspense>
          </section>

          <section aria-label="Mapa regional">
            <div className="mb-4 border-l-4 border-blue-300 pl-3">
              <SectionTitle icone="Mapa" titulo="Distribuicao regional" />
            </div>
            <Suspense fallback={<SectionSkeleton linhas={4} />}>
              <MapaEstado
                uf={perfil?.uf}
                nivel={perfil?.uf === "BR" ? "brasil" : "estado"}
                coIbgeDestaque={perfil?.coIbge}
                ranking={rankingEstado}
              />
            </Suspense>
          </section>

          {rankingEstado?.length > 0 && (
            <section aria-label="Ranking territorial">
              <div className="mb-4 border-l-4 border-blue-300 pl-3">
                <SectionTitle icone="Ranking" titulo="Ranking territorial" />
              </div>
              <Suspense fallback={<SectionSkeleton linhas={6} />}>
                <TabelaRanking ranking={rankingEstado} />
              </Suspense>
            </section>
          )}
        </div>
      </main>
    </div>
  );
}

