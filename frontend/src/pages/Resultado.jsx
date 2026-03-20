import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import TextoIa from '../components/TextoIa';
import CurvaEpidemiologica from '../components/CurvaEpidemiologica';
import RiscoCard from '../components/RiscoCard';
import MapaHospitais from '../components/MapaHospitais';
import HeaderAlerta from '../components/HeaderAlerta';
import KpiCards from '../components/KpiCards';
import { buscarRisco, buscarHospitais } from '../services/api';

function Resultado() {
  const location = useLocation();
  const navigate = useNavigate();
  const { dados, pergunta } = location.state || {};

  const [risco, setRisco] = useState(null);
  const [hospitais, setHospitais] = useState([]);
  const [loadingRisco, setLoadingRisco] = useState(false);
  const [loadingHospitais, setLoadingHospitais] = useState(false);

  useEffect(() => {
    if (!dados) {
      navigate('/');
      return;
    }

    const coIbge = dados.coIbge || dados.municipio?.coIbge;

    if (coIbge) {
      setLoadingRisco(true);
      buscarRisco(coIbge)
        .then((res) => setRisco(res.data))
        .catch(() => setRisco(null))
        .finally(() => setLoadingRisco(false));

      setLoadingHospitais(true);
      buscarHospitais(coIbge, dados.condicao || 'dengue')
        .then((res) => setHospitais(res.data || []))
        .catch(() => setHospitais([]))
        .finally(() => setLoadingHospitais(false));
    }
  }, [dados, navigate]);

  if (!dados) return null;

  return (
    <div className="min-h-screen bg-gray-50">

      {/* BLOCO 1 — Header colorido */}
      <HeaderAlerta perfil={dados?.perfil} risco={dados?.risco} />

      {/* BLOCO 2 — KPI Cards */}
      <KpiCards perfil={dados?.perfil} risco={dados?.risco} />

      {/* BLOCO 3 — Gráfico */}
      <CurvaEpidemiologica perfil={dados?.perfil} />

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
        {dados.municipio && (
          <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-4">
            <h1 className="text-xl font-bold text-gray-900">
              {dados.municipio.nome}
              {dados.municipio.uf ? ` — ${dados.municipio.uf}` : ''}
            </h1>
            {dados.condicao && (
              <p className="text-sus-blue font-medium capitalize">{dados.condicao}</p>
            )}
          </div>
        )}

        {/* Texto IA */}
        <TextoIa texto={dados.textoIa || dados.texto || dados.analise} />

        {/* Risco */}
        {loadingRisco ? (
          <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 text-center text-gray-400">
            Carregando previsão de risco...
          </div>
        ) : (
          (risco || dados.risco) && (
            <RiscoCard
              score={(risco || dados.risco).score}
              nivel={(risco || dados.risco).nivel}
              fatores={(risco || dados.risco).fatores || []}
            />
          )
        )}

        {/* Mapa de Hospitais */}
        {loadingHospitais ? (
          <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 text-center text-gray-400">
            Carregando hospitais próximos...
          </div>
        ) : (
          <MapaHospitais
            municipio={dados.municipio?.nome}
            lat={dados.municipio?.lat}
            lng={dados.municipio?.lng}
            hospitais={hospitais}
          />
        )}
      </main>
    </div>
  );
}

export default Resultado;
