import React, { useMemo } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler,
  Title,
  Tooltip,
  Legend
);

const CORES_CLASSIFICACAO = {
  EPIDEMIA: '#DC2626',
  ALTO:     '#EA580C',
  MODERADO: '#CA8A04',
  BAIXO:    '#16A34A',
};

// Normaliza entrada: aceita array de números OU array de { semana_epi, total_casos }
function normalizarDados(raw) {
  if (!raw || raw.length === 0) return Array(52).fill(null);
  if (typeof raw[0] === 'object' && raw[0] !== null) {
    const arr = Array(52).fill(null);
    raw.forEach((d) => {
      const idx = Number(d.semana_epi) - 1;
      if (idx >= 0 && idx < 52) arr[idx] = d.total_casos ?? null;
    });
    return arr;
  }
  return raw;
}

function CurvaEpidemiologica({
  dadosAnoAtual,
  dadosAnoAnterior,
  anoAtual,
  anoAnterior,
  doenca,
  classificacao,
  // novo formato via perfil.semanas / perfil.semanasAnoAnterior
  semanas,
  semanasAnoAnterior,
  ano,
}) {
  const dadosAtual = useMemo(
    () => normalizarDados(semanas || dadosAnoAtual),
    [semanas, dadosAnoAtual]
  );
  const dadosAnterior = useMemo(
    () => normalizarDados(semanasAnoAnterior || dadosAnoAnterior),
    [semanasAnoAnterior, dadosAnoAnterior]
  );

  const anoLabel = ano || anoAtual || new Date().getFullYear();
  const anoAnteriorLabel = anoAnterior || Number(anoLabel) - 1;

  const clasKey = (classificacao || '').toUpperCase();
  const mainColor = CORES_CLASSIFICACAO[clasKey] || '#009EE3';
  const mainBg = mainColor + '33'; // 20% opacity

  // Localizar o pico
  const pico = useMemo(() => {
    let maxVal = -Infinity;
    let maxIdx = -1;
    dadosAtual.forEach((v, i) => {
      if (v != null && v > maxVal) { maxVal = v; maxIdx = i; }
    });
    return maxIdx >= 0 ? { idx: maxIdx, val: maxVal } : null;
  }, [dadosAtual]);

  const labels = Array.from({ length: 52 }, (_, i) => `Sem. ${i + 1}`);

  const temAnterior = dadosAnterior.some((v) => v != null);

  const pointRadii = dadosAtual.map((_, i) =>
    pico && i === pico.idx ? 8 : 3
  );
  const pointStyles = dadosAtual.map((_, i) =>
    pico && i === pico.idx ? 'star' : 'circle'
  );

  const datasets = [
    {
      label: String(anoLabel),
      data: dadosAtual,
      borderColor: mainColor,
      backgroundColor: mainBg,
      borderWidth: 2.5,
      pointRadius: pointRadii,
      pointStyle: pointStyles,
      pointBackgroundColor: dadosAtual.map((_, i) =>
        pico && i === pico.idx ? mainColor : mainColor
      ),
      tension: 0.35,
      fill: true,
    },
  ];

  if (temAnterior) {
    datasets.push({
      label: String(anoAnteriorLabel),
      data: dadosAnterior,
      borderColor: '#9CA3AF',
      backgroundColor: 'transparent',
      borderWidth: 1.5,
      pointRadius: 2,
      tension: 0.35,
      borderDash: [5, 5],
      fill: false,
    });
  }

  const data = { labels, datasets };

  const options = {
    responsive: true,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { position: 'top' },
      title: {
        display: true,
        text: `Curva epidemiológica — ${doenca || 'doença'} ${anoLabel}`,
        font: { size: 14, weight: 'bold' },
        color: '#374151',
      },
      tooltip: {
        callbacks: {
          title: (items) => `Semana ${items[0].dataIndex + 1}`,
          afterBody: (items) => {
            if (pico && items[0]?.dataIndex === pico.idx) {
              return [`🔺 Pico: ${pico.val} casos (Sem. ${pico.idx + 1})`];
            }
            return [];
          },
        },
      },
    },
    scales: {
      x: {
        title: { display: true, text: 'Semana Epidemiológica' },
        ticks: { maxTicksLimit: 13 },
      },
      y: {
        title: { display: true, text: 'Número de Casos' },
        beginAtZero: true,
      },
    },
  };

  const temDados = dadosAtual.some((v) => v != null);

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-bold text-gray-800 mb-4">📈 Curva Epidemiológica</h2>
      {temDados ? (
        <Line data={data} options={options} />
      ) : (
        <div className="text-center text-gray-400 py-12">
          Dados de curva epidemiológica temporariamente indisponíveis
        </div>
      )}
    </div>
  );
}

export default CurvaEpidemiologica;
