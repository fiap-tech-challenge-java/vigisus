import { getCor, TENDENCIA } from "../utils/cores";

const CONTEXTO_INCIDENCIA = {
  EPIDEMIA: "⚠️ Muito acima do normal",
  ALTO:     "↑ Acima da média",
  MODERADO: "→ Na média estadual",
  BAIXO:    "✓ Abaixo da média",
};

const ICONE_TENDENCIA = {
  CRESCENTE:   "↗",
  ESTAVEL:     "→",
  DECRESCENTE: "↘",
};

export default function StatusRapido({ perfil, risco }) {
  const classificacao = perfil?.classificacao?.toUpperCase();
  const contextoInc   = CONTEXTO_INCIDENCIA[classificacao] || "— Sem dados";

  const tendenciaKey  = perfil?.tendencia?.toUpperCase();
  const tendenciaInfo = TENDENCIA[tendenciaKey] || null;
  const icone         = ICONE_TENDENCIA[tendenciaKey] || "—";

  const corRisco = getCor(risco?.classificacao);

  return (
    <div className="bg-white rounded-xl shadow-sm mx-6 max-w-6xl md:mx-auto mt-2">
      <div className="grid grid-cols-3 divide-x divide-gray-100">

        {/* Incidência */}
        <div className="flex flex-col gap-0.5 px-6 py-4">
          <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
            Incidência
          </p>
          <p className="text-2xl font-black text-gray-800">
            {perfil?.incidencia100k != null
              ? perfil.incidencia100k.toLocaleString("pt-BR", { maximumFractionDigits: 0 })
              : "—"}
            <span className="text-sm font-normal text-gray-500">/100k</span>
          </p>
          <p className="text-xs text-gray-400">{contextoInc}</p>
        </div>

        {/* Tendência */}
        <div className="flex flex-col gap-0.5 px-6 py-4">
          <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
            Tendência
          </p>
          <p className={`text-2xl font-black ${tendenciaInfo?.cor || "text-gray-800"}`}>
            {icone}{" "}
            <span className="text-lg">
              {tendenciaInfo?.label || perfil?.tendencia || "—"}
            </span>
          </p>
          <p className="text-xs text-gray-400">Última semana</p>
        </div>

        {/* Risco futuro */}
        <div className="flex flex-col gap-0.5 px-6 py-4">
          <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
            Risco futuro
          </p>
          <p className="text-2xl font-black text-gray-800">
            {risco?.score != null ? `${risco.score}/8` : "—"}
            {risco?.classificacao && (
              <span
                className={`ml-2 text-sm font-bold px-2 py-0.5 rounded-full ${corRisco.badge}`}
              >
                {risco.classificacao}
              </span>
            )}
          </p>
          <p className="text-xs text-gray-400">Próx. 14 dias</p>
        </div>

      </div>
    </div>
  );
}
