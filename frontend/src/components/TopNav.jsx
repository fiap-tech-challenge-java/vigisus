import { useEffect, useId, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAccessibility } from "../accessibility/AccessibilityProvider";

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
  const [showA11y, setShowA11y] = useState(false);
  const panelRef = useRef(null);
  const a11yButtonRef = useRef(null);

  const {
    theme,
    highContrast,
    fontPercent,
    toggleTheme,
    toggleHighContrast,
    increaseFont,
    decreaseFont,
    resetAccessibility,
  } = useAccessibility();

  const formStatusId = useId();
  const municipioId = useId();
  const ufId = useId();
  const doencaId = useId();
  const anoId = useId();
  const panelId = useId();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setMunicipio(params.get("municipio") || "");
    setUf(params.get("uf") || "BR");
    setDoenca(params.get("doenca") || "dengue");
    setAno(params.get("ano") || ANO_HISTORICO_PADRAO);
    setPendingNavigation(false);
  }, [location.search, location.pathname]);

  useEffect(() => {
    if (!showA11y) {
      return undefined;
    }

    const firstFocusable = panelRef.current?.querySelector("button");
    firstFocusable?.focus();

    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        setShowA11y(false);
      }
    };

    const onClickOutside = (event) => {
      if (
        panelRef.current &&
        !panelRef.current.contains(event.target) &&
        !a11yButtonRef.current?.contains(event.target)
      ) {
        setShowA11y(false);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("mousedown", onClickOutside);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("mousedown", onClickOutside);
    };
  }, [showA11y]);

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

  const handleSubmit = (event) => {
    event.preventDefault();
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
          aria-describedby={formStatusId}
        >
          <label className="sr-only" htmlFor={municipioId}>
            Municipio
          </label>
          <input
            id={municipioId}
            type="text"
            placeholder="Cidade..."
            value={municipio}
            onChange={(event) => setMunicipio(event.target.value)}
            disabled={isBusy}
            className="flex-1 min-w-0 border border-gray-200 rounded-lg px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-red-400 disabled:bg-gray-100 disabled:text-gray-400"
          />

          <label className="sr-only" htmlFor={ufId}>
            Estado ou Brasil
          </label>
          <select
            id={ufId}
            value={uf}
            onChange={(event) => setUf(event.target.value)}
            disabled={isBusy}
            className="w-16 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0 disabled:bg-gray-100 disabled:text-gray-400"
          >
            {UFS.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>

          <label className="sr-only" htmlFor={doencaId}>
            Doenca
          </label>
          <select
            id={doencaId}
            value={doenca}
            onChange={(event) => setDoenca(event.target.value)}
            disabled={isBusy}
            className="w-28 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0 disabled:bg-gray-100 disabled:text-gray-400"
          >
            <option value="dengue">Dengue</option>
            <option value="chikungunya">Chikungunya</option>
            <option value="zika">Zika</option>
          </select>

          {isHistorico && (
            <>
              <label className="sr-only" htmlFor={anoId}>
                Ano
              </label>
              <select
                id={anoId}
                value={ano}
                onChange={(event) => setAno(event.target.value)}
                disabled={isBusy}
                className="w-20 border border-gray-200 rounded-lg px-1.5 py-1.5 text-sm shrink-0 disabled:bg-gray-100 disabled:text-gray-400"
              >
                {ANOS.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </>
          )}

          <button
            type="submit"
            disabled={isBusy}
            className="vigi-button-primary px-3 py-1.5 rounded-lg text-sm font-medium shrink-0"
          >
            {isBusy ? "Buscando..." : "Buscar"}
          </button>
          <span id={formStatusId} aria-live="polite" className="sr-only">
            {isBusy ? "Busca em andamento" : "Busca pronta para envio"}
          </span>
        </form>

        <div className="flex gap-1 bg-gray-100 rounded-lg p-1 shrink-0" role="tablist" aria-label="Modo de consulta">
          <button
            onClick={() => goToPage("/atual")}
            disabled={isBusy}
            role="tab"
            aria-selected={isAtual}
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
            role="tab"
            aria-selected={isHistorico}
            className={`px-3 py-1.5 text-xs rounded-md transition-colors ${
              isHistorico
                ? "bg-gray-600 text-white font-semibold shadow"
                : "text-gray-600 hover:bg-white"
            } disabled:opacity-60 disabled:cursor-not-allowed`}
          >
            Historico
          </button>
        </div>

        <div className="relative shrink-0">
          <button
            ref={a11yButtonRef}
            type="button"
            onClick={() => setShowA11y((current) => !current)}
            aria-expanded={showA11y}
            aria-controls={panelId}
            aria-haspopup="dialog"
            className="px-2.5 py-1.5 border border-gray-200 rounded-lg text-xs font-semibold text-gray-700 bg-white hover:bg-gray-50"
          >
            Acessibilidade
          </button>

          {showA11y && (
            <div
              ref={panelRef}
              id={panelId}
              role="dialog"
              aria-label="Configuracoes de acessibilidade"
              aria-modal="false"
              className="absolute right-0 mt-2 w-64 vigi-card shadow-lg p-3 space-y-3 z-50"
            >
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                  Tema e contraste
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={toggleTheme}
                    aria-pressed={theme === "dark"}
                    className="flex-1 px-2 py-1.5 text-xs border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    {theme === "dark" ? "Tema claro" : "Tema escuro"}
                  </button>
                  <button
                    type="button"
                    onClick={toggleHighContrast}
                    aria-pressed={highContrast}
                    className="flex-1 px-2 py-1.5 text-xs border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    {highContrast ? "Contraste normal" : "Alto contraste"}
                  </button>
                </div>
              </div>

              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                  Fonte ({fontPercent}%)
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={decreaseFont}
                    className="flex-1 px-2 py-1.5 text-xs border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    A-
                  </button>
                  <button
                    type="button"
                    onClick={increaseFont}
                    className="flex-1 px-2 py-1.5 text-xs border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    A+
                  </button>
                  <button
                    type="button"
                    onClick={resetAccessibility}
                    className="flex-1 px-2 py-1.5 text-xs border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    Reset
                  </button>
                </div>
              </div>
              <p className="text-[11px] text-gray-500">
                Dica: pressione Esc para fechar este painel.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
