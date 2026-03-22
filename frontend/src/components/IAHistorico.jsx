// IAHistorico — Card com análise da IA sobre o período histórico
// Visual "analítico": fundo azul claro, borda azul, destaque de IA

import { useState } from "react";

export default function IAHistorico({ textoIa, ano }) {
  const [expandido, setExpandido] = useState(false);

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-2xl p-6 mx-6 max-w-6xl md:mx-auto">
      {/* Cabeçalho */}
      <div className="flex items-center gap-2 mb-4">
        <span className="text-lg">🤖</span>
        <div>
          <p className="text-sm font-bold text-blue-800">
            Análise do período — {ano}
          </p>
          <p className="text-xs text-blue-500 font-medium">
            🤖 Análise gerada por IA
          </p>
        </div>
      </div>

      {/* Corpo */}
      {textoIa ? (
        <>
          <p
            className={`text-sm text-blue-900 leading-relaxed whitespace-pre-line ${
              !expandido ? "line-clamp-4" : ""
            }`}
          >
            {textoIa}
          </p>
          <button
            onClick={() => setExpandido(!expandido)}
            className="mt-2 text-xs text-blue-600 hover:underline font-medium"
          >
            {expandido ? "Ver menos" : "Ver análise completa"}
          </button>
        </>
      ) : (
        <p className="text-sm text-blue-400 italic">
          Análise não disponível para este período.
        </p>
      )}

      {/* Rodapé */}
      <p className="mt-4 text-xs text-blue-400 border-t border-blue-200 pt-3">
        Gerado com base em dados reais do SINAN
      </p>
    </div>
  );
}
