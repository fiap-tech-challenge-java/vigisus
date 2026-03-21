import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import HeaderAlerta from '../components/HeaderAlerta';
import InterpretacaoOperacional from '../components/InterpretacaoOperacional';
import KpiCards from '../components/KpiCards';
import CurvaEpidemiologica from '../components/CurvaEpidemiologica';
import RiscoFuturo from '../components/RiscoFuturo';
import MapaEstado from '../components/MapaEstado';
import MapaHospitais from '../components/MapaHospitais';
import ResumoIa from '../components/ResumoIa';
import { buscarRankingEstado } from '../services/api';

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

function Resultado() {
  const location = useLocation();
  const navigate = useNavigate();
  const { dados } = location.state || {};

  const [rankingEstado, setRankingEstado] = useState([]);

  useEffect(() => {
    if (dados?.perfil?.uf) {
      buscarRankingEstado(dados.perfil.uf, dados.perfil.ano)
        .then(r => setRankingEstado(r?.ranking || []))
        .catch(() => {});
    }
  }, [dados]);

  if (!dados) {
    navigate('/');
    return null;
  }

  const perfil = dados.perfil || {};

  // Map perfil fields to the shape expected by ResumoIa and MapaHospitais
  const perfilMapped = {
    ...perfil,
    totalCasos: perfil.total,
    incidencia100k: perfil.incidencia,
  };

  // Map encaminhamento hospitais to the shape expected by MapaHospitais
  const encaminhamentoMapped = dados.encaminhamento ? {
    ...dados.encaminhamento,
    hospitais: (dados.encaminhamento.hospitais || []).map(mapHospital),
  } : null;

  return (
    <div className="min-h-screen bg-gray-50">

      {/* 1. CONTEXTO — header colorido */}
      <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />

      {/* Breadcrumb pequeno — substitui o header verde do meio */}
      <div className="bg-white border-b border-gray-100 px-6 py-2
                      flex items-center justify-between sticky top-0 z-10 shadow-sm">
        <p className="text-xs text-gray-400">
          {dados?.perfil?.municipio} · {dados?.perfil?.uf} ·{" "}
          {dados?.perfil?.doenca} · {dados?.perfil?.ano}
        </p>
        <button
          onClick={() => navigate("/")}
          className="text-xs text-red-500 hover:underline"
        >
          ← Nova busca
        </button>
      </div>

      {/* Container único com espaçamento consistente */}
      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">

        {/* 2. INTERPRETAÇÃO — o que isso significa */}
        <InterpretacaoOperacional perfil={dados?.perfil} />

        {/* 3. KPIs — números principais */}
        <KpiCards perfil={perfilMapped} risco={dados?.risco} />

        {/* 4. ANÁLISE — curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          <CurvaEpidemiologica perfil={dados?.perfil} />
        </section>

        {/* 5. RISCO FUTURO — 14 bolinhas */}
        <section>
          <SectionTitle icone="🌡️" titulo="Risco futuro" />
          <RiscoFuturo risco={dados?.risco} />
        </section>

        {/* 6. VISÃO MACRO — mapa do estado */}
        <section>
          <SectionTitle icone="🗺️" titulo="Distribuição regional" />
          <MapaEstado
            uf={dados?.perfil?.uf}
            coIbgeDestaque={dados?.perfil?.coIbge}
            ranking={rankingEstado}
          />
        </section>

        {/* 7. AÇÃO — hospitais + mapa local */}
        <section>
          <SectionTitle icone="🏥" titulo="Infraestrutura disponível" />
          <MapaHospitais
            perfil={perfilMapped}
            encaminhamento={encaminhamentoMapped}
          />
        </section>

        {/* 8. RESUMO FINAL — IA */}
        <section>
          <SectionTitle icone="📋" titulo="Resumo operacional" />
          <ResumoIa textoIa={dados?.textoIa} perfil={perfilMapped} />
        </section>

      </div>
    </div>
  );
}

export default Resultado;
