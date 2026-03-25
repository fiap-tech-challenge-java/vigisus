function numero(valor, fallback = 0) {
  return Number.isFinite(Number(valor)) ? Number(valor) : fallback;
}

function formatarInteiro(valor) {
  return numero(valor).toLocaleString("pt-BR");
}

function formatarDecimal(valor, casas = 1) {
  return numero(valor).toLocaleString("pt-BR", {
    minimumFractionDigits: casas,
    maximumFractionDigits: casas,
  });
}

function formatarPercentual(valor, casas = 1) {
  return `${formatarDecimal(valor, casas)}%`;
}

function listaSemanas(semanas = []) {
  return (Array.isArray(semanas) ? semanas : []).map((item) => ({
    semana: numero(item?.semanaEpi ?? item?.semana ?? item?.semana_epi),
    casos: numero(item?.casos ?? item?.total ?? item?.valor),
  }));
}

function somarSemanas(semanas = []) {
  return listaSemanas(semanas).reduce((acc, item) => acc + item.casos, 0);
}

function calcularPico(semanas = []) {
  const normalizadas = listaSemanas(semanas).filter((item) => item.semana > 0);
  if (!normalizadas.length) return null;

  return normalizadas.reduce((melhor, atual) => (
    atual.casos > melhor.casos ? atual : melhor
  ), normalizadas[0]);
}

function calcularUltimasSemanas(semanas = [], quantidade = 4) {
  const normalizadas = listaSemanas(semanas)
    .filter((item) => item.semana > 0)
    .sort((a, b) => a.semana - b.semana);

  return normalizadas.slice(-quantidade);
}

function percentualDoTotal(parte, total) {
  if (!total) return 0;
  return (numero(parte) / numero(total)) * 100;
}

function calcularVariacaoPercentual(atual, anterior) {
  if (!anterior) return null;
  return ((numero(atual) - numero(anterior)) / numero(anterior)) * 100;
}

function calcularConcentracaoRanking(ranking = [], quantidade = 3) {
  const lista = (Array.isArray(ranking) ? ranking : [])
    .map((item) => ({
      nome: item?.municipio || item?.nome || item?.sgUf || item?.uf || "Regiao",
      total: numero(item?.totalCasos ?? item?.total ?? item?.casos),
    }))
    .filter((item) => item.total > 0)
    .sort((a, b) => b.total - a.total);

  if (!lista.length) return null;

  const total = lista.reduce((acc, item) => acc + item.total, 0);
  const top = lista.slice(0, quantidade);
  const somaTop = top.reduce((acc, item) => acc + item.total, 0);

  return {
    nomes: top.map((item) => item.nome),
    somaTop,
    total,
    participacao: percentualDoTotal(somaTop, total),
  };
}

function fraseClassificacao(classificacao, incidencia) {
  const nivel = String(classificacao || "SEM_DADO").toUpperCase();

  if (nivel === "EPIDEMIA") {
    return `A incidência de ${formatarDecimal(incidencia)} por 100 mil coloca o local em faixa epidêmica, indicando transmissão muito acima do esperado.`;
  }
  if (nivel === "ALTO") {
    return `A incidência de ${formatarDecimal(incidencia)} por 100 mil mostra pressão elevada, acima do padrão confortável para vigilância de rotina.`;
  }
  if (nivel === "MODERADO") {
    return `A incidência de ${formatarDecimal(incidencia)} por 100 mil sugere circulação relevante, mas sem o mesmo grau de pressão observado em cenários epidêmicos.`;
  }
  if (nivel === "BAIXO") {
    return `A incidência de ${formatarDecimal(incidencia)} por 100 mil indica circulação mais controlada no período analisado.`;
  }
  return `A incidência observada foi de ${formatarDecimal(incidencia)} por 100 mil habitantes.`;
}

function resumirComparacaoAnual(perfil) {
  const totalAtual = numero(perfil?.totalCasos ?? perfil?.total);
  const totalAnterior = somarSemanas(perfil?.semanasAnoAnterior);
  const variacao = calcularVariacaoPercentual(totalAtual, totalAnterior);

  if (!totalAnterior || variacao == null) {
    return null;
  }

  const direcao = variacao >= 0 ? "acima" : "abaixo";
  return `O acumulado ficou ${formatarPercentual(Math.abs(variacao))} ${direcao} do mesmo recorte de ${numero(perfil?.ano) - 1}.`;
}

function resumirPico(perfil) {
  const totalAtual = numero(perfil?.totalCasos ?? perfil?.total);
  const pico = calcularPico(perfil?.semanas);
  if (!pico || !totalAtual) return null;

  const participacao = percentualDoTotal(pico.casos, totalAtual);
  return `O pico ocorreu na semana ${pico.semana}, com ${formatarInteiro(pico.casos)} casos, o que representa ${formatarPercentual(participacao)} do total anual.`;
}

