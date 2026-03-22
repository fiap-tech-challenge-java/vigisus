import { getCor } from "../utils/cores";

const contextoIncidencia = (inc, classif) => {
  if (!inc) return null;
  if (classif === "EPIDEMIA") return "⚠️ Muito acima do normal";
  if (classif === "ALTO")     return "↑ Acima da média";
  if (classif === "MODERADO") return "→ Na média";
  return "✓ Abaixo da média";
};

function Card({ titulo, valor, sub, cor, extraClasses = "", children }) {
  return (
    <div className={`bg-white rounded-xl shadow-sm p-5 flex flex-col gap-1 ${extraClasses}`}>
      <p className="text-xs text-gray-400 uppercase tracking-wider">{titulo}</p>
      <p className="text-3xl font-black text-gray-800">{valor}</p>
      {sub && <p className="text-xs text-gray-400">{sub}</p>}
      {cor && (
        <span className={`mt-1 self-start text-xs font-bold px-2 py-0.5 rounded-full ${cor.badge}`}>
          {cor.label || ""}
        </span>
      )}
      {children}
    </div>
  );
}

export default function KpiCards({ perfil, risco, modoHistorico = false }) {
  const corClassif = getCor(perfil?.classificacao);
  const corRisco   = getCor(risco?.classificacao);

  // Peak week calculation for historical mode
  const semanas = perfil?.semanas || [];
  const picoDado = semanas.length
    ? semanas.reduce((prev, curr) => (curr.casos > prev.casos ? curr : prev), semanas[0])
    : null;
  const picoValor = picoDado ? picoDado.casos : null;
  const picoSemana = picoDado ? picoDado.semanaEpi : null;

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 p-6 max-w-6xl mx-auto">

      <Card
        titulo="Total de casos"
        valor={perfil?.totalCasos?.toLocaleString("pt-BR") || "—"}
        sub={`em ${perfil?.ano}`}
      >
        {perfil?.semanasAnoAnterior?.length > 0 && (() => {
          const totalAnterior = perfil.semanasAnoAnterior
            .reduce((s, x) => s + x.casos, 0);
          const variacao = totalAnterior > 0
            ? ((perfil.totalCasos - totalAnterior) / totalAnterior * 100).toFixed(0)
            : null;
          return variacao ? (
            <p className={`text-xs mt-1 ${Number(variacao) > 0 ? "text-red-500" : "text-green-600"}`}>
              {Number(variacao) > 0 ? "↑ +" : "↓ "}{variacao}% vs {perfil.ano - 1}
            </p>
          ) : null;
        })()}
      </Card>

      <Card
        titulo="Incidência"
        valor={perfil?.incidencia100k?.toLocaleString("pt-BR", { maximumFractionDigits: 0 }) || "—"}
        sub="por 100 mil habitantes"
      >
        <p className="text-xs text-gray-400 mt-1">
          {contextoIncidencia(perfil?.incidencia100k, perfil?.classificacao)}
        </p>
      </Card>

      <Card
        titulo="Classificação"
        valor={perfil?.classificacao || "—"}
        cor={{ ...corClassif, label: perfil?.classificacao }}
        extraClasses={`border-2 ${corClassif.borda || "border-gray-200"} ${corClassif.light || ""}`}
      />

      {modoHistorico ? (
        <Card
          titulo="Semana de pico"
          valor={picoSemana ? `Sem. ${picoSemana}` : "—"}
          sub={picoValor ? `${picoValor.toLocaleString("pt-BR")} casos` : ""}
          extraClasses="border-2 border-slate-200 bg-slate-50"
        />
      ) : (
        <Card
          titulo="Risco próx. 2 semanas"
          valor={`${risco?.score ?? "—"}/8`}
          sub={risco?.classificacao || ""}
          cor={{ ...corRisco, label: risco?.classificacao }}
        />
      )}

    </div>
  );
}
