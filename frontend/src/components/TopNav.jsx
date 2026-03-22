import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

const UFS = ["AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
             "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
             "RS","RO","RR","SC","SP","SE","TO"];
const ANOS = [2024, 2023, 2022, 2021, 2020, 2019, 2018];

export default function TopNav() {
  const location  = useLocation();
  const navigate  = useNavigate();
  const sp        = new URLSearchParams(location.search);

  const isAtual     = location.pathname === "/atual";
  const isHistorico = location.pathname === "/historico";

  const [municipio, setMunicipio] = useState(sp.get("municipio") || "");
  const [uf,        setUf]        = useState(sp.get("uf")        || "MG");
  const [doenca,    setDoenca]    = useState(sp.get("doenca")    || "dengue");
  const [ano,       setAno]       = useState(sp.get("ano")       || "2024");

  const buildParams = (overrides = {}) => {
    const p = new URLSearchParams({ uf, doenca });
    if (municipio.trim()) p.set("municipio", municipio.trim());
    if (isHistorico) p.set("ano", ano);
    Object.entries(overrides).forEach(([k, v]) => { if (v) p.set(k, v); });
    return p.toString();
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    navigate(`${location.pathname}?${buildParams()}`);
  };

  const goToPage = (page) => {
    const p = new URLSearchParams({ uf, doenca });
    if (municipio.trim()) p.set("municipio", municipio.trim());
    if (page === "/historico") p.set("ano", ano || "2024");
    navigate(`${page}?${p}`);
  };

  return (
    <div className="sticky top-0 z-50 bg-white shadow-sm border-b border-gray-100">
      <div className="max-w-6xl mx-auto px-3 py-2 flex items-center gap-2">

        {/* Logo */}
        <Link to="/atual" className="text-red-600 font-bold text-base shrink-0 mr-1">
          🏥 VígiSUS
        </Link>

        {/* Search form inline */}
        <form onSubmit={handleSubmit}
          className="flex flex-1 gap-1.5 items-center min-w-0">
          <input
            type="text"
            placeholder="Cidade..."
            value={municipio}
            onChange={e => setMunicipio(e.target.value)}
            className="flex-1 min-w-0 border border-gray-200 rounded-lg px-2.5 py-1.5
                       text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
          />
          <select value={uf} onChange={e => setUf(e.target.value)}
            className="w-16 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0">
            {UFS.map(u => <option key={u} value={u}>{u}</option>)}
          </select>
          <select value={doenca} onChange={e => setDoenca(e.target.value)}
            className="w-28 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0">
            <option value="dengue">Dengue</option>
            <option value="chikungunya">Chikungunya</option>
            <option value="zika">Zika</option>
          </select>
          {isHistorico && (
            <select value={ano} onChange={e => setAno(e.target.value)}
              className="w-20 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0">
              {ANOS.map(a => <option key={a} value={a}>{a}</option>)}
            </select>
          )}
          <button type="submit"
            className="bg-red-600 hover:bg-red-700 text-white px-3 py-1.5
                       rounded-lg text-sm font-medium shrink-0">
            Buscar
          </button>
        </form>

        {/* Tab toggle */}
        <div className="flex gap-1 bg-gray-100 rounded-lg p-1 shrink-0">
          <button
            onClick={() => goToPage("/atual")}
            className={`px-3 py-1.5 text-xs rounded-md transition-colors ${
              isAtual
                ? "bg-blue-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            }`}
          >
            Atual
          </button>
          <button
            onClick={() => goToPage("/historico")}
            className={`px-3 py-1.5 text-xs rounded-md transition-colors ${
              isHistorico
                ? "bg-gray-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            }`}
          >
            Histórico
          </button>
        </div>
      </div>
    </div>
  );
}
