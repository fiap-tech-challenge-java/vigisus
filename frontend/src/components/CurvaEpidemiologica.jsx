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

  return (
    <div className="bg-white rounded-xl shadow p-6 mx-6 max-w-6xl md:mx-auto">
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
        📈 Curva epidemiológica — {perfil.doenca} {perfil.ano}
        <span className="ml-2 text-xs text-gray-400 normal-case">
          Pico: {pico} casos na semana {semPico}
        </span>
      </h2>
      <Line data={data} options={options} />
    </div>
  );
}
