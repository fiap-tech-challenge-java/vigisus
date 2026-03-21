import {
  Chart as ChartJS,
  LineElement, PointElement, LinearScale, CategoryScale,
  Filler, Tooltip, Legend
} from "chart.js";
import { Line } from "react-chartjs-2";
import { getCor } from "../utils/cores";

ChartJS.register(LineElement, PointElement, LinearScale,
  CategoryScale, Filler, Tooltip, Legend);

export default function CurvaEpidemiologica({ perfil }) {
  if (!perfil?.semanas?.length) return null;

  const cor = getCor(perfil.classificacao);
  const semanas = Array.from({ length: 52 }, (_, i) => i + 1);

  const casosPorSemana = (lista) => semanas.map(s => {
    const found = lista?.find(x => x.semanaEpi === s);
    return found ? found.casos : 0;
  });

  const casosAno      = casosPorSemana(perfil.semanas);
  const casosAnterior = casosPorSemana(perfil.semanasAnoAnterior);

  const pico = Math.max(...casosAno);
  const semPico = casosAno.indexOf(pico) + 1;

  const data = {
    labels: semanas.map(s => `Sem. ${s}`),
    datasets: [
      {
        label: `${perfil.doenca} ${perfil.ano}`,
        data: casosAno,
        borderColor: cor.hex,
        backgroundColor: cor.hex + "33",
        fill: true,
        tension: 0.3,
        pointRadius: casosAno.map((_, i) => i + 1 === semPico ? 6 : 2),
        pointBackgroundColor: casosAno.map((_, i) =>
          i + 1 === semPico ? cor.hex : cor.hex + "88"
        ),
      },
      ...(perfil.semanasAnoAnterior?.length ? [{
        label: `${perfil.doenca} ${perfil.ano - 1}`,
        data: casosAnterior,
        borderColor: "#9CA3AF",
        borderDash: [5, 5],
        backgroundColor: "transparent",
        fill: false,
        tension: 0.3,
        pointRadius: 0,
      }] : []),
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: { position: "top" },
      tooltip: {
        callbacks: {
          afterLabel: (ctx) =>
            ctx.dataIndex + 1 === semPico
              ? `🔴 Pico da epidemia`
              : "",
        },
      },
    },
    scales: {
      y: { beginAtZero: true, title: { display: true, text: "Casos" } },
      x: { ticks: { maxTicksLimit: 13 } },
    },
  };

  const temComparativo = perfil?.semanasAnoAnterior?.length > 0;

  return (
    <div className="bg-white rounded-xl shadow-sm p-6 mx-6 max-w-6xl md:mx-auto">
      <div className="flex items-start justify-between mb-4">
        <div>
          <h2 className="text-sm font-semibold text-gray-700">
            Casos por semana epidemiológica — {perfil.ano}
          </h2>
          <p className="text-xs text-gray-400 mt-0.5">
            {temComparativo
              ? `Linha sólida = ${perfil.ano} · Linha tracejada = ${perfil.ano - 1}`
              : `Semanas 1 a 52 de ${perfil.ano}`}
          </p>
        </div>
        <div className="text-right">
          <p className="text-xs text-gray-400">Pico registrado</p>
          <p className="text-sm font-bold text-gray-700">
            {pico} casos — Sem. {semPico}
          </p>
        </div>
      </div>
      <Line data={data} options={options} />
    </div>
  );
}
