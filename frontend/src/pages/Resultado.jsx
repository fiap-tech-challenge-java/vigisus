import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import HeaderAlerta from '../components/HeaderAlerta';
import KpiCards from '../components/KpiCards';
import CurvaEpidemiologica from '../components/CurvaEpidemiologica';
import RiscoFuturo from '../components/RiscoFuturo';
import MapaHospitais from '../components/MapaHospitais';
import ResumoIa from '../components/ResumoIa';

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
  const { dados, pergunta } = location.state || {};

  if (!dados) {
    navigate('/');
    return null;
  }

  const perfil = dados.perfil || {};
  const interpretacao = dados.interpretacao || {};

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

  const municipioNome = perfil.municipio || interpretacao.municipio;
  const municipioUf = perfil.uf || interpretacao.uf;
  const condicao = perfil.doenca || interpretacao.doenca;

  return (
    <div className="min-h-screen bg-gray-50">

      {/* BLOCO 1 — Header colorido */}
      <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />

      {/* BLOCO 2 — KPI Cards */}
      <KpiCards perfil={perfilMapped} risco={dados?.risco} />

      {/* BLOCO 3 — Gráfico */}
      <CurvaEpidemiologica perfil={perfilMapped} />

      <header className="bg-sus-green text-white px-4 py-4 shadow">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 text-white hover:text-green-100 transition-colors"
          >
            <span className="text-xl font-bold">VígiSUS</span>
          </button>
          <p className="text-green-100 text-sm truncate max-w-xs md:max-w-none">
            "{pergunta}"
          </p>
          <button
            onClick={() => navigate('/')}
            className="text-green-100 hover:text-white text-sm underline"
          >
            Nova busca
          </button>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-8 space-y-6">
        {municipioNome && (
          <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-4">
            <h1 className="text-xl font-bold text-gray-900">
              {municipioNome}
              {municipioUf ? ` — ${municipioUf}` : ''}
            </h1>
            {condicao && (
              <p className="text-sus-blue font-medium capitalize">{condicao}</p>
            )}
          </div>
        )}

        {/* Risco 14 dias */}
        <RiscoFuturo risco={dados.risco} />

        {/* Mapa + lista de Hospitais */}
        <MapaHospitais perfil={perfilMapped} encaminhamento={encaminhamentoMapped} />

        {/* Resumo IA */}
        <ResumoIa textoIa={dados.textoIa} perfil={perfilMapped} />
      </main>
    </div>
  );
}

export default Resultado;
