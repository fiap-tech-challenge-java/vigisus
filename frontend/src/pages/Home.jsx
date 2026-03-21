import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { buscarPorPergunta, buscarSituacaoAtual } from "../services/api";

const UFS = [
  "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
  "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
  "RS","RO","RR","SC","SP","SE","TO"
];

const DOENCAS = [
  { value: "dengue",       label: "Dengue" },
  { value: "chikungunya",  label: "Chikungunya" },
  { value: "zika",         label: "Zika" },
];

const ANOS = [2021, 2022, 2023, 2024, 2025];

export default function Home() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");

  // Formulário estruturado
  const [municipio, setMunicipio] = useState("");
  const [uf, setUf] = useState("MG");
  const [doenca, setDoenca] = useState("dengue");
  const [ano, setAno] = useState(2024);

  // Busca livre (secundária)
  const [buscaLivre, setBuscaLivre] = useState("");

  // Situação atual — cards dinâmicos
  const [situacao, setSituacao] = useState([]);

  useEffect(() => {
    buscarSituacaoAtual("MG", 6)
      .then(data => setSituacao(data?.ranking || []))
      .catch(() => {});
  }, []);

  // Cores por classificação
  const COR_BADGE = {
    EPIDEMIA: "bg-red-100 text-red-700 border-red-200",
    ALTO:     "bg-orange-100 text-orange-700 border-orange-200",
    MODERADO: "bg-yellow-100 text-yellow-700 border-yellow-200",
    BAIXO:    "bg-green-100 text-green-700 border-green-200",
  };

  const ICONE = {
    EPIDEMIA: "🔴",
    ALTO:     "🟠",
    MODERADO: "🟡",
    BAIXO:    "🟢",
  };

  const buscarEstruturado = async (e) => {
    e.preventDefault();
    if (!municipio.trim()) {
      setErro("Informe o município.");
      return;
    }
    setErro("");
    setLoading(true);
    const pergunta = `${doenca} em ${municipio} ${uf} ${ano}`;
    await executarBusca(pergunta);
  };

  const buscarLivre = async (e) => {
    e.preventDefault();
    if (!buscaLivre.trim()) return;
    setLoading(true);
    await executarBusca(buscaLivre);
  };

  const executarBusca = async (pergunta) => {
    try {
      const response = await buscarPorPergunta(pergunta);
      navigate("/resultado", { state: { dados: response.data, pergunta } });
    } catch (e) {
      setErro("Erro ao buscar dados. Tente novamente.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4">

      {/* Logo */}
      <div className="mb-8 text-center">
        <h1 className="text-4xl font-bold text-red-600">🏥 VígiSUS</h1>
        <p className="text-gray-500 mt-2">
          Vigilância epidemiológica pública do SUS
        </p>
        <p className="text-xs text-gray-400 mt-1">
          Dados: DATASUS · IBGE · Open-Meteo — sem login, sem cadastro
        </p>
      </div>

      {/* Formulário principal */}
      <div className="bg-white rounded-2xl shadow-lg p-8 w-full max-w-lg">
        <form onSubmit={buscarEstruturado}>

          {/* Linha 1: Município + Estado */}
          <div className="flex gap-3 mb-4">
            <input
              type="text"
              placeholder="Município (ex: Lavras)"
              value={municipio}
              onChange={e => setMunicipio(e.target.value)}
              className="flex-1 border border-gray-300 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            />
            <select
              value={uf}
              onChange={e => setUf(e.target.value)}
              className="w-24 border border-gray-300 rounded-lg px-3 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              {UFS.map(u => <option key={u} value={u}>{u}</option>)}
            </select>
          </div>

          {/* Linha 2: Doença + Ano */}
          <div className="flex gap-3 mb-6">
            <select
              value={doenca}
              onChange={e => setDoenca(e.target.value)}
              className="flex-1 border border-gray-300 rounded-lg px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              {DOENCAS.map(d => (
                <option key={d.value} value={d.value}>{d.label}</option>
              ))}
            </select>
            <select
              value={ano}
              onChange={e => setAno(Number(e.target.value))}
              className="w-32 border border-gray-300 rounded-lg px-3 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              {ANOS.map(a => <option key={a} value={a}>{a}</option>)}
            </select>
          </div>

          {erro && (
            <p className="text-red-500 text-sm mb-4">{erro}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3 rounded-lg text-sm transition disabled:opacity-50"
          >
            {loading ? "Buscando..." : "🔍 Buscar"}
          </button>
        </form>

        {/* Separador */}
        <div className="flex items-center my-6">
          <hr className="flex-1 border-gray-200" />
          <span className="px-3 text-xs text-gray-400">ou</span>
          <hr className="flex-1 border-gray-200" />
        </div>

        {/* Busca livre — secundária */}
        <form onSubmit={buscarLivre}>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Busca livre: ex. dengue em Campinas 2023"
              value={buscaLivre}
              onChange={e => setBuscaLivre(e.target.value)}
              className="flex-1 border border-gray-200 rounded-lg px-4 py-2 text-sm text-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-300"
            />
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm border border-gray-300 rounded-lg text-gray-500 hover:bg-gray-50 disabled:opacity-50"
            >
              →
            </button>
          </div>
        </form>
      </div>

      {/* Exemplos rápidos */}
      <div className="mt-6 flex gap-2 flex-wrap justify-center">
        {[
          ["Dengue Lavras 2024",      "dengue em Lavras MG 2024"],
          ["Dengue BH 2024",          "dengue em Belo Horizonte MG 2024"],
          ["Dengue São Paulo 2024",   "dengue em São Paulo SP 2024"],
        ].map(([label, q]) => (
          <button
            key={label}
            onClick={() => executarBusca(q)}
            disabled={loading}
            className="text-xs px-3 py-1 bg-white border border-gray-200 rounded-full text-gray-500 hover:border-red-300 hover:text-red-500 transition"
          >
            {label}
          </button>
        ))}
      </div>

      {/* Cards dinâmicos — situação atual */}
      {situacao.length > 0 && (
        <div className="mt-8 w-full max-w-lg">
          <p className="text-xs text-gray-400 uppercase tracking-wider text-center mb-3">
            Situação atual — Minas Gerais
          </p>
          <div className="grid grid-cols-2 gap-2">
            {situacao.slice(0, 6).map((m) => (
              <button
                key={m.municipio}
                onClick={() => executarBusca(`dengue em ${m.municipio} MG 2024`)}
                className={`flex items-center justify-between p-3 rounded-xl
                            border text-left hover:opacity-80 transition
                            ${COR_BADGE[m.classificacao] || COR_BADGE.BAIXO}`}
              >
                <div>
                  <p className="text-xs font-semibold">{m.municipio}</p>
                  <p className="text-xs opacity-70">
                    {m.incidencia100k?.toFixed(0)}/100k
                  </p>
                </div>
                <span className="text-lg">
                  {ICONE[m.classificacao] || "⚪"}
                </span>
              </button>
            ))}
          </div>

          {/* Alerta se tiver municípios em epidemia */}
          {(() => {
            const epidemiaCount = situacao.filter(m => m.classificacao === "EPIDEMIA").length;
            return epidemiaCount > 0 ? (
              <p className="text-xs text-red-500 text-center mt-3">
                ⚠️ {epidemiaCount} município(s)
                em situação de epidemia
              </p>
            ) : null;
          })()}
        </div>
      )}
    </div>
  );
}
