import { useState } from "react";
import InsightsIaBloco from "./InsightsIaBloco";

export default function ResumoIa({ textoIa, perfil, ranking = [], risco = null }) {
  const [expandido, setExpandido] = useState(false);

  const bullets = [
    { label: "Situacao", valor: perfil?.classificacao || "-" },
    { label: "Tendencia", valor: perfil?.tendencia || "-" },
    {
      label: "Casos",
      valor: perfil?.totalCasos
        ? `${perfil.totalCasos.toLocaleString("pt-BR")} em ${perfil.ano}`
        : "-",
    },
    {
      label: "Incidencia",
      valor: perfil?.incidencia100k
        ? `${perfil.incidencia100k.toFixed(0)}/100k hab.`
        : "-",
    },
  ];

  return (
    <div className="bg-gray-50 border border-gray-200 rounded-xl p-5 mx-6 max-w-6xl md:mx-auto mt-4 mb-8 vigi-focus-card" tabIndex={0}>
      <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
        Resumo operacional ampliado
      </p>

      <ul className="space-y-1 mb-4">
        {bullets.map((bullet, index) => (
          <li key={index} className="text-sm text-gray-700">
            <span className="text-gray-400">• {bullet.label}:</span>{" "}
            <span className="font-medium">{bullet.valor}</span>
          </li>
        ))}
      </ul>

      <InsightsIaBloco
        perfil={perfil}
        ranking={ranking}
        risco={risco}
        textoIa={textoIa}
        foco="resumo"
      />

      {textoIa && (
        <>
          <p
            className={`mt-4 text-sm text-gray-500 leading-relaxed whitespace-pre-line ${
              !expandido ? "line-clamp-3" : ""
            }`}
          >
            {textoIa}
          </p>
          <button
            onClick={() => setExpandido(!expandido)}
            className="mt-1 text-xs text-blue-500 hover:underline"
          >
            {expandido ? "Ver menos" : "Ver analise completa"}
          </button>
        </>
      )}
    </div>
  );
}
