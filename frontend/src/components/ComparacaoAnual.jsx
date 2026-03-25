// ComparacaoAnual - Grafico de barras comparando os ultimos 5 anos
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
import { buscarBrasil, buscarHistoricoEstado, buscarPerfil } from "../services/api";

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

export default function ComparacaoAnual({
  anoBase,
  coIbge,
  uf,
  doenca = "dengue",
  escopo = "municipio",
  totalAnoSelecionado = null,
}) {
  const [totais, setTotais] = useState({});
  const [carregando, setCarregando] = useState(false);

  useEffect(() => {
    if (!anoBase) return;
    if (escopo === "municipio" && !coIbge) return;
    if (escopo === "estado" && !uf) return;

    setCarregando(true);
    const anos = Array.from({ length: 5 }, (_, i) => anoBase - 4 + i);

    const buscarTotalPorAno = async (ano) => {
      if (ano === anoBase && totalAnoSelecionado != null) {
        return { ano, total: totalAnoSelecionado };
      }

      if (escopo === "brasil") {
        const res = await buscarBrasil(doenca, ano);
        return { ano, total: res?.totalCasos ?? res?.total ?? null };
      }

      if (escopo === "estado") {
        const res = await buscarHistoricoEstado(uf, ano, doenca);
        return { ano, total: res?.total ?? res?.totalCasos ?? null };
      }

      const res = await buscarPerfil(coIbge, ano, doenca);
      return {
        ano,
        total:
          res.data?.perfil?.total ??
          res.data?.perfil?.totalCasos ??
          res.data?.total ??
          res.data?.totalCasos ??
          null,
      };
    };

    Promise.allSettled(
      anos.map((ano) => buscarTotalPorAno(ano).catch(() => ({ ano, total: null })))
    )
      .then((results) => {
        const mapa = {};
        results.forEach((resultado) => {
          if (resultado.status === "fulfilled") {
            mapa[resultado.value.ano] = resultado.value.total;
          }
        });
        setTotais(mapa);
      })
      .finally(() => setCarregando(false));
  }, [anoBase, coIbge, uf, doenca, escopo, totalAnoSelecionado]);

  const anos = Array.from({ length: 5 }, (_, i) => anoBase - 4 + i);

  const data = {
    labels: anos.map(String),
    datasets: [
      {
        label: "Casos totais",
        data: anos.map((ano) => totais[ano] ?? 0),
        backgroundColor: anos.map((ano) =>
          ano === anoBase ? "#DC2626" : "#D1D5DB"
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
          label: (ctx) => ` ${ctx.parsed.y.toLocaleString("pt-BR")} casos`,
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
  if (escopo === "municipio" && !coIbge) return null;
  if (escopo === "estado" && !uf) return null;

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mx-6 max-w-6xl md:mx-auto vigi-focus-card" tabIndex={0}>
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
        Comparacao anual
      </p>
      <p className="text-xs text-slate-400 mb-4">
        Ultimos 5 anos - barra vermelha indica {anoBase}
      </p>

      {carregando ? (
        <p className="text-sm text-slate-400 text-center py-8 animate-pulse">
          Carregando dados anuais...
        </p>
      ) : (
        <Bar data={data} options={options} />
      )}
    </div>
  );
}
