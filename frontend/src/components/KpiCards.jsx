import React from 'react';

const CORES = {
  EPIDEMIA:     { bg: '#FEE2E2', badge: '#DC2626', text: '#991B1B' },
  ALTO:         { bg: '#FFEDD5', badge: '#EA580C', text: '#9A3412' },
  MODERADO:     { bg: '#FEF9C3', badge: '#CA8A04', text: '#92400E' },
  BAIXO:        { bg: '#DCFCE7', badge: '#16A34A', text: '#14532D' },
  INDISPONIVEL: { bg: '#F3F4F6', badge: '#6B7280', text: '#374151' },
};

function ClassBadge({ classificacao }) {
  const key = (classificacao || 'INDISPONIVEL').toUpperCase();
  const c = CORES[key] || CORES.INDISPONIVEL;
  return (
    <span
      style={{ backgroundColor: c.badge, color: '#FFFFFF' }}
      className="inline-block rounded-full px-4 py-1 text-lg font-bold"
    >
      {classificacao || '—'}
    </span>
  );
}

function Card({ title, value, label, children, accent }) {
  return (
    <div
      className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-1"
      style={accent ? { borderTopColor: accent, borderTopWidth: 3 } : {}}
    >
      <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">{title}</p>
      <div className="mt-1">
        {children || (
          <p className="text-4xl font-extrabold text-gray-900 leading-none">{value ?? '—'}</p>
        )}
      </div>
      {label && <p className="text-xs text-gray-400 mt-1">{label}</p>}
    </div>
  );
}

function KpiCards({ totalCasos, incidencia100k, classificacao, riscoClassificacao, riscoScore, ano }) {
  const clasKey = (classificacao || 'INDISPONIVEL').toUpperCase();
  const riskKey = (riscoClassificacao || 'INDISPONIVEL').toUpperCase();
  const coresClas = CORES[clasKey] || CORES.INDISPONIVEL;
  const coresRisk = CORES[riskKey] || CORES.INDISPONIVEL;

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      <Card title="Total de Casos" accent={coresClas.badge} label={ano ? `casos em ${ano}` : 'casos registrados'}>
        <p className="text-4xl font-extrabold text-gray-900 leading-none">
          {totalCasos != null ? Number(totalCasos).toLocaleString('pt-BR') : '—'}
        </p>
      </Card>

      <Card title="Incidência" accent="#6B7280" label="por 100 mil hab.">
        <p className="text-4xl font-extrabold text-gray-900 leading-none">
          {incidencia100k != null ? Number(incidencia100k).toLocaleString('pt-BR') : '—'}
        </p>
      </Card>

      <Card title="Classificação" accent={coresClas.badge} label="situação atual">
        <ClassBadge classificacao={classificacao} />
      </Card>

      <Card title="Risco (próx. 2 sem.)" accent={coresRisk.badge} label={riscoScore != null ? `Score ${riscoScore}/8` : undefined}>
        <ClassBadge classificacao={riscoClassificacao} />
      </Card>
    </div>
  );
}

export default KpiCards;
