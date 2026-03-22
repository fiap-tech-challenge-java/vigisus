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
    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
      {icone} {titulo}
    </p>
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
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
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
        <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center gap-3">
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
    <div className="min-h-screen bg-gray-50">
      <TopNav />

      {/* Breadcrumb */}
      <div className="bg-white border-b border-gray-100 px-6 py-2 flex items-center justify-between">
        <p className="text-xs text-gray-400">
          {perfil?.municipio} · {perfil?.uf} · {perfil?.doenca} · {perfil?.ano || ano}
        </p>
        <span className="text-xs bg-gray-100 text-gray-500 px-2 py-1 rounded-full font-medium">
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

        {/* 2. KPIs */}
        <KpiCards perfil={perfilMapped} risco={dados?.risco} />

        {/* 3. Curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          <CurvaEpidemiologica perfil={perfil} />
        </section>

        {/* 4. Distribuição regional */}
        <section>
          <SectionTitle icone="🗺️" titulo="Distribuição regional" />
          <MapaEstado
            uf={perfil?.uf}
            coIbgeDestaque={perfil?.coIbge}
            ranking={rankingEstado}
          />
        </section>

        {/* 5. Resumo histórico — IA sem encaminhamento */}
        <section>
          <SectionTitle icone="📋" titulo="Resumo histórico" />
          <ResumoIa textoIa={dados?.textoIa} perfil={perfilMapped} />
        </section>

      </div>
    </div>
  );
}
