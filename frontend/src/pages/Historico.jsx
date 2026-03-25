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
import {
  buscarMunicipio,
  buscarRankingEstado,
  buscarBrasil,
  buscarHistoricoEstado,
} from "../services/api";
import { gerarTextoNarrativoPadrao } from "../utils/iaInsights";

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
    setLoading(true);
    try {
      if (municipio) {
        const resp = await buscarMunicipio(municipio, uf, doenca, ano);
        setDados(resp);
        const ranking = await buscarRankingEstado(resp?.perfil?.uf || uf, ano, doenca);
        setRankingEstado(ranking?.ranking || []);
        return;
      }

      if ((uf || "").toUpperCase() === "BR") {
        const resp = await buscarBrasil(doenca, ano);
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
          textoIa: resp?.textoIa || gerarTextoNarrativoPadrao({
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
        buscarRankingEstado(uf, ano, doenca),
        buscarHistoricoEstado(uf, ano, doenca),
      ]);

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
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  if (anoParam >= ANO_ATUAL) {
    const params = new URLSearchParams({ uf: ufParam, doenca: doencaParam });
    if (municipioParam) params.append("municipio", municipioParam);
    return <Navigate to={`/atual?${params}`} replace />;
  }

  if (loading) {
    return (
      <>
        <TopNav />
        <div className="min-h-screen bg-slate-50 flex items-center justify-center">
          <p className="text-gray-400 text-sm animate-pulse">
            Carregando dados historicos...
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

      <div className="bg-slate-700 text-white px-6 py-4">
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
      </div>

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">
        <section>
          <SectionTitle icone="IA" titulo="Analise de IA - periodo historico" />
          <IAHistorico
            textoIa={dados?.textoIa}
            ano={perfil?.ano || anoParam}
            perfil={perfilMapped}
            ranking={rankingEstado}
          />
        </section>

        <KpisHistorico perfil={perfilMapped} />
        <KpiCards perfil={perfilMapped} modoHistorico={true} />

        <section>
          <SectionTitle icone="Dados" titulo="Resumo anual selecionado" />
          <ComparacaoAnual
            coIbge={perfil?.coIbge}
            uf={perfil?.uf}
            doenca={perfil?.doenca || doencaParam}
            escopo={municipioParam ? "municipio" : ((perfil?.uf || ufParam) === "BR" ? "brasil" : "estado")}
            anoBase={Number(perfil?.ano || anoParam)}
            totalAnoSelecionado={perfil?.total}
          />
        </section>

        <section>
          <SectionTitle icone="Curva" titulo="Evolucao dos casos" />
          <CurvaEpidemiologica perfil={perfil} />
        </section>

        <section>
          <SectionTitle icone="Mapa" titulo="Distribuicao regional" />
          <MapaEstado
            uf={perfil?.uf}
            nivel={perfil?.uf === "BR" ? "brasil" : "estado"}
            coIbgeDestaque={perfil?.coIbge}
            ranking={rankingEstado}
          />
        </section>

        {rankingEstado?.length > 0 && (
          <section>
            <SectionTitle icone="Ranking" titulo="Ranking territorial" />
            <TabelaRanking ranking={rankingEstado} />
          </section>
        )}
      </div>
    </div>
  );
}
