import { useState } from "react";

export default function ResumoIa({ textoIa, perfil }) {
  const [expandido, setExpandido] = useState(false);

  // Bullets fixos baseados nos dados (sempre presentes)
  const bullets = [
    { label: "Situação",   valor: perfil?.classificacao || "—" },
    { label: "Tendência",  valor: perfil?.tendencia || "—" },
    { label: "Casos",      valor: perfil?.totalCasos
        ? `${perfil.totalCasos.toLocaleString("pt-BR")} em ${perfil.ano}`
        : "—" },
    { label: "Incidência", valor: perfil?.incidencia100k
        ? `${perfil.incidencia100k.toFixed(0)}/100k hab.`
        : "—" },
  ];

  return (
    <div className="bg-gray-50 border border-gray-200 rounded-xl p-5 mx-6 max-w-6xl md:mx-auto mt-4 mb-8">
      <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
        🤖 Análise VígiSUS
      </p>

      {/* Bullets fixos */}
      <ul className="space-y-1 mb-3">
        {bullets.map((b, i) => (
          <li key={i} className="text-sm text-gray-700">
            <span className="text-gray-400">• {b.label}:</span>{" "}
            <span className="font-medium">{b.valor}</span>
          </li>
        ))}
      </ul>

      {/* Texto da IA — colapsável */}
      {textoIa && (
        <>
          <p className={`text-sm text-gray-500 leading-relaxed ${
            !expandido ? "line-clamp-2" : ""
          }`}>
            {textoIa}
          </p>
          <button
            onClick={() => setExpandido(!expandido)}
            className="mt-1 text-xs text-blue-500 hover:underline"
          >
            {expandido ? "Ver menos" : "Ver análise completa"}
          </button>
        </>
      )}
    </div>
  );
}
