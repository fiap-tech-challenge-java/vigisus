import React from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
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
  Title,
  Tooltip,
  Legend
);

function CurvaEpidemiologica({ dadosAnoAtual, dadosAnoAnterior, anoAtual, anoAnterior }) {
  const semanas = Array.from({ length: 52 }, (_, i) => `SE ${i + 1}`);

  const data = {
    labels: semanas,
    datasets: [
      {
        label: `${anoAtual || 'Ano atual'}`,
        data: dadosAnoAtual || [],
        borderColor: '#009EE3',
        backgroundColor: 'rgba(0, 158, 227, 0.1)',
        borderWidth: 2,
        pointRadius: 3,
        tension: 0.3,
      },
      {
        label: `${anoAnterior || 'Ano anterior'}`,
        data: dadosAnoAnterior || [],
        borderColor: '#009C3B',
        backgroundColor: 'rgba(0, 156, 59, 0.1)',
        borderWidth: 2,
        pointRadius: 3,
        tension: 0.3,
        borderDash: [5, 5],
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: false,
      },
      tooltip: {
        callbacks: {
          title: (items) => `Semana Epidemiológica ${items[0].dataIndex + 1}`,
        },
      },
    },
    scales: {
      x: {
        title: {
          display: true,
          text: 'Semana Epidemiológica',
        },
        ticks: {
          maxTicksLimit: 13,
        },
      },
      y: {
        title: {
          display: true,
          text: 'Número de Casos',
        },
        beginAtZero: true,
      },
    },
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-bold text-gray-800 mb-4">📈 Curva Epidemiológica</h2>
      <Line data={data} options={options} />
    </div>
  );
}

export default CurvaEpidemiologica;
