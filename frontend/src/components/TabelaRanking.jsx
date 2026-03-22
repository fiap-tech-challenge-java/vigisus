// TabelaRanking — tabela dos municípios do ranking
// Ordenada por incidência decrescente, máximo 20 linhas + "Ver todos" expansível

import { useState } from "react";
import { getCor } from "../utils/cores";

const LIMITE_PADRAO = 20;

const ICONE_CLASSIF = {
  EPIDEMIA: "🔴",
  ALTO:     "🟠",
  MODERADO: "🟡",
  BAIXO:    "🟢",
};

export default function TabelaRanking({ ranking = [] }) {
  const [expandido, setExpandido] = useState(false);

  if (!ranking.length) return null;

  // Ordenar por incidência decrescente
  const ordenado = [...ranking].sort(
    (a, b) => (b.incidencia ?? b.incidencia100k ?? 0) - (a.incidencia ?? a.incidencia100k ?? 0)
  );

  const exibidos = expandido ? ordenado : ordenado.slice(0, LIMITE_PADRAO);
  const temMais = ordenado.length > LIMITE_PADRAO;

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden mx-6 max-w-6xl md:mx-auto">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 border-b border-slate-200">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider w-10">
              #
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Município
            </th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Casos
            </th>
            <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Incidência
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider hidden sm:table-cell">
              Classificação
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {exibidos.map((item, idx) => {
            const classif = item.classificacao?.toUpperCase() || "INDISPONÍVEL";
            const cor = getCor(classif);
            const icone = ICONE_CLASSIF[classif] || "⚪";
            const incidencia = item.incidencia ?? item.incidencia100k ?? null;
            const casos = item.total ?? item.casos ?? item.totalCasos ?? null;

            return (
              <tr
                key={item.coIbge ?? idx}
                className={`hover:bg-slate-50 transition-colors ${idx === 0 ? "font-semibold" : ""}`}
              >
                <td className="px-4 py-3 text-slate-400 text-xs">{idx + 1}</td>
                <td className="px-4 py-3 text-slate-700">
                  {item.municipio || item.nome || "—"}
                </td>
                <td className="px-4 py-3 text-right text-slate-700">
                  {casos !== null ? casos.toLocaleString("pt-BR") : "—"}
                </td>
                <td className="px-4 py-3 text-right text-slate-700">
                  {incidencia !== null
                    ? `${Math.round(incidencia).toLocaleString("pt-BR")}/100k`
                    : "—"}
                </td>
                <td className="px-4 py-3 hidden sm:table-cell">
                  <span className={`inline-flex items-center gap-1 text-xs font-bold px-2 py-0.5 rounded-full ${cor.badge}`}>
                    {icone} {classif}
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {temMais && (
        <div className="border-t border-slate-100 px-4 py-3 text-center">
          <button
            onClick={() => setExpandido(!expandido)}
            className="text-xs text-blue-500 hover:underline font-medium"
          >
            {expandido
              ? "Ver menos"
              : `Ver todos (${ordenado.length} municípios)`}
          </button>
        </div>
      )}
    </div>
  );
}
