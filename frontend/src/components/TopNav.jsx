import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

const UFS = [
  "BR", "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
  "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
  "RS", "RO", "RR", "SC", "SP", "SE", "TO",
];
const ANO_ATUAL = new Date().getFullYear();
const ANO_HISTORICO_PADRAO = String(ANO_ATUAL - 1);
const ANOS = Array.from({ length: 8 }, (_, i) => ANO_ATUAL - 1 - i);

export default function TopNav({ loading = false }) {
  const location = useLocation();
  const navigate = useNavigate();
  const sp = new URLSearchParams(location.search);

  const isAtual = location.pathname === "/atual";
  const isHistorico = location.pathname === "/historico";

  const [municipio, setMunicipio] = useState(sp.get("municipio") || "");
  const [uf, setUf] = useState(sp.get("uf") || "BR");
  const [doenca, setDoenca] = useState(sp.get("doenca") || "dengue");
  const [ano, setAno] = useState(sp.get("ano") || ANO_HISTORICO_PADRAO);
  const [pendingNavigation, setPendingNavigation] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setMunicipio(params.get("municipio") || "");
    setUf(params.get("uf") || "BR");
    setDoenca(params.get("doenca") || "dengue");
    setAno(params.get("ano") || ANO_HISTORICO_PADRAO);
    setPendingNavigation(false);
  }, [location.search, location.pathname]);

  const isBusy = loading || pendingNavigation;

  const buildParams = () => {
    const p = new URLSearchParams({ uf, doenca });
    if (municipio.trim()) {
      p.set("municipio", municipio.trim());
    }
    if (isHistorico) {
      p.set("ano", ano || ANO_HISTORICO_PADRAO);
    }
    return p.toString();
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isBusy) {
      return;
    }

    const nextQuery = buildParams();
    const currentQuery = location.search.replace(/^\?/, "");
    if (nextQuery === currentQuery) {
      return;
    }

    setPendingNavigation(true);
    navigate(`${location.pathname}?${nextQuery}`);
  };

  const goToPage = (page) => {
    if (isBusy) {
      return;
    }

    const p = new URLSearchParams({ uf, doenca });
    if (municipio.trim()) {
      p.set("municipio", municipio.trim());
    }
    if (page === "/historico") {
      p.set("ano", ano || ANO_HISTORICO_PADRAO);
    }

    const nextQuery = p.toString();
    const currentQuery = location.search.replace(/^\?/, "");
    if (page === location.pathname && nextQuery === currentQuery) {
      return;
    }

    setPendingNavigation(true);
    navigate(`${page}?${nextQuery}`);
  };

  return (
    <div className="sticky top-0 z-50 bg-white shadow-sm border-b border-gray-100">
      <div className="max-w-6xl mx-auto px-3 py-2 flex items-center gap-2">
        <Link to="/atual" className="text-red-600 font-bold text-base shrink-0 mr-1">
          VigiSUS
        </Link>

        <form
          onSubmit={handleSubmit}
          className="flex flex-1 gap-1.5 items-center min-w-0"
          aria-busy={isBusy}
        >
          <input
            type="text"
            placeholder="Cidade..."
            value={municipio}
            onChange={(e) => setMunicipio(e.target.value)}
            disabled={isBusy}
            className="flex-1 min-w-0 border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-red-400 disabled:bg-gray-100 disabled:text-gray-400"
          />

          <select
            value={uf}
            onChange={(e) => setUf(e.target.value)}
            disabled={isBusy}
            className="w-16 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0 disabled:bg-gray-100 disabled:text-gray-400"
          >
            {UFS.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>

          <select
            value={doenca}
            onChange={(e) => setDoenca(e.target.value)}
            disabled={isBusy}
            className="w-28 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0 disabled:bg-gray-100 disabled:text-gray-400"
          >
            <option value="dengue">Dengue</option>
            <option value="chikungunya">Chikungunya</option>
            <option value="zika">Zika</option>
          </select>

          {isHistorico && (
            <select
              value={ano}
              onChange={(e) => setAno(e.target.value)}
              disabled={isBusy}
              className="w-20 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0 disabled:bg-gray-100 disabled:text-gray-400"
            >
              {ANOS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          )}

          <button
            type="submit"
            disabled={isBusy}
            className="vigi-button-primary px-3 py-1.5 rounded-lg text-sm font-medium shrink-0"
          >
            {isBusy ? "Buscando..." : "Buscar"}
          </button>
        </form>

        <div className="flex gap-1 bg-gray-100 rounded-lg p-1 shrink-0">
          <button
            onClick={() => goToPage("/atual")}
            disabled={isBusy}
            className={`px-3 py-1.5 text-xs rounded-md transition-colors ${
              isAtual
                ? "bg-blue-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            } disabled:opacity-60 disabled:cursor-not-allowed`}
          >
            Atual
          </button>
          <button
            onClick={() => goToPage("/historico")}
            disabled={isBusy}
            className={`px-3 py-1.5 text-xs rounded-md transition-colors ${
              isHistorico
                ? "bg-gray-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            } disabled:opacity-60 disabled:cursor-not-allowed`}
          >
            Historico
          </button>
        </div>
      </div>
    </div>
  );
}