function resumirRitmoRecente(perfil) {
  const totalAtual = numero(perfil?.totalCasos ?? perfil?.total);
  const ultimas = calcularUltimasSemanas(perfil?.semanas, 4);
  if (!ultimas.length || !totalAtual) return null;

  const somaUltimas = ultimas.reduce((acc, item) => acc + item.casos, 0);
  const share = percentualDoTotal(somaUltimas, totalAtual);
  const semanaInicial = ultimas[0]?.semana;
  const semanaFinal = ultimas[ultimas.length - 1]?.semana;
  return `As semanas ${semanaInicial} a ${semanaFinal} concentraram ${formatarInteiro(somaUltimas)} casos, ou ${formatarPercentual(share)} do total do ano.`;
}

function resumirConcentracaoTerritorial(ranking = [], perfil = {}) {
  const concentracao = calcularConcentracaoRanking(ranking, 3);
  if (!concentracao) return null;

  const escopo = String(perfil?.uf || "").toUpperCase() === "BR" ? "estados" : "municipios";
  const nomes = concentracao.nomes.join(", ");
  const leitura = concentracao.participacao >= 45
    ? "o problema aparece mais concentrado em poucos polos"
    : "a carga parece mais espalhada pelo territorio";

  return `Os 3 ${escopo} mais afetados (${nomes}) concentram ${formatarPercentual(concentracao.participacao)} dos casos ranqueados; ${leitura}.`;
}

function resumirRisco(risco) {
  if (!risco?.classificacao) return null;
  const fatores = Array.isArray(risco?.fatores) ? risco.fatores.filter(Boolean).slice(0, 2) : [];
  const trechoFatores = fatores.length ? ` Fatores dominantes: ${fatores.join("; ")}.` : "";
  return `O risco para as proximas 2 semanas esta em ${risco.classificacao}, com score ${numero(risco.score)}/8.${trechoFatores}`;
}

function construirHeadline(perfil, risco) {
  const local = [perfil?.municipio, perfil?.uf].filter(Boolean).join("/");
  const total = formatarInteiro(perfil?.totalCasos ?? perfil?.total);
  const ano = perfil?.ano;
  const classificacao = String(perfil?.classificacao || "SEM_DADO").toLowerCase();
  const tendencia = String(perfil?.tendencia || "estavel").toLowerCase();
  const sufixoRisco = risco?.classificacao ? ` O sinal climatico atual permanece em ${String(risco.classificacao).toLowerCase()}.` : "";

  return `${local} fechou ${ano} com ${total} casos e classificacao ${classificacao}, em uma curva ${tendencia}.${sufixoRisco}`;
}

function construirMensagemClara(perfil, ranking, risco) {
  const partes = [
    fraseClassificacao(perfil?.classificacao, perfil?.incidencia100k ?? perfil?.incidencia),
    resumirComparacaoAnual(perfil),
    resumirConcentracaoTerritorial(ranking, perfil),
    resumirRisco(risco),
  ].filter(Boolean);

  return partes.join(" ");
}

export function gerarInsightsIA({ perfil, ranking = [], risco = null, textoIa = "" }) {
  const headline = construirHeadline(perfil || {}, risco);

  const achados = [
    resumirComparacaoAnual(perfil),
    resumirPico(perfil),
    resumirConcentracaoTerritorial(ranking, perfil),
    resumirRitmoRecente(perfil),
  ].filter(Boolean);

  const implicacoes = [
    fraseClassificacao(perfil?.classificacao, perfil?.incidencia100k ?? perfil?.incidencia),
    risco?.classificacao
      ? `O risco futuro em ${String(risco.classificacao).toLowerCase()} sugere que a leitura historica precisa ser acompanhada de vigilancia nas proximas semanas.`
      : null,
    achados.length >= 3 && achados[2]?.includes("concentrado")
      ? "A concentracao territorial sugere ganho operacional ao priorizar poucos focos com maior volume absoluto."
      : "A distribuicao menos concentrada sugere necessidade de vigilancia mais espalhada entre municipios ou regioes.",
  ].filter(Boolean);

  const monitoramento = [
    resumirRitmoRecente(perfil),
    perfil?.tendencia ? `A tendencia classificada como ${String(perfil.tendencia).toLowerCase()} merece ser confrontada com as semanas mais recentes, nao apenas com o total anual.` : null,
    risco?.fatores?.length ? `Vale acompanhar se os fatores climaticos atuais (${risco.fatores.slice(0, 2).join("; ")}) vao sustentar ou aliviar a transmissao observada.` : null,
  ].filter(Boolean);

  return {
    headline,
    mensagemClara: construirMensagemClara(perfil || {}, ranking, risco),
    achados: achados.slice(0, 4),
    implicacoes: implicacoes.slice(0, 3),
    monitoramento: monitoramento.slice(0, 3),
    textoBase: textoIa || "",
  };
}

export function gerarTextoNarrativoPadrao({ perfil, ranking = [], risco = null, textoIa = "" }) {
  const analise = gerarInsightsIA({ perfil, ranking, risco, textoIa });
  return [
    analise.headline,
    analise.mensagemClara,
    ...analise.achados.slice(0, 3),
    analise.monitoramento[0],
  ]
    .filter(Boolean)
    .join("\n\n");
}
