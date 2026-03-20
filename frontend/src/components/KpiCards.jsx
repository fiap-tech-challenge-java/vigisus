import { getCor } from "../utils/cores";

function Card({ titulo, valor, sub, cor }) {
  return (
    <div className="bg-white rounded-xl shadow p-5 flex flex-col gap-1">
      <p className="text-xs text-gray-400 uppercase tracking-wider">{titulo}</p>
      <p className="text-3xl font-black text-gray-800">{valor}</p>
      {sub && <p className="text-xs text-gray-400">{sub}</p>}
      {cor && (
        <span className={`mt-1 self-start text-xs font-bold px-2 py-0.5 rounded-full ${cor.badge}`}>
          {cor.label || ""}
        </span>
      )}
    </div>
  );
}

export default function KpiCards({ perfil, risco }) {
  const corClassif = getCor(perfil?.classificacao);
  const corRisco   = getCor(risco?.classificacao);

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 p-6 max-w-6xl mx-auto">

      <Card
        titulo="Total de casos"
        valor={perfil?.totalCasos?.toLocaleString("pt-BR") || "—"}
        sub={`em ${perfil?.ano}`}
      />

      <Card
        titulo="Incidência"
        valor={perfil?.incidencia100k?.toLocaleString("pt-BR", { maximumFractionDigits: 0 }) || "—"}
        sub="por 100 mil habitantes"
      />

      <Card
        titulo="Classificação"
        valor={perfil?.classificacao || "—"}
        cor={{ ...corClassif, label: perfil?.classificacao }}
      />

      <Card
        titulo="Risco próx. 2 semanas"
        valor={`${risco?.score ?? "—"}/8`}
        sub={risco?.classificacao || ""}
        cor={{ ...corRisco, label: risco?.classificacao }}
      />

    </div>
  );
}
