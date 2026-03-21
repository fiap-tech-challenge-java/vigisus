import { getCor, TENDENCIA } from "../utils/cores";

export default function HeaderAlerta({ perfil, risco }) {
  const cor = getCor(perfil?.classificacao);
  const tend = TENDENCIA[perfil?.tendencia] || TENDENCIA.ESTAVEL;

  return (
    <div className={`${cor.bg} ${cor.text} px-6 py-5`}>
      <div className="max-w-6xl mx-auto">

        {/* Linha 1: Município + doença + ano */}
        <p className="text-sm opacity-80 font-medium uppercase tracking-wider mb-1">
          {perfil?.municipio} — {perfil?.uf} · {perfil?.doenca} · {perfil?.ano}
        </p>

        {/* Linha 2: Badge + métricas */}
        <div className="flex flex-wrap items-center gap-6">

          {/* Badge classificação */}
          <span className="text-4xl md:text-5xl font-black tracking-tight">
            {perfil?.classificacao === "EPIDEMIA" && (
              <span className="inline-block animate-pulse mr-2">🔴</span>
            )}
            {perfil?.classificacao === "ALTO" && "🟠 "}
            {perfil?.classificacao === "MODERADO" && "🟡 "}
            {perfil?.classificacao === "BAIXO" && "🟢 "}
            {perfil?.classificacao || "—"}
          </span>

          {/* Incidência */}
          <div>
            <p className="text-xs opacity-70">Incidência</p>
            <p className="text-xl font-bold">
              {perfil?.incidencia100k?.toLocaleString("pt-BR", { maximumFractionDigits: 0 })}
              <span className="text-sm font-normal opacity-80"> /100k hab.</span>
            </p>
          </div>

          {/* Tendência */}
          <div>
            <p className="text-xs opacity-70">Tendência</p>
            <p className="text-xl font-bold">
              {tend.icone} {tend.label}
            </p>
          </div>

          {/* Pressão SUS */}
          {risco?.pressaoSus && (
            <div>
              <p className="text-xs opacity-70">Pressão SUS</p>
              <p className="text-xl font-bold">{risco.pressaoSus}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
