import React, { useEffect, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import HeaderAlerta from "../components/HeaderAlerta";
import OQueFazerAgora from "../components/OQueFazerAgora";
import KpiCards from "../components/KpiCards";
import InterpretacaoOperacional from "../components/InterpretacaoOperacional";
import StatusRapido from "../components/StatusRapido";
import CurvaEpidemiologica from "../components/CurvaEpidemiologica";
import RiscoFuturo from "../components/RiscoFuturo";
import MapaEstado from "../components/MapaEstado";
import MapaHospitais from "../components/MapaHospitais";
import ResumoIa from "../components/ResumoIa";
import { buscarPorPergunta, buscarRankingEstado, buscarSituacaoAtual } from "../services/api";

const DEFAULT_UF = "MG";
const DEFAULT_DOENCA = "dengue";

function SectionTitle({ icone, titulo }) {
  return (
    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
      {icone} {titulo}
    </p>
  );
}

function SectionSkeleton({ linhas = 3 }) {
  return (
    <div className="bg-white rounded-xl p-4 animate-pulse space-y-3">
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
    <div className="bg-gray-100 border border-gray-200 rounded-xl p-4 text-sm text-gray-400 text-center">
      Dados indisponíveis
    </div>
  );
}

function mapHospital(h) {
  return {
    nome: h.noFantasia,
    leitosSus: h.qtLeitosSus,
    telefone: h.nuTelefone,
    distanciaKm: h.distanciaKm,
    servicoInfectologia: h.servicoInfectologia,
    nuLatitude: h.nuLatitude,
    nuLongitude: h.nuLongitude,
  };
}

export default function Atual() {
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const municipio = searchParams.get("municipio");
  const uf = searchParams.get("uf");
  const doenca = searchParams.get("doenca") || DEFAULT_DOENCA;

  // Track whether the initial navigation state was already consumed, so we
  // skip a redundant fetch on first render but reload whenever params change.
  const initialStateConsumed = React.useRef(false);

  const [dados, setDados] = useState(location.state?.dados || null);
  const [loadingDados, setLoadingDados] = useState(!location.state?.dados);
  const [erroDados, setErroDados] = useState(false);
  const [rankingEstado, setRankingEstado] = useState([]);
  const [loadingRanking, setLoadingRanking] = useState(false);
  const [erroRanking, setErroRanking] = useState(false);

  // Carrega dados dos params ou via default (top cidade de MG)
  useEffect(() => {
    // Skip the first run if we already have data from navigation state
    if (!initialStateConsumed.current && location.state?.dados) {
      initialStateConsumed.current = true;
      return;
    }
    initialStateConsumed.current = true;

    const carregarDados = async () => {
      setLoadingDados(true);
      setErroDados(false);
      try {
        let pergunta;
        if (municipio && uf) {
          pergunta = `${doenca} em ${municipio} ${uf}`;
        } else {
          // Sem params: carrega MG via ranking
          const ranking = await buscarSituacaoAtual(DEFAULT_UF, 1);
          const topCidade = ranking?.ranking?.[0];
          const cidadeDefault = topCidade?.municipio || "Belo Horizonte";
          pergunta = `${DEFAULT_DOENCA} em ${cidadeDefault} ${DEFAULT_UF}`;
        }
        const r = await buscarPorPergunta(pergunta);
        setDados(r.data);
      } catch {
        setErroDados(true);
      } finally {
        setLoadingDados(false);
      }
    };

    carregarDados();
  }, [municipio, uf, doenca]); // eslint-disable-line react-hooks/exhaustive-deps

  // Carrega ranking do estado separadamente
  useEffect(() => {
    if (!dados?.perfil?.uf) return;
    setLoadingRanking(true);
    setErroRanking(false);
    buscarRankingEstado(dados.perfil.uf, dados.perfil.ano)
      .then(r => setRankingEstado(r?.ranking || []))
      .catch(() => setErroRanking(true))
      .finally(() => setLoadingRanking(false));
  }, [dados]);

  const perfil = dados?.perfil || {};

  const hoje = new Date();
  const em14dias = new Date(hoje.getTime() + 14 * 24 * 60 * 60 * 1000);
  const formatarData = (d) =>
    d.toLocaleDateString("pt-BR", { day: "2-digit", month: "short" });

  const perfilMapped = dados
    ? { ...perfil, totalCasos: perfil.total, incidencia100k: perfil.incidencia }
    : null;

  const encaminhamentoMapped = dados?.encaminhamento
    ? {
        ...dados.encaminhamento,
        hospitais: (dados.encaminhamento.hospitais || []).map(mapHospital),
      }
    : null;

  return (
    <div className="min-h-screen bg-gray-50">
      <TopNav />

      {/* Breadcrumb */}
      <div className="bg-white border-b border-gray-100 px-6 py-2 flex items-center justify-between">
        <p className="text-xs text-gray-400">
          {perfil?.municipio
            ? `${perfil.municipio} · ${perfil.uf} · ${perfil.doenca}`
            : "Carregando..."}
        </p>
        <span className="text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded-full font-medium">
          📡 Atual & Previsão
        </span>
      </div>

      {/* 1. Cabeçalho com alerta */}
      {loadingDados ? (
        <div className="px-6 py-5 bg-gray-200 animate-pulse">
          <div className="max-w-6xl mx-auto h-16 bg-gray-300 rounded" />
        </div>
      ) : erroDados ? (
        <div className="px-6 py-5">
          <SectionErro />
        </div>
      ) : (
        <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />
      )}

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">

        {/* 2. O que fazer agora — tomada de decisão com IA */}
        <OQueFazerAgora perfil={perfil} textoIa={dados?.textoIa} />
        {/* 2. Interpretação operacional — tomada de decisão */}
        <section>
          {loadingDados ? (
            <SectionSkeleton linhas={4} />
          ) : erroDados ? (
            <SectionErro />
          ) : (
            <InterpretacaoOperacional perfil={perfil} />
          )}
        </section>

        {/* 3. Status rápido — 3 métricas compactas */}
        <StatusRapido perfil={perfilMapped} risco={dados?.risco} />
        {/* 3. KPIs */}
        {loadingDados ? (
          <SectionSkeleton linhas={2} />
        ) : erroDados ? (
          <SectionErro />
        ) : (
          <KpiCards perfil={perfilMapped} risco={dados?.risco} />
        )}

        {/* 4. Curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          {loadingDados ? (
            <SectionSkeleton linhas={5} />
          ) : erroDados ? (
            <SectionErro />
          ) : (
            <CurvaEpidemiologica perfil={perfil} />
          )}
        </section>

        {/* 5. Risco futuro (14 dias) */}
        <section>
          <SectionTitle
            icone="🌡️"
            titulo={`Risco climático — ${formatarData(hoje)} a ${formatarData(em14dias)}`}
          />
          {loadingDados ? (
            <SectionSkeleton linhas={2} />
          ) : erroDados ? (
            <SectionErro />
          ) : (
            <RiscoFuturo risco={dados?.risco} />
          )}
        </section>

        {/* 6. Distribuição regional */}
        <section>
          <SectionTitle icone="🗺️" titulo="Distribuição regional" />
          {loadingRanking ? (
            <SectionSkeleton linhas={4} />
          ) : erroRanking ? (
            <SectionErro />
          ) : (
            <MapaEstado
              uf={perfil?.uf}
              coIbgeDestaque={perfil?.coIbge}
              ranking={rankingEstado}
            />
          )}
        </section>

        {/* 7. Infraestrutura / encaminhamento */}
        <section>
          <SectionTitle icone="🏥" titulo="Infraestrutura disponível" />
          {loadingDados ? (
            <SectionSkeleton linhas={3} />
          ) : erroDados ? (
            <SectionErro />
          ) : (
            <MapaHospitais
              perfil={perfilMapped}
              encaminhamento={encaminhamentoMapped}
            />
          )}
        </section>

        {/* 8. Resumo IA */}
        <section>
          <SectionTitle icone="📋" titulo="Resumo operacional" />
          {loadingDados ? (
            <SectionSkeleton linhas={4} />
          ) : erroDados ? (
            <SectionErro />
          ) : (
            <ResumoIa textoIa={dados?.textoIa} perfil={perfilMapped} />
          )}
        </section>

      </div>
    </div>
  );
}
