import React, { useEffect, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import HeaderAlerta from "../components/HeaderAlerta";
import InterpretacaoOperacional from "../components/InterpretacaoOperacional";
import KpiCards from "../components/KpiCards";
import CurvaEpidemiologica from "../components/CurvaEpidemiologica";
import RiscoFuturo from "../components/RiscoFuturo";
import MapaEstado from "../components/MapaEstado";
import MapaHospitais from "../components/MapaHospitais";
import ResumoIa from "../components/ResumoIa";
import { buscarPorPergunta, buscarRankingEstado } from "../services/api";

function SectionTitle({ icone, titulo }) {
  return (
    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
      {icone} {titulo}
    </p>
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
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const municipio = searchParams.get("municipio");
  const uf = searchParams.get("uf");
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
      const pergunta = `${doenca} em ${municipio} ${uf}`;
      setLoading(true);
      buscarPorPergunta(pergunta)
        .then(r => setDados(r.data))
        .catch(() => setErro("Erro ao buscar dados. Tente novamente."))
        .finally(() => setLoading(false));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [municipio, uf, doenca]);

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
          <p className="text-gray-400 text-sm animate-pulse">⏳ Carregando dados...</p>
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

  const hoje = new Date();
  const em14dias = new Date(hoje.getTime() + 14 * 24 * 60 * 60 * 1000);
  const formatarData = (d) =>
    d.toLocaleDateString("pt-BR", { day: "2-digit", month: "short" });

  const perfilMapped = {
    ...perfil,
    totalCasos: perfil.total,
    incidencia100k: perfil.incidencia,
  };

  const encaminhamentoMapped = dados.encaminhamento
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
          {perfil?.municipio} · {perfil?.uf} · {perfil?.doenca}
        </p>
        <span className="text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded-full font-medium">
          📡 Atual & Previsão
        </span>
      </div>

      {/* 1. Cabeçalho com alerta */}
      <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">

        {/* 2. Interpretação operacional — tomada de decisão */}
        <InterpretacaoOperacional perfil={perfil} />

        {/* 3. KPIs */}
        <KpiCards perfil={perfilMapped} risco={dados?.risco} />

        {/* 4. Curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          <CurvaEpidemiologica perfil={perfil} />
        </section>

        {/* 5. Risco futuro (14 dias) */}
        <section>
          <SectionTitle
            icone="🌡️"
            titulo={`Risco climático — ${formatarData(hoje)} a ${formatarData(em14dias)}`}
          />
          <RiscoFuturo risco={dados?.risco} />
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

        {/* 7. Infraestrutura / encaminhamento */}
        <section>
          <SectionTitle icone="🏥" titulo="Infraestrutura disponível" />
          <MapaHospitais
            perfil={perfilMapped}
            encaminhamento={encaminhamentoMapped}
          />
        </section>

        {/* 8. Resumo IA */}
        <section>
          <SectionTitle icone="📋" titulo="Resumo operacional" />
          <ResumoIa textoIa={dados?.textoIa} perfil={perfilMapped} />
        </section>

      </div>
    </div>
  );
}
