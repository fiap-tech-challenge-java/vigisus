import React, { useState } from 'react';

// Tenta extrair bullet points de um texto livre
function extrairBullets(texto) {
  if (!texto) return [];
  const linhas = texto
    .split(/\n|\. /)
    .map((l) => l.trim())
    .filter((l) => l.length > 10);
  return linhas.slice(0, 4);
}

function ResumoIa({ textoIa, situacao, tendencia, riscoClimatico }) {
  const [expandido, setExpandido] = useState(false);

  if (!textoIa && !situacao && !tendencia && !riscoClimatico) return null;

  const bullets = extrairBullets(textoIa);
  const temTextoLongo = textoIa && textoIa.length > 200;

  return (
    <div className="bg-gray-50 border border-gray-200 rounded-xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-gray-600 text-lg">🤖</span>
        <h2 className="text-sm font-bold text-gray-700 uppercase tracking-wide">Análise VígiSUS</h2>
      </div>

      <ul className="space-y-1.5 text-sm text-gray-700">
        {situacao && (
          <li className="flex gap-2">
            <span className="text-gray-400 shrink-0">•</span>
            <span><strong>Situação:</strong> {situacao}</span>
          </li>
        )}
        {tendencia && (
          <li className="flex gap-2">
            <span className="text-gray-400 shrink-0">•</span>
            <span><strong>Tendência:</strong> {tendencia}</span>
          </li>
        )}
        {riscoClimatico && (
          <li className="flex gap-2">
            <span className="text-gray-400 shrink-0">•</span>
            <span><strong>Risco climático:</strong> {riscoClimatico}</span>
          </li>
        )}
        {bullets.map((b, i) => (
          <li key={i} className="flex gap-2">
            <span className="text-gray-400 shrink-0">•</span>
            <span>{b}</span>
          </li>
        ))}
      </ul>

      {temTextoLongo && !situacao && !tendencia && (
        <div className="mt-3">
          <div
            className={`text-sm text-gray-600 leading-relaxed overflow-hidden transition-all ${
              expandido ? '' : 'max-h-16'
            }`}
            style={expandido ? {} : { WebkitMaskImage: 'linear-gradient(to bottom, black 60%, transparent 100%)' }}
          >
            <p className="whitespace-pre-line">{textoIa}</p>
          </div>
          <button
            onClick={() => setExpandido(!expandido)}
            className="mt-1 text-xs text-blue-600 hover:underline"
          >
            {expandido ? 'Ver menos' : 'Ver mais'}
          </button>
        </div>
      )}
    </div>
  );
}

export default ResumoIa;
