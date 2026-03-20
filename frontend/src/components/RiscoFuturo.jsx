import React, { useState } from 'react';

function scoreParaCor(score) {
  if (score <= 2) return '#16A34A'; // verde
  if (score <= 4) return '#CA8A04'; // amarelo
  if (score <= 6) return '#EA580C'; // laranja
  return '#DC2626';                 // vermelho
}

function scoreParaEmoji(score) {
  if (score <= 2) return '🟢';
  if (score <= 4) return '🟡';
  if (score <= 6) return '🟠';
  return '🔴';
}

function DayCircle({ dia }) {
  const [showTip, setShowTip] = useState(false);
  const cor = scoreParaCor(dia.score_dia ?? dia.score ?? 0);

  const label = [
    dia.data,
    dia.temperatura != null ? `${dia.temperatura}°C` : null,
    dia.chuva != null ? `${dia.chuva}mm` : null,
    dia.umidade != null ? `Umidade: ${dia.umidade}%` : null,
  ].filter(Boolean).join(' · ');

  return (
    <div className="relative flex flex-col items-center">
      <button
        className="w-8 h-8 rounded-full border-2 border-white shadow-md focus:outline-none transition-transform hover:scale-110"
        style={{ backgroundColor: cor }}
        onMouseEnter={() => setShowTip(true)}
        onMouseLeave={() => setShowTip(false)}
        onFocus={() => setShowTip(true)}
        onBlur={() => setShowTip(false)}
        aria-label={label || `Dia ${dia.data}`}
      />
      {showTip && label && (
        <div
          className="absolute bottom-10 left-1/2 z-20 whitespace-nowrap rounded bg-gray-900 text-white text-xs px-2 py-1 shadow-lg pointer-events-none"
          style={{ transform: 'translateX(-50%)' }}
        >
          {label}
        </div>
      )}
    </div>
  );
}

function RiscoFuturo({ previsao14Dias, tempMedia, chuvaTotal, umidadeMedia }) {
  if (!previsao14Dias || previsao14Dias.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 text-center text-gray-400">
        Dados de previsão de risco temporariamente indisponíveis
      </div>
    );
  }

  // Exibir legenda de emojis
  const emojis = previsao14Dias.map((d) => scoreParaEmoji(d.score_dia ?? d.score ?? 0));

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-bold text-gray-800 mb-1">⚠️ Previsão de risco — próximos 14 dias</h2>
      <p className="text-xs text-gray-400 mb-4">Passe o mouse sobre cada círculo para detalhes</p>

      <div className="flex items-center gap-2 flex-wrap mb-4">
        {previsao14Dias.map((dia, i) => (
          <DayCircle key={i} dia={dia} />
        ))}
      </div>

      <p className="text-sm text-gray-500 mb-4 tracking-wide">{emojis.join(' ')}</p>

      <div className="flex flex-wrap gap-2">
        {tempMedia != null && (
          <span className="inline-flex items-center gap-1 bg-orange-50 text-orange-700 text-xs font-medium px-3 py-1 rounded-full">
            🌡️ Temp. média: {tempMedia}°C
          </span>
        )}
        {chuvaTotal != null && (
          <span className="inline-flex items-center gap-1 bg-blue-50 text-blue-700 text-xs font-medium px-3 py-1 rounded-full">
            🌧️ Chuva prevista: {chuvaTotal}mm
          </span>
        )}
        {umidadeMedia != null && (
          <span className="inline-flex items-center gap-1 bg-cyan-50 text-cyan-700 text-xs font-medium px-3 py-1 rounded-full">
            💧 Umidade: {umidadeMedia}%
          </span>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3 mt-4 text-xs text-gray-500">
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full inline-block bg-green-600"></span>Baixo (0–2)</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full inline-block bg-yellow-500"></span>Moderado (3–4)</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full inline-block bg-orange-500"></span>Alto (5–6)</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded-full inline-block bg-red-600"></span>Muito Alto (7–8)</span>
      </div>
    </div>
  );
}

export default RiscoFuturo;
