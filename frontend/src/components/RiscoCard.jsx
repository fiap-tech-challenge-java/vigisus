import React from 'react';

const NIVEIS = {
  BAIXO: { emoji: '🟢', label: 'Baixo', bg: 'bg-green-50', border: 'border-green-400', text: 'text-green-700' },
  MODERADO: { emoji: '🟡', label: 'Moderado', bg: 'bg-yellow-50', border: 'border-yellow-400', text: 'text-yellow-700' },
  ALTO: { emoji: '🟠', label: 'Alto', bg: 'bg-orange-50', border: 'border-orange-400', text: 'text-orange-700' },
  MUITO_ALTO: { emoji: '🔴', label: 'Muito Alto', bg: 'bg-red-50', border: 'border-red-500', text: 'text-red-700' },
};

function RiscoCard({ score, nivel, fatores }) {
  const nivelKey = (nivel || 'BAIXO').toUpperCase().replace(' ', '_');
  const config = NIVEIS[nivelKey] || NIVEIS.BAIXO;

  return (
    <div className={`rounded-lg shadow-sm border-l-4 p-6 ${config.bg} ${config.border}`}>
      <h2 className="text-lg font-bold text-gray-800 mb-3">⚠️ Risco nas Próximas 2 Semanas</h2>

      <div className="flex items-center gap-3 mb-4">
        <span className="text-4xl">{config.emoji}</span>
        <div>
          <p className={`text-2xl font-bold ${config.text}`}>{config.label}</p>
          {score !== undefined && score !== null && (
            <p className="text-gray-500 text-sm">Score: {typeof score === 'number' ? score.toFixed(2) : score}</p>
          )}
        </div>
      </div>

      {fatores && fatores.length > 0 && (
        <div>
          <p className="font-semibold text-gray-700 mb-2">Fatores identificados:</p>
          <ul className="space-y-1">
            {fatores.map((fator, idx) => (
              <li key={idx} className="flex items-start gap-2 text-gray-600 text-sm">
                <span className="mt-0.5">•</span>
                <span>{fator}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default RiscoCard;
