import { useState } from "react";
import InsightsIaBloco from "./InsightsIaBloco";

export default function IAHistorico({ textoIa, ano, perfil, ranking = [] }) {
  const [expandido, setExpandido] = useState(false);

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-2xl p-6 mx-6 max-w-6xl md:mx-auto">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-lg">IA</span>
        <div>
          <p className="text-sm font-bold text-blue-800">
            Analise do periodo - {ano}
          </p>
          <p className="text-xs text-blue-500 font-medium">
            Leitura ampliada e complementar aos graficos
          </p>
        </div>
      </div>

      <InsightsIaBloco
        perfil={perfil}
        ranking={ranking}
        textoIa={textoIa}
        cor="blue"
        foco="historico"
      />

      {textoIa ? (
        <div className="mt-4 pt-4 border-t border-blue-200">
          <p className="text-xs font-semibold uppercase tracking-wider text-blue-500 mb-2">
            Texto-base da IA
          </p>
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
            {expandido ? "Ver menos" : "Ver analise completa"}
          </button>
        </div>
      ) : (
        <p className="mt-4 text-sm text-blue-400 italic">
          Texto narrativo indisponivel; exibindo leitura complementar baseada nos dados.
        </p>
      )}

      <p className="mt-4 text-xs text-blue-400 border-t border-blue-200 pt-3">
        Sintese baseada em SINAN, serie semanal, comparacao anual e distribuicao territorial.
      </p>
    </div>
  );
}
