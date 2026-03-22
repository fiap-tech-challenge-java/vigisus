import { useEffect, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import KpiCards from "../components/KpiCards";
import KpisHistorico from "../components/KpisHistorico";
import ComparacaoAnual from "../components/ComparacaoAnual";
import CurvaEpidemiologica from "../components/CurvaEpidemiologica";
import MapaEstado from "../components/MapaEstado";
import TabelaRanking from "../components/TabelaRanking";
import IAHistorico from "../components/IAHistorico";
import { buscarMunicipio, buscarRankingEstado } from "../services/api";

// Historico NUNCA mostra: InterpretacaoOperacional, MapaHospitais,
// RiscoFuturo — apenas dados do período selecionado.

function SectionTitle({ icone, titulo }) {
  return (
    <div className="flex items-center gap-3 mb-4 border-l-4 border-blue-300 pl-3">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
        {icone} {titulo}
      </p>
    </div>
  );
}


const ANO_ATUAL = new Date().getFullYear();

export default function Historico() {
  const [searchParams, setSearchParams] = useSearchParams();

  const municipioParam = searchParams.get("municipio");
  const ufParam        = searchParams.get("uf")     || "MG";
  const anoParam       = Number(searchParams.get("ano")) || 2024;
  const doencaParam    = searchParams.get("doenca") || "dengue";

  const [dados, setDados]                 = useState(null);
  const [rankingEstado, setRankingEstado] = useState([]);
  const [loading, setLoading]             = useState(true);

  useEffect(() => {
    carregar(municipioParam, ufParam, doencaParam, anoParam);
  }, [municipioParam, ufParam, doencaParam, anoParam]); // eslint-disable-line react-hooks/exhaustive-deps

  const carregar = async (municipio, uf, doenca, ano) => {
    setLoading(true);
    try {
      const resp = municipio
        ? await buscarMunicipio(municipio, uf, doenca, ano)
        : await buscarMunicipio("Belo Horizonte", uf, doenca, ano);
      setDados(resp);
      const ranking = await buscarRankingEstado(uf, doenca, ano);
      setRankingEstado(ranking?.ranking || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  // Ano recente → redireciona para /atual (após todos os hooks)
  if (anoParam >= ANO_ATUAL - 1) {
    const p = new URLSearchParams({ uf: ufParam, doenca: doencaParam });
    if (municipioParam) p.append("municipio", municipioParam);
    return <Navigate to={`/atual?${p}`} replace />;
  }

  if (loading) {
    return (
      <>
        <TopNav />
        <div className="min-h-screen bg-slate-50 flex items-center justify-center">
          <p className="text-gray-400 text-sm animate-pulse">
            ⏳ Carregando dados históricos...
          </p>
        </div>
      </>
    );
  }

  if (!dados) return null;

  const perfil = dados.perfil || {};

  const perfilMapped = {
    ...perfil,
    totalCasos: perfil.total,
    incidencia100k: perfil.incidencia,
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <TopNav />

      {/* Cabeçalho histórico — fundo neutro slate (não vermelho) */}
      <div className="bg-slate-700 text-white px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div>
            <p className="text-xs text-slate-300 uppercase tracking-wider mb-0.5">
              📚 Modo Histórico
            </p>
            <h1 className="text-lg font-bold">
              {perfil?.municipio} · {perfil?.uf}
            </h1>
            <p className="text-sm text-slate-300">
              {perfil?.doenca?.charAt(0).toUpperCase() + perfil?.doenca?.slice(1)} — {perfil?.ano || anoParam}
            </p>
          </div>
          <div className="text-right hidden sm:block">
            <p className="text-xs text-slate-400">Total de casos</p>
            <p className="text-2xl font-black">
              {perfil?.total?.toLocaleString("pt-BR") ?? "—"}
            </p>
            <p className="text-xs text-slate-300">{perfil?.classificacao}</p>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">

        {/* 1. Análise da IA — ao topo */}
        <section>
          <SectionTitle icone="🤖" titulo="Análise de IA — período histórico" />
          <IAHistorico textoIa={dados?.textoIa} ano={perfil?.ano || anoParam} />
        </section>

        {/* 2. KPIs históricos grandes */}
        <KpisHistorico perfil={perfilMapped} />

        {/* 3. KPIs secundários — incidência, classificação */}
        <KpiCards perfil={perfilMapped} modoHistorico={true} />

        {/* 4. Comparação interanual — barras Chart.js */}
        <section>
          <SectionTitle icone="📊" titulo="Comparação anual (últimos 5 anos)" />
          <ComparacaoAnual
            coIbge={perfil?.coIbge}
            anoBase={Number(perfil?.ano || anoParam)}
          />
        </section>

        {/* 5. Curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          <CurvaEpidemiologica perfil={perfil} />
        </section>

        {/* 6. Distribuição regional */}
        <section>
          <SectionTitle icone="🗺️" titulo="Distribuição regional" />
          <MapaEstado
            uf={perfil?.uf}
            coIbgeDestaque={perfil?.coIbge}
            ranking={rankingEstado}
          />
        </section>

        {/* 7. Ranking de municípios do estado */}
        {rankingEstado?.length > 0 && (
          <section>
            <SectionTitle icone="🏆" titulo="Ranking de municípios — situação do estado" />
            <TabelaRanking ranking={rankingEstado} />
          </section>
        )}

      </div>
    </div>
  );
}
