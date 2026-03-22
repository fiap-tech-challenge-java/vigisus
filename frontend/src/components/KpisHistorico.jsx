// KpisHistorico — 2 cards grandes para o modo Histórico
// Card 1: Total do ano | Card 2: Pico registrado (incidência + mês)

const MESES = [
  "", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
  "Jul", "Ago", "Set", "Out", "Nov", "Dez",
];

/** Converte semana epidemiológica (1–52) para mês abreviado.
 *  Nota: esta é uma aproximação — cada mês tem ~4,33 semanas e as semanas
 *  epidemiológicas nem sempre coincidem com o início do mês calendário.
 */
function semanaParaMes(semana) {
  if (!semana) return "";
  // Cada mês tem ~4.33 semanas; semana 1 começa em janeiro.
  const mes = Math.min(12, Math.ceil(semana / 4.33));
  return MESES[mes] || "";
}

/** Retorna o número de casos de uma entrada de semana, aceitando campos alternativos. */
function getCasosFromSemana(s) {
  return s.casos ?? s.totalCasos ?? 0;
}

export default function KpisHistorico({ perfil }) {
  if (!perfil) return null;

  const totalCasos = perfil.totalCasos ?? perfil.total ?? 0;
  const semanas = perfil.semanas || [];

  const picoDado = semanas.length
    ? semanas.reduce((prev, curr) =>
        getCasosFromSemana(curr) > getCasosFromSemana(prev) ? curr : prev
      , semanas[0])
    : null;

  const picoCasos = picoDado != null ? getCasosFromSemana(picoDado) || null : null;
  const picoSemana = picoDado != null
    ? (picoDado.semanaEpi ?? picoDado.semana_epi ?? picoDado.semana ?? null)
    : null;
  const picoMes = semanaParaMes(picoSemana);

  const populacao = perfil.populacao || null;
  const incidenciaPico =
    picoCasos && populacao ? ((picoCasos / populacao) * 100000).toFixed(0) : null;

  // Valor e subtítulo do card de pico
  const picoPrincipal = incidenciaPico
    ? `${Number(incidenciaPico).toLocaleString("pt-BR")}/100k`
    : picoCasos?.toLocaleString("pt-BR") ?? "—";

  const picoSub = (() => {
    if (!picoSemana) return "—";
    const semLabel = `Semana ${picoSemana}${picoMes ? ` — ${picoMes}` : ""}`;
    if (incidenciaPico) return `${semLabel} · ${picoCasos?.toLocaleString("pt-BR")} casos`;
    return picoCasos ? `${semLabel} · ${picoCasos.toLocaleString("pt-BR")} casos` : semLabel;
  })();

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6 max-w-6xl mx-auto">
      {/* Card 1 — Total do ano */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 flex flex-col gap-2">
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
      <div className="bg-white rounded-2xl shadow-sm border border-red-200 p-8 flex flex-col gap-2">
        <p className="text-xs font-semibold text-red-400 uppercase tracking-wider">
          Pico registrado
        </p>
        <p className="text-5xl font-black text-red-700">
          {picoPrincipal}
        </p>
        <p className="text-sm text-slate-500">{picoSub}</p>
      </div>
    </div>
  );
}
