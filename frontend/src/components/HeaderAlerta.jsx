import React from 'react';

const CORES = {
  EPIDEMIA:     { bg: '#DC2626', text: '#FFFFFF' },
  ALTO:         { bg: '#EA580C', text: '#FFFFFF' },
  MODERADO:     { bg: '#CA8A04', text: '#FFFFFF' },
  BAIXO:        { bg: '#16A34A', text: '#FFFFFF' },
  INDISPONIVEL: { bg: '#6B7280', text: '#FFFFFF' },
};

const TENDENCIA = {
  CRESCENTE:   { icon: '↗', color: '#FCA5A5', label: 'Crescente' },
  ESTAVEL:     { icon: '→', color: '#D1D5DB', label: 'Estável' },
  DECRESCENTE: { icon: '↘', color: '#86EFAC', label: 'Decrescente' },
};

function HeaderAlerta({ municipio, uf, doenca, ano, classificacao, incidencia100k, tendencia, pressaoSus }) {
  const clasKey = (classificacao || 'INDISPONIVEL').toUpperCase();
  const cores = CORES[clasKey] || CORES.INDISPONIVEL;
  const tend = TENDENCIA[(tendencia || '').toUpperCase()] || null;

  const pressaoLabel =
    pressaoSus === 'Alta' || pressaoSus === 'ALTA' ? 'Alta'
    : pressaoSus === 'Media' || pressaoSus === 'MEDIA' || pressaoSus === 'Média' ? 'Média'
    : pressaoSus === 'Baixa' || pressaoSus === 'BAIXA' ? 'Baixa'
    : pressaoSus || null;

  return (
    <div
      style={{ backgroundColor: cores.bg, color: cores.text }}
      className="w-full px-4 py-5 shadow-lg"
    >
      <div className="max-w-5xl mx-auto">
        <p className="text-sm font-semibold uppercase tracking-widest opacity-80 mb-1">
          {[municipio, uf, doenca, ano].filter(Boolean).join(' · ')}
        </p>
        <div className="flex flex-wrap items-center gap-4 mt-1">
          <span className="text-2xl font-extrabold tracking-tight">
            {clasKey === 'EPIDEMIA' ? '🔴' : clasKey === 'ALTO' ? '🟠' : clasKey === 'MODERADO' ? '🟡' : '🟢'}{' '}
            {classificacao || 'SEM DADOS'}
          </span>

          {incidencia100k != null && (
            <span className="bg-white/20 rounded px-2 py-0.5 text-sm font-medium">
              Incidência: {Number(incidencia100k).toLocaleString('pt-BR')}/100k
            </span>
          )}

          {tend && (
            <span className="bg-white/20 rounded px-2 py-0.5 text-sm font-medium">
              {tend.icon} {tend.label}
            </span>
          )}

          {pressaoLabel && (
            <span className="bg-white/20 rounded px-2 py-0.5 text-sm font-medium">
              Pressão SUS: {pressaoLabel}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

export default HeaderAlerta;
