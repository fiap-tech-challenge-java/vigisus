import { gerarInsightsIA } from "../utils/iaInsights";

export default function InsightsIaBloco({
  perfil,
  ranking = [],
  risco = null,
  textoIa = "",
  cor = "slate",
  foco = "geral",
}) {
  const analise = gerarInsightsIA({ perfil, ranking, risco, textoIa });

  const cores = {
    blue: {
      destaque: "bg-blue-100 text-blue-900 border-blue-200",
      titulo: "text-blue-800",
      texto: "text-blue-900",
      card: "bg-white/70 border-blue-100",
      bullet: "text-blue-500",
    },
    slate: {
      destaque: "bg-slate-100 text-slate-900 border-slate-200",
      titulo: "text-slate-800",
      texto: "text-slate-700",
      card: "bg-white border-slate-200",
      bullet: "text-slate-500",
    },
  }[cor] || {
    destaque: "bg-slate-100 text-slate-900 border-slate-200",
    titulo: "text-slate-800",
    texto: "text-slate-700",
    card: "bg-white border-slate-200",
    bullet: "text-slate-500",
  };

  const renderLista = (titulo, itens) => {
    if (!itens?.length) return null;

    return (
      <div className={`rounded-xl border p-4 ${cores.card}`}>
        <p className={`text-xs font-semibold uppercase tracking-wider mb-2 ${cores.titulo}`}>
          {titulo}
        </p>
        <ul className="space-y-2">
          {itens.map((item, index) => (
            <li key={`${titulo}-${index}`} className={`flex gap-2 text-sm leading-relaxed ${cores.texto}`}>
              <span className={`mt-0.5 ${cores.bullet}`}>•</span>
              <span>{item}</span>
            </li>
          ))}
        </ul>
      </div>
    );
  };

  const configuracoes = {
    acao: {
      tituloPrincipal: "Leitura para decisao imediata",
      textoPrincipal: analise.mensagemClara,
      coluna1Titulo: "Prioridades operacionais",
      coluna1Itens: analise.implicacoes,
      coluna2Titulo: "Sinais para acompanhar agora",
      coluna2Itens: analise.monitoramento,
      blocoExtraTitulo: "Pontos menos obvios do cenario",
      blocoExtraItens: analise.achados,
    },
    resumo: {
      tituloPrincipal: "Fechamento executivo",
      textoPrincipal: analise.mensagemClara,
      coluna1Titulo: "Principais conclusoes",
      coluna1Itens: analise.achados,
      coluna2Titulo: "O que isso significa para a gestao",
      coluna2Itens: analise.implicacoes,
      blocoExtraTitulo: "Pontos que merecem seguimento",
      blocoExtraItens: analise.monitoramento,
    },
    historico: {
      tituloPrincipal: "Leitura ampliada do periodo",
      textoPrincipal: analise.mensagemClara,
      coluna1Titulo: "Leituras que nao estao explicitas no grafico",
      coluna1Itens: analise.achados,
      coluna2Titulo: "O que isso significa no periodo analisado",
      coluna2Itens: analise.implicacoes,
      blocoExtraTitulo: "Pontos para acompanhar",
      blocoExtraItens: analise.monitoramento,
    },
    geral: {
      tituloPrincipal: "Leitura complementar",
      textoPrincipal: analise.mensagemClara,
      coluna1Titulo: "Leituras complementares",
      coluna1Itens: analise.achados,
      coluna2Titulo: "Implicacoes",
      coluna2Itens: analise.implicacoes,
      blocoExtraTitulo: "Monitoramento",
      blocoExtraItens: analise.monitoramento,
    },
  };

  const config = configuracoes[foco] || configuracoes.geral;

  return (
    <div className="space-y-4">
      <div className={`rounded-xl border px-4 py-3 ${cores.destaque}`}>
        <p className="text-sm font-semibold leading-relaxed">{analise.headline}</p>
        <p className={`text-xs font-semibold uppercase tracking-wider mt-3 ${cores.titulo}`}>
          {config.tituloPrincipal}
        </p>
        {config.textoPrincipal && (
          <p className="text-sm mt-2 leading-relaxed opacity-90">{analise.mensagemClara}</p>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {renderLista(config.coluna1Titulo, config.coluna1Itens)}
        {renderLista(config.coluna2Titulo, config.coluna2Itens)}
      </div>

      {renderLista(config.blocoExtraTitulo, config.blocoExtraItens)}
    </div>
  );
}
