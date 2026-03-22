import React, { useEffect, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import HeaderAlerta from "../components/HeaderAlerta";
import KpiCards from "../components/KpiCards";
import CurvaEpidemiologica from "../components/CurvaEpidemiologica";
import MapaEstado from "../components/MapaEstado";
import ResumoIa from "../components/ResumoIa";
import { buscarPorPergunta, buscarRankingEstado } from "../services/api";

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

function ComparacaoAnual({ perfil }) {
  if (!perfil?.semanasAnoAnterior?.length) return null;

  const totalAtual = perfil.totalCasos || 0;
  const totalAnterior = perfil.semanasAnoAnterior.reduce((s, x) => s + x.casos, 0);
  const variacao = totalAnterior > 0
    ? ((totalAtual - totalAnterior) / totalAnterior * 100).toFixed(1)
    : null;

  const picoAtual = perfil.semanas?.length
    ? perfil.semanas.reduce((prev, curr) => (curr.casos > prev.casos ? curr : prev), perfil.semanas[0])
    : null;
  const picoAnterior = perfil.semanasAnoAnterior.reduce(
    (prev, curr) => (curr.casos > prev.casos ? curr : prev),
    perfil.semanasAnoAnterior[0]
  );

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 mx-6 max-w-6xl md:mx-auto">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">
        📊 Comparação interanual
      </p>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="text-center">
          <p className="text-xs text-slate-600 font-medium mb-1">{perfil.ano}</p>
          <p className="text-2xl font-black text-slate-700">
            {totalAtual.toLocaleString("pt-BR")}
          </p>
          <p className="text-xs text-slate-500">casos totais</p>
        </div>
        <div className="text-center">
          <p className="text-xs text-slate-600 font-medium mb-1">{perfil.ano - 1}</p>
          <p className="text-2xl font-black text-slate-400">
            {totalAnterior.toLocaleString("pt-BR")}
          </p>
          <p className="text-xs text-slate-500">casos totais</p>
        </div>
        <div className="text-center">
          <p className="text-xs text-slate-600 font-medium mb-1">Variação</p>
          <p className={`text-2xl font-black ${
            variacao === null ? "text-slate-400"
            : Number(variacao) > 0 ? "text-red-500" : "text-green-600"
          }`}>
            {variacao !== null
              ? `${Number(variacao) > 0 ? "+" : ""}${variacao}%`
              : "—"}
          </p>
          <p className="text-xs text-slate-500">ano a ano</p>
        </div>
        <div className="text-center">
          <p className="text-xs text-slate-600 font-medium mb-1">Semanas de pico</p>
          <p className="text-sm font-bold text-slate-600">
            {picoAtual ? `Sem. ${picoAtual.semanaEpi}` : "—"}
            <span className="text-slate-300 mx-1">/</span>
            <span className="text-slate-400">Sem. {picoAnterior.semanaEpi}</span>
          </p>
          <p className="text-xs text-slate-500">{perfil.ano} / {perfil.ano - 1}</p>
        </div>
      </div>
    </div>
  );
}

export default function Historico() {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const municipio = searchParams.get("municipio");
  const uf = searchParams.get("uf");
  const ano = searchParams.get("ano");
  const doenca = searchParams.get("doenca") || "dengue";

  const [dados, setDados] = useState(location.state?.dados || null);
  const [loading, setLoading] = useState(!location.state?.dados);
  const [erro, setErro] = useState("");
  const [rankingEstado, setRankingEstado] = useState([]);

  useEffect(() => {
    if (!municipio || !uf) {
      navigate("/");
      return;
    }
    if (!dados) {
      const pergunta = `${doenca} em ${municipio} ${uf}${ano ? ` ${ano}` : ""}`;
      setLoading(true);
      buscarPorPergunta(pergunta)
        .then(r => setDados(r.data))
        .catch(() => setErro("Erro ao buscar dados. Tente novamente."))
        .finally(() => setLoading(false));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [municipio, uf, ano, doenca]);

  useEffect(() => {
    if (dados?.perfil?.uf) {
      buscarRankingEstado(dados.perfil.uf, dados.perfil.ano)
        .then(r => setRankingEstado(r?.ranking || []))
        .catch(() => {});
    }
  }, [dados]);

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

  if (erro) {
    return (
      <>
        <TopNav />
        <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center gap-3">
          <p className="text-red-500 text-sm">{erro}</p>
          <button
            onClick={() => navigate("/")}
            className="text-xs text-gray-500 hover:text-red-500 transition"
          >
            ← Voltar à busca
          </button>
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

      {/* Breadcrumb */}
      <div className="bg-white border-b border-slate-100 px-6 py-2 flex items-center justify-between">
        <p className="text-xs text-slate-400">
          {perfil?.municipio} · {perfil?.uf} · {perfil?.doenca} · {perfil?.ano || ano}
        </p>
        <span className="text-xs bg-slate-100 text-slate-500 px-2 py-1 rounded-full font-medium">
          📚 Modo Histórico
        </span>
      </div>

      {/* 1. Cabeçalho com alerta */}
      <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">

        {/* Aviso de modo histórico */}
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 text-sm text-blue-700">
          📚 Você está visualizando dados históricos de{" "}
          <strong>{perfil?.ano || ano}</strong>. Para ver a situação atual e
          previsões, use a aba{" "}
          <strong>Atual & Previsão</strong>.
        </div>

        {/* 2. KPIs históricos — 4º card mostra semana de pico em vez de risco futuro */}
        <KpiCards perfil={perfilMapped} modoHistorico={true} />

        {/* 3. Comparação interanual */}
        <ComparacaoAnual perfil={perfilMapped} />

        {/* 4. Curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          <CurvaEpidemiologica perfil={perfil} />
        </section>

        {/* 5. Distribuição regional */}
        <section>
          <SectionTitle icone="🗺️" titulo="Distribuição regional" />
          <MapaEstado
            uf={perfil?.uf}
            coIbgeDestaque={perfil?.coIbge}
            ranking={rankingEstado}
          />
        </section>

        {/* 6. Resumo histórico — IA sem encaminhamento */}
        <section>
          <SectionTitle icone="📋" titulo="Resumo histórico" />
          <ResumoIa textoIa={dados?.textoIa} perfil={perfilMapped} />
        </section>

      </div>
    </div>
  );
}
