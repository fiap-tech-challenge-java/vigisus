import React from 'react';

const COR_SCORE = {
  MUITO_ALTO: { bg: "bg-red-500",    emoji: "🔴", text: "text-red-700" },
  ALTO:       { bg: "bg-orange-400", emoji: "🟠", text: "text-orange-700" },
  MODERADO:   { bg: "bg-yellow-400", emoji: "🟡", text: "text-yellow-700" },
  BAIXO:      { bg: "bg-green-400",  emoji: "🟢", text: "text-green-700" },
};

export default function RiscoFuturo({ risco }) {
  const dias = risco?.risco14Dias || [];
  const fatores = risco?.fatores || [];

  if (!dias.length) return null;

  return (
    <div className="bg-white rounded-xl shadow p-6 mx-6 max-w-6xl md:mx-auto mt-4">
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
        🌡️ Risco epidemiológico — próximos 14 dias
      </h2>

      {/* 14 círculos */}
      <div className="flex gap-2 flex-wrap mb-4">
        {dias.map((dia, i) => {
          const cor = COR_SCORE[dia.classificacao] || COR_SCORE.BAIXO;
          return (
            <div key={i} className="group relative flex flex-col items-center">
              <div
                className={`w-8 h-8 rounded-full ${cor.bg} cursor-pointer
                            transition hover:scale-110`}
                title={`${dia.data}: ${dia.tempMax}°C, ${dia.chuvaMm}mm`}
              />
              {/* Tooltip */}
              <div className="absolute bottom-10 left-1/2 -translate-x-1/2
                              bg-gray-800 text-white text-xs rounded px-2 py-1
                              whitespace-nowrap opacity-0 group-hover:opacity-100
                              pointer-events-none z-10 transition">
                {dia.data}<br/>
                🌡️ {dia.tempMax}°C · 🌧️ {dia.chuvaMm}mm
              </div>
            </div>
          );
        })}
      </div>

      {/* Legenda */}
      <div className="flex gap-4 text-xs text-gray-500 mb-4">
        {Object.entries(COR_SCORE).reverse().map(([key, val]) => (
          <span key={key} className="flex items-center gap-1">
            <span>{val.emoji}</span> {key.replace("_", " ")}
          </span>
        ))}
      </div>

      {/* Fatores em chips */}
      {fatores.length > 0 && (
        <div className="flex flex-wrap gap-2 mt-2">
          {fatores.map((f, i) => (
            <span key={i}
              className="text-xs bg-gray-100 text-gray-600 px-3 py-1 rounded-full">
              {f}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
