// ComparacaoAnual — Gráfico de barras comparando os últimos 5 anos
// Ano selecionado: vermelho | Anos anteriores: cinza

import { useEffect, useState } from "react";
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import { buscarPerfil } from "../services/api";

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

export default function ComparacaoAnual({ anoBase, coIbge, somenteAnoSelecionado = false, totalAnoSelecionado = null }) {
  const [totais, setTotais] = useState({});
  const [carregando, setCarregando] = useState(false);

  useEffect(() => {
    if (somenteAnoSelecionado) {
      setTotais({ [anoBase]: totalAnoSelecionado ?? 0 });
      return;
    }

    if (!anoBase) return;
    if (!coIbge) return;

    setCarregando(true);
    const anos = Array.from({ length: 5 }, (_, i) => anoBase - 4 + i);

    Promise.allSettled(
      anos.map((ano) =>
        buscarPerfil(coIbge, ano)
          .then((res) => {
            const total =
              res.data?.perfil?.total ??
              res.data?.perfil?.totalCasos ??
              res.data?.total ??
              null;
            return { ano, total };
          })
          .catch(() => ({ ano, total: null }))
      )
    )
      .then((results) => {
        const mapa = {};
        results.forEach((r) => {
          if (r.status === "fulfilled") {
            mapa[r.value.ano] = r.value.total;
          }
        });
        setTotais(mapa);
      })
      .finally(() => setCarregando(false));
  }, [coIbge, anoBase, somenteAnoSelecionado, totalAnoSelecionado]);

  const anos = somenteAnoSelecionado
    ? [anoBase]
    : Array.from({ length: 5 }, (_, i) => anoBase - 4 + i);

  const data = {
    labels: anos.map(String),
    datasets: [
      {
        label: "Casos totais",
        data: anos.map((a) => totais[a] ?? 0),
        backgroundColor: anos.map((a) =>
          a === anoBase ? "#DC2626" : "#D1D5DB"
        ),
        borderRadius: 6,
        borderSkipped: false,
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) =>
            ` ${ctx.parsed.y.toLocaleString("pt-BR")} casos`,
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        title: { display: true, text: "Total de casos" },
      },
      x: {
        title: { display: false },
      },
    },
  };

  if (!anoBase) return null;
  if (!somenteAnoSelecionado && !coIbge) return null;

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mx-6 max-w-6xl md:mx-auto">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
        {somenteAnoSelecionado ? "📊 Total do ano selecionado" : "📊 Comparação anual"}
      </p>
      <p className="text-xs text-slate-400 mb-4">
        {somenteAnoSelecionado
          ? `Ano selecionado: ${anoBase} (sem carregar outros anos)`
          : `Últimos 5 anos — barra vermelha indica ${anoBase}`}
      </p>

      {carregando ? (
        <p className="text-sm text-slate-400 text-center py-8 animate-pulse">
          Carregando dados anuais…
        </p>
      ) : (
        <Bar data={data} options={options} />
      )}
    </div>
  );
}
