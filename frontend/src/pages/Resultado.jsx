import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import HeaderAlerta from '../components/HeaderAlerta';
import KpiCards from '../components/KpiCards';
import CurvaEpidemiologica from '../components/CurvaEpidemiologica';
import RiscoFuturo from '../components/RiscoFuturo';
import MapaHospitais from '../components/MapaHospitais';
import ResumoIa from '../components/ResumoIa';
import { buscarRisco, buscarHospitais } from '../services/api';

// Skeleton placeholder for loading state
function Skeleton({ height = 'h-40', label }) {
  return (
    <div className={`bg-gray-100 animate-pulse rounded-xl ${height} flex items-center justify-center`}>
      <span className="text-gray-400 text-sm">
        {label ? `Dados de ${label} temporariamente indisponíveis` : 'Carregando...'}
      </span>
    </div>
  );
}

function Resultado() {
  const location = useLocation();
  const navigate = useNavigate();
  const { dados, pergunta } = location.state || {};

  const [risco, setRisco] = useState(null);
  const [hospitais, setHospitais] = useState([]);
  const [loadingRisco, setLoadingRisco] = useState(false);
  const [loadingHospitais, setLoadingHospitais] = useState(false);
  const [erroRisco, setErroRisco] = useState(false);
  const [erroHospitais, setErroHospitais] = useState(false);

  useEffect(() => {
    if (!dados) {
      navigate('/');
      return;
    }

    const coIbge = dados.coIbge || dados.municipio?.coIbge;

    if (coIbge) {
      setLoadingRisco(true);
      setErroRisco(false);
      buscarRisco(coIbge)
        .then((res) => setRisco(res.data))
        .catch(() => { setRisco(null); setErroRisco(true); })
        .finally(() => setLoadingRisco(false));

      setLoadingHospitais(true);
      setErroHospitais(false);
      buscarHospitais(coIbge, dados.condicao || 'dengue')
        .then((res) => setHospitais(res.data || []))
        .catch(() => { setHospitais([]); setErroHospitais(true); })
        .finally(() => setLoadingHospitais(false));
    }
  }, [dados, navigate]);

  if (!dados) return null;

  const perfil = dados.perfil || {};
  const riscoData = risco || dados.risco || {};
  const encaminhamento = dados.encaminhamento || {};

  // Extrair dados do perfil com fallbacks
  const municipioNome = dados.municipio?.nome || perfil.municipio || '';
  const ufSigla = dados.municipio?.uf || perfil.uf || '';
  const doencaNome = (dados.condicao || perfil.doenca || '').toUpperCase() || 'DENGUE';
  const ano = perfil.ano || perfil.anoAtual || new Date().getFullYear();
  const classificacao = (perfil.classificacao || '').toUpperCase();
  const incidencia100k = perfil.incidencia100k;
  const totalCasos = perfil.totalCasos;
  const tendencia = perfil.tendencia || '';
  const pressaoSus = perfil.pressaoSus || '';

  const riscoClassificacao = (riscoData.classificacao || riscoData.nivel || '').toUpperCase();
  const riscoScore = riscoData.score;
  const previsao14Dias = riscoData.previsao14Dias || riscoData.previsao || [];
  const tempMedia = riscoData.tempMedia || riscoData.temperatura;
  const chuvaTotal = riscoData.chuvaTotal || riscoData.chuva;
  const umidadeMedia = riscoData.umidadeMedia || riscoData.umidade;

  const hospitaisList = hospitais.length > 0
    ? hospitais
    : encaminhamento.hospitais || [];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Nav */}
      <nav className="bg-gray-900 text-white px-4 py-3">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 hover:text-red-400 transition-colors"
          >
            <div className="w-7 h-7 bg-red-600 rounded-full flex items-center justify-center">
              <span className="text-white text-sm font-extrabold">V</span>
            </div>
            <span className="text-lg font-extrabold">VígiSUS</span>
          </button>
          {pergunta && (
            <p className="text-gray-400 text-sm truncate max-w-xs hidden sm:block">
              "{pergunta}"
            </p>
          )}
          <button
            onClick={() => navigate('/')}
            className="text-gray-400 hover:text-white text-sm underline"
          >
            Nova busca
          </button>
        </div>
      </nav>

      {/* BLOCO 1 — Header de Alerta */}
      <HeaderAlerta
        municipio={municipioNome}
        uf={ufSigla}
        doenca={doencaNome}
        ano={ano}
        classificacao={classificacao}
        incidencia100k={incidencia100k}
        tendencia={tendencia}
        pressaoSus={pressaoSus}
      />

      <main className="max-w-5xl mx-auto px-4 py-6 space-y-6">
        {/* BLOCO 2 — KPI Cards */}
        <KpiCards
          totalCasos={totalCasos}
          incidencia100k={incidencia100k}
          classificacao={classificacao}
          riscoClassificacao={riscoClassificacao || undefined}
          riscoScore={riscoScore}
          ano={ano}
        />

        {/* BLOCO 3 — Gráfico Epidemiológico */}
        <CurvaEpidemiologica
          semanas={perfil.semanas}
          semanasAnoAnterior={perfil.semanasAnoAnterior}
          dadosAnoAtual={perfil.casosAnoAtual}
          dadosAnoAnterior={perfil.casosAnoAnterior}
          anoAtual={perfil.anoAtual || ano}
          anoAnterior={perfil.anoAnterior || (Number(ano) - 1)}
          doenca={doencaNome}
          classificacao={classificacao}
          ano={ano}
        />

        {/* BLOCO 4 — Risco Futuro */}
        {loadingRisco ? (
          <Skeleton height="h-40" label="previsão de risco" />
        ) : erroRisco ? (
          <div className="bg-gray-100 rounded-xl p-6 text-center text-gray-400">
            Dados de previsão de risco temporariamente indisponíveis
          </div>
        ) : (
          <RiscoFuturo
            previsao14Dias={previsao14Dias}
            tempMedia={tempMedia}
            chuvaTotal={chuvaTotal}
            umidadeMedia={umidadeMedia}
          />
        )}

        {/* BLOCO 5 — Mapa + Hospitais */}
        {loadingHospitais ? (
          <Skeleton height="h-64" label="hospitais" />
        ) : erroHospitais && hospitaisList.length === 0 ? (
          <div className="bg-gray-100 rounded-xl p-6 text-center text-gray-400">
            Dados de hospitais temporariamente indisponíveis
          </div>
        ) : (
          <MapaHospitais
            municipio={municipioNome}
            lat={dados.municipio?.lat}
            lng={dados.municipio?.lng}
            hospitais={hospitaisList}
          />
        )}

        {/* BLOCO 6 — Resumo da IA */}
        <ResumoIa
          textoIa={dados.textoIa || dados.texto || dados.analise}
          situacao={classificacao || undefined}
          tendencia={tendencia || undefined}
          riscoClimatico={riscoClassificacao || undefined}
        />
      </main>
    </div>
  );
}

export default Resultado;
