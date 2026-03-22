import { Link, useLocation, useNavigate } from "react-router-dom";

export default function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const sp = new URLSearchParams(location.search);

  const municipio = sp.get("municipio") || "";
  const uf = sp.get("uf") || "";
  const ano = sp.get("ano") || "";
  const doenca = sp.get("doenca") || "";

  const isAtual = location.pathname === "/atual";
  const isHistorico = location.pathname === "/historico";

  const buildParams = (extra = {}) => {
    const p = new URLSearchParams();
    if (municipio) p.set("municipio", municipio);
    if (uf) p.set("uf", uf);
    if (doenca) p.set("doenca", doenca);
    Object.entries(extra).forEach(([k, v]) => { if (v) p.set(k, v); });
    return p.toString();
  };

  return (
    <div className="sticky top-0 z-50 bg-white shadow-sm border-b border-gray-100">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between gap-4">

        {/* Logo */}
        <Link to="/home" className="text-red-600 font-bold text-lg shrink-0">
          🏥 VígiSUS
        </Link>

        {/* Tab toggle */}
        <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
          <button
            onClick={() => navigate(`/atual?${buildParams()}`)}
            className={`px-4 py-2 text-sm rounded-md transition-colors ${
              isAtual
                ? "bg-blue-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            }`}
          >
            Atual & Previsão
          </button>
          <button
            onClick={() => navigate(`/historico?${buildParams({ ano })}`)}
            className={`px-4 py-2 text-sm rounded-md transition-colors ${
              isHistorico
                ? "bg-gray-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            }`}
          >
            Histórico
          </button>
        </div>

        {/* Nova busca */}
        <Link
          to="/home"
          className="text-xs text-gray-500 hover:text-red-500 transition shrink-0"
        >
          ← Nova busca
        </Link>
      </div>
    </div>
  );
}
