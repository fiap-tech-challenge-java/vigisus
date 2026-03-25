// KpisHistorico — 2 cards grandes para o modo Histórico
// Card 1: Total do ano | Card 2: Pico registrado (incidência + mês)

/** Lookup de semana epidemiológica (1–52) → mês abreviado (pt-BR). */
const MESES_SEMANA = [
  "",
  "Jan","Jan","Jan","Jan",
  "Fev","Fev","Fev","Fev",
  "Mar","Mar","Mar","Mar","Mar",
  "Abr","Abr","Abr","Abr",
  "Mai","Mai","Mai","Mai",
  "Jun","Jun","Jun","Jun",
  "Jul","Jul","Jul","Jul","Jul",
  "Ago","Ago","Ago","Ago",
  "Set","Set","Set","Set",
  "Out","Out","Out","Out","Out",
  "Nov","Nov","Nov","Nov",
  "Dez","Dez","Dez","Dez",
];

/** Encontra o pico nas semanas epidemiológicas, aceitando variações de nome de campo. */
function calcularPico(semanas) {
  if (!Array.isArray(semanas) || semanas.length === 0)
    return { casos: null, semana: null, mes: "—" };

  const melhor = semanas.reduce((max, s) => {
    const c = s.casos ?? s.totalCasos ?? s.total_casos ?? 0;
    const m = max.casos ?? max.totalCasos ?? max.total_casos ?? 0;
    return c > m ? s : max;
  }, semanas[0]);

  const casos  = melhor.casos ?? melhor.totalCasos ?? melhor.total_casos ?? 0;
  const semana = melhor.semanaEpi ?? melhor.semana_epi ?? melhor.semana ?? 0;

  return {
    casos,
    semana,
    mes: MESES_SEMANA[semana] || (semana ? `Sem. ${semana}` : "—"),
  };
}

export default function KpisHistorico({ perfil }) {
  if (!perfil) return null;

  const totalCasos = perfil.totalCasos ?? perfil.total ?? 0;
  const pico = calcularPico(perfil.semanas);

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6 max-w-6xl mx-auto">
      {/* Card 1 — Total do ano */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 flex flex-col gap-2 vigi-focus-card" tabIndex={0} role="group" aria-label="Card de total anual">
        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
          Total do ano
        </p>
        <p className="text-5xl font-black text-slate-800">
          {totalCasos.toLocaleString("pt-BR")}
        </p>
        <p className="text-sm text-slate-500">
          casos registrados em {perfil.ano}
        </p>
      </div>

      {/* Card 2 — Pico registrado */}
      <div className="bg-white rounded-2xl shadow-sm border border-red-200 p-8 flex flex-col gap-2 vigi-focus-card" tabIndex={0} role="group" aria-label="Card de pico anual">
        <p className="text-xs font-semibold text-red-400 uppercase tracking-wider">
          Pico registrado
        </p>
        <p className="text-5xl font-black text-red-700">
          {pico.casos != null ? pico.casos.toLocaleString("pt-BR") : "—"}
        </p>
        <p className="text-sm text-slate-500">
          {pico.mes !== "—" ? `Pico em ${pico.mes} · Semana ${pico.semana}` : "Sem dados"}
        </p>
      </div>
    </div>
  );
}
