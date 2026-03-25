const COR_SCORE = {
  MUITO_ALTO: { bg: "bg-red-500", label: "Muito alto" },
  ALTO: { bg: "bg-orange-400", label: "Alto" },
  MODERADO: { bg: "bg-yellow-400", label: "Moderado" },
  BAIXO: { bg: "bg-green-400", label: "Baixo" },
};

export default function RiscoFuturo({ risco }) {
  const dias = risco?.risco14Dias || [];
  const fatores = risco?.fatores || [];

  if (!dias.length) return null;

  return (
    <div className="bg-white rounded-xl shadow p-6 mx-6 max-w-6xl md:mx-auto mt-4 vigi-focus-card" tabIndex={0}>
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
        Risco epidemiologico - proximos 14 dias
      </h2>

      <div className="flex gap-2 flex-wrap mb-4" role="list" aria-label="Linha de risco dos proximos 14 dias">
        {dias.map((dia, i) => {
          const cor = COR_SCORE[dia.classificacao] || COR_SCORE.BAIXO;

          return (
            <div key={`${dia.data}-${i}`} className="group relative flex flex-col items-center" role="listitem">
              <button
                type="button"
                className={`w-8 h-8 rounded-full ${cor.bg} transition hover:scale-110`}
                title={`${dia.data}: ${dia.tempMax}C, ${dia.chuvaMm}mm`}
                aria-label={`${dia.data}, risco ${cor.label}, temperatura maxima ${dia.tempMax} graus e chuva ${dia.chuvaMm} milimetros`}
              />
              <div className="absolute bottom-10 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs rounded px-2 py-1 whitespace-nowrap opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 pointer-events-none z-10 transition">
                {dia.data}
                <br />
                {dia.tempMax}C · {dia.chuvaMm}mm
              </div>
            </div>
          );
        })}
      </div>

      <div className="flex gap-4 text-xs text-gray-500 mb-4">
        {Object.entries(COR_SCORE).reverse().map(([key, val]) => (
          <span key={key} className="flex items-center gap-1">
            <span className={`inline-block w-2.5 h-2.5 rounded-full ${val.bg}`} aria-hidden="true" />
            {val.label}
          </span>
        ))}
      </div>

      {fatores.length > 0 && (
        <div className="flex flex-wrap gap-2 mt-2" aria-label="Fatores de risco">
          {fatores.map((fator, i) => (
            <span key={`${fator}-${i}`} className="text-xs bg-gray-100 text-gray-600 px-3 py-1 rounded-full">
              {fator}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
