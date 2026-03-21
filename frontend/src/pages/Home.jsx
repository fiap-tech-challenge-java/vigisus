import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { buscarPorPergunta, buscarSituacaoAtual } from "../services/api";

const UFS = [
  "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
  "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
  "RS","RO","RR","SC","SP","SE","TO"
];

const DOENCAS = [
  { value: "dengue",      label: "Dengue" },
  { value: "chikungunya", label: "Chikungunya" },
  { value: "zika",        label: "Zika" },
];

const ANOS = [2021, 2022, 2023, 2024, 2025];

const COR_CARD = {
  EPIDEMIA: { borda: "border-red-400",    fundo: "bg-red-50",    texto: "text-red-700",    icone: "🔴" },
  ALTO:     { borda: "border-orange-400", fundo: "bg-orange-50", texto: "text-orange-700", icone: "🟠" },
  MODERADO: { borda: "border-yellow-400", fundo: "bg-yellow-50", texto: "text-yellow-700", icone: "🟡" },
  BAIXO:    { borda: "border-green-400",  fundo: "bg-green-50",  texto: "text-green-700",  icone: "🟢" },
};

export default function Home() {
  const navigate = useNavigate();

  // Estado do formulário
  const [municipio, setMunicipio]   = useState("");
  const [uf, setUf]                 = useState("MG");
  const [doenca, setDoenca]         = useState("dengue");
  const [ano, setAno]               = useState(2024);
  const [buscaLivre, setBuscaLivre] = useState("");
  const [loading, setLoading]       = useState(false);
  const [erro, setErro]             = useState("");

  // Estado dos cards dinâmicos
  const [situacao, setSituacao]           = useState([]);
  const [loadingCards, setLoadingCards]   = useState(false);
  const [ufCards, setUfCards]             = useState("MG");

  // Carrega cards quando muda UF selecionada
  useEffect(() => {
    setLoadingCards(true);
    buscarSituacaoAtual(ufCards, 6)
      .then(data => {
        const lista = data?.ranking || data?.content || data || [];
        setSituacao(Array.isArray(lista) ? lista : []);
      })
      .catch(err => {
        console.warn("Erro ao carregar situação atual:", err);
        setSituacao([]);
      })
      .finally(() => setLoadingCards(false));
  }, [ufCards]);

  // ── Executa busca e navega ────────────────────────────────
  const executarBusca = async (pergunta) => {
    setErro("");
    setLoading(true);
    try {
      const response = await buscarPorPergunta(pergunta);
      navigate("/resultado", { state: { dados: response.data, pergunta } });
    } catch {
      setErro("Erro ao buscar dados. Tente novamente.");
    } finally {
      setLoading(false);
    }
  };

  // ── Formulário estruturado ────────────────────────────────
  const submitEstruturado = (e) => {
    e.preventDefault();
    if (!municipio.trim()) { setErro("Informe o município."); return; }
    executarBusca(`${doenca} em ${municipio} ${uf} ${ano}`);
  };

  // ── Clique no card de cidade crítica ─────────────────────
  const clicarCard = (card) => {
    const pergunta = `${doenca} em ${card.municipio} ${ufCards} ${ano}`;
    executarBusca(pergunta);
  };

  // ── Botões de exemplo rápido ─────────────────────────────
  const EXEMPLOS = [
    { label: "Dengue Lavras 2024",    municipio: "Lavras",        uf: "MG", doenca: "dengue", ano: 2024 },
    { label: "Dengue BH 2024",        municipio: "Belo Horizonte", uf: "MG", doenca: "dengue", ano: 2024 },
    { label: "Dengue São Paulo 2024", municipio: "São Paulo",     uf: "SP", doenca: "dengue", ano: 2024 },
  ];

  const clicarExemplo = (ex) => {
    setMunicipio(ex.municipio);
    setUf(ex.uf);
    setDoenca(ex.doenca);
    setAno(ex.ano);
    executarBusca(`${ex.doenca} em ${ex.municipio} ${ex.uf} ${ex.ano}`);
  };

  // ── Conta municípios em epidemia ─────────────────────────
  const totalEpidemia = situacao.filter(m => m.classificacao === "EPIDEMIA").length;

  // ────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center
                    justify-center p-4 pb-16">

      {/* Logo */}
      <div className="mb-8 text-center">
        <h1 className="text-4xl font-bold text-red-600">🏥 VígiSUS</h1>
        <p className="text-gray-500 mt-1">Vigilância epidemiológica pública do SUS</p>
        <p className="text-xs text-gray-400 mt-1">
          Dados: DATASUS · IBGE · Open-Meteo — sem login, sem cadastro
        </p>
      </div>

      {/* Formulário */}
      <div className="bg-white rounded-2xl shadow-lg p-8 w-full max-w-lg">
        <form onSubmit={submitEstruturado}>

          {/* Município + UF */}
          <div className="flex gap-3 mb-4">
            <input
              type="text"
              placeholder="Município (ex: Lavras)"
              value={municipio}
              onChange={e => setMunicipio(e.target.value)}
              className="flex-1 border border-gray-300 rounded-lg px-4 py-3
                         text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            />
            <select
              value={uf}
              onChange={e => setUf(e.target.value)}
              className="w-24 border border-gray-300 rounded-lg px-3 py-3
                         text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              {UFS.map(u => <option key={u} value={u}>{u}</option>)}
            </select>
          </div>

          {/* Doença + Ano */}
          <div className="flex gap-3 mb-6">
            <select
              value={doenca}
              onChange={e => setDoenca(e.target.value)}
              className="flex-1 border border-gray-300 rounded-lg px-4 py-3
                         text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              {DOENCAS.map(d => (
                <option key={d.value} value={d.value}>{d.label}</option>
              ))}
            </select>
            <select
              value={ano}
              onChange={e => setAno(Number(e.target.value))}
              className="w-32 border border-gray-300 rounded-lg px-3 py-3
                         text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              {ANOS.map(a => <option key={a} value={a}>{a}</option>)}
            </select>
          </div>

          {erro && <p className="text-red-500 text-sm mb-3">{erro}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-red-600 hover:bg-red-700 text-white font-bold
                       py-3 rounded-lg text-sm transition disabled:opacity-50
                       flex items-center justify-center gap-2"
          >
            {loading
              ? <><span className="animate-spin">⏳</span> Buscando...</>
              : "🔍 Buscar"}
          </button>
        </form>

        {/* Separador + busca livre */}
        <div className="flex items-center my-5">
          <hr className="flex-1 border-gray-200" />
          <span className="px-3 text-xs text-gray-400">ou</span>
          <hr className="flex-1 border-gray-200" />
        </div>
        <form onSubmit={e => { e.preventDefault(); executarBusca(buscaLivre); }}>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Busca livre: ex. dengue em Campinas 2023"
              value={buscaLivre}
              onChange={e => setBuscaLivre(e.target.value)}
              className="flex-1 border border-gray-200 rounded-lg px-4 py-2
                         text-sm text-gray-500 focus:outline-none focus:ring-1
                         focus:ring-gray-300"
            />
            <button type="submit" disabled={loading}
              className="px-4 py-2 text-sm border border-gray-300 rounded-lg
                         text-gray-500 hover:bg-gray-50 disabled:opacity-50">
              →
            </button>
          </div>
        </form>
      </div>

      {/* Botões de exemplo rápido — CLICÁVEIS */}
      <div className="mt-5 flex gap-2 flex-wrap justify-center">
        {EXEMPLOS.map((ex) => (
          <button
            key={ex.label}
            onClick={() => clicarExemplo(ex)}
            disabled={loading}
            className="text-xs px-3 py-1.5 bg-white border border-gray-200
                       rounded-full text-gray-500 hover:border-red-300
                       hover:text-red-600 hover:shadow-sm transition
                       disabled:opacity-50 cursor-pointer"
          >
            {ex.label}
          </button>
        ))}
      </div>

      {/* Cards dinâmicos — situação atual */}
      <div className="mt-8 w-full max-w-lg">

        {/* Header com filtro de estado */}
        <div className="flex items-center justify-between mb-3">
          <p className="text-xs text-gray-400 uppercase tracking-wider">
            Situação atual
          </p>
          <select
            value={ufCards}
            onChange={e => setUfCards(e.target.value)}
            className="text-xs border border-gray-200 rounded-lg px-2 py-1
                       text-gray-500 focus:outline-none focus:ring-1
                       focus:ring-gray-300"
          >
            {UFS.map(u => <option key={u} value={u}>{u}</option>)}
          </select>
        </div>

        {/* Alerta se tiver epidemia */}
        {totalEpidemia > 0 && (
          <div className="mb-3 flex items-center gap-2 bg-red-50 border
                          border-red-200 rounded-lg px-4 py-2">
            <span>⚠️</span>
            <p className="text-xs text-red-600 font-medium">
              {totalEpidemia} município{totalEpidemia > 1 ? "s" : ""} em
              situação de epidemia em {ufCards}
            </p>
          </div>
        )}

        {/* Grid de cards — CLICÁVEIS */}
        {loadingCards ? (
          <div className="grid grid-cols-2 gap-2">
            {[1,2,3,4,5,6].map(i => (
              <div key={i}
                className="h-20 bg-gray-100 rounded-xl animate-pulse" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-2">
            {situacao.map((m) => {
              const cor = COR_CARD[m.classificacao] || COR_CARD.BAIXO;
              return (
                <button
                  key={`${m.municipio}-${ufCards}`}
                  onClick={() => clicarCard(m)}
                  disabled={loading}
                  className={`w-full text-left p-3 rounded-xl border-l-4
                              ${cor.borda} ${cor.fundo}
                              hover:shadow-md hover:scale-[1.02]
                              transition-all cursor-pointer
                              disabled:opacity-50`}
                >
                  {/* Linha 1: nome + ícone */}
                  <div className="flex items-center justify-between mb-1">
                    <p className={`text-xs font-bold ${cor.texto} truncate`}>
                      {m.municipio}
                    </p>
                    <span className="text-sm">{cor.icone}</span>
                  </div>

                  {/* Linha 2: incidência */}
                  <p className="text-xs text-gray-500">
                    {m.incidencia100k?.toFixed(0)}/100k hab.
                  </p>

                  {/* Linha 3: abrir análise */}
                  <p className={`text-xs mt-1 font-medium ${cor.texto}`}>
                    Abrir análise →
                  </p>
                </button>
              );
            })}
          </div>
        )}

        {situacao.length === 0 && !loadingCards && (
          <p className="text-xs text-gray-400 text-center py-4">
            Nenhum dado disponível para {ufCards}
          </p>
        )}
      </div>
    </div>
  );
}
