import { useEffect, useMemo, useState } from "react";
import { MapContainer, TileLayer, GeoJSON } from "react-leaflet";
import "leaflet/dist/leaflet.css";

// ─────────────────────────────────────────────────────────────
// CORES POR RISCO EPIDEMIOLÓGICO
// ─────────────────────────────────────────────────────────────
const COR_RISCO = {
  EPIDEMIA:  "#DC2626",  // 🔴 vermelho vivo
  ALTO:      "#EA580C",  // 🟠 laranja vivo
  MODERADO:  "#CA8A04",  // 🟡 amarelo ouro
  BAIXO:     "#16A34A",  // 🟢 verde vivo
  SEM_DADO:  "#D1D5DB",  // ⚪ cinza claro
};

// Emojis para legenda interativa
const EMOJI_RISCO = {
  EPIDEMIA:  "🔴",
  ALTO:      "🟠",
  MODERADO:  "🟡",
  BAIXO:     "🟢",
  SEM_DADO:  "⚪",
};

const normalizarNome = (valor) =>
  String(valor || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();

// ─────────────────────────────────────────────────────────────
// CÓDIGOS E CENTROS DOS ESTADOS
// ─────────────────────────────────────────────────────────────
const ESTADOS = {
  AC: { cod: 12, center: [-9.0,  -70.8], nome: "Acre" },
  AL: { cod: 27, center: [-9.7,  -36.7], nome: "Alagoas" },
  AM: { cod: 13, center: [-4.0,  -62.0], nome: "Amazonas" },
  AP: { cod: 16, center: [1.4,   -51.8], nome: "Amapá" },
  BA: { cod: 29, center: [-12.0, -42.0], nome: "Bahia" },
  CE: { cod: 23, center: [-5.5,  -39.5], nome: "Ceará" },
  DF: { cod: 53, center: [-15.8, -47.9], nome: "Distrito Federal" },
  ES: { cod: 32, center: [-19.6, -40.7], nome: "Espírito Santo" },
  GO: { cod: 52, center: [-16.0, -49.6], nome: "Goiás" },
  MA: { cod: 21, center: [-5.4,  -45.4], nome: "Maranhão" },
  MG: { cod: 31, center: [-18.5, -44.5], nome: "Minas Gerais" },
  MS: { cod: 50, center: [-20.5, -54.6], nome: "Mato Grosso do Sul" },
  MT: { cod: 51, center: [-13.4, -56.0], nome: "Mato Grosso" },
  PA: { cod: 15, center: [-3.5,  -52.0], nome: "Pará" },
  PB: { cod: 25, center: [-7.1,  -36.6], nome: "Paraíba" },
  PE: { cod: 26, center: [-8.5,  -37.9], nome: "Pernambuco" },
  PI: { cod: 22, center: [-7.7,  -42.6], nome: "Piauí" },
  PR: { cod: 41, center: [-24.6, -51.5], nome: "Paraná" },
  RJ: { cod: 33, center: [-22.3, -42.8], nome: "Rio de Janeiro" },
  RN: { cod: 24, center: [-5.8,  -36.7], nome: "Rio Grande do Norte" },
  RO: { cod: 11, center: [-11.0, -62.8], nome: "Rondônia" },
  RR: { cod: 14, center: [2.2,   -61.2], nome: "Roraima" },
  RS: { cod: 43, center: [-30.0, -53.0], nome: "Rio Grande do Sul" },
  SC: { cod: 42, center: [-27.3, -50.3], nome: "Santa Catarina" },
  SE: { cod: 28, center: [-10.6, -37.4], nome: "Sergipe" },
  SP: { cod: 35, center: [-22.3, -48.7], nome: "São Paulo" },
  TO: { cod: 17, center: [-10.2, -48.4], nome: "Tocantins" },
};

// ─────────────────────────────────────────────────────────────
// COMPONENTE PRINCIPAL
// ─────────────────────────────────────────────────────────────
export default function MapaEstado({ 
  uf = "MG", 
  nivel = "estado",
  coIbgeDestaque, 
  ranking = [],
  risco = null,
  onRegionClick = null,
}) {
  const [geojson, setGeojson] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);

  const ufUpper = (uf || "").toUpperCase();
  const isBrasil = ufUpper === "BR" || nivel === "brasil";
  const estado = ESTADOS[ufUpper] || ESTADOS.MG;
  const codEstado = estado.cod;
  const center = isBrasil ? [-14.2, -52.0] : estado.center;
  const zoom = isBrasil ? 4 : 6;

  // ─────────────────────────────────────────────────────────────
  // 1. CARREGAR GEOJSON DO IBGE
  // ─────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!isBrasil && !codEstado) return;

    setLoading(true);
    setErro(null);

    const apiUrl = isBrasil
      ? "https://raw.githubusercontent.com/codeforamerica/click_that_hood/master/public/data/brazil-states.geojson"
      : `https://raw.githubusercontent.com/tbrugz/geodata-br/master/geojson/geojs-${String(codEstado).padStart(2, "0")}-mun.json`;

    fetch(apiUrl)
      .then(r => r.json())
      .then(data => {
        setGeojson(data);
        setErro(null);
      })
      .catch(err => {
        console.error("[MapaEstado] Erro ao carregar GeoJSON:", err);
        setErro("Erro ao carregar mapa");
        setGeojson(null);
      })
      .finally(() => setLoading(false));
  }, [codEstado, isBrasil]);

  // ─────────────────────────────────────────────────────────────
  // 2. CONSTRUIR MAPA DE CLASSIFICAÇÕES
  // ─────────────────────────────────────────────────────────────
  const fallbackClassif = "SEM_DADO";

  const classificacaoMap = useMemo(() => {
    const map = {};

    (ranking || []).forEach(item => {
      if (!item) return;

      const classif =
        item.classificacao ??
        item.classification ??
        item.status ??
        fallbackClassif;

      if (!classif || !COR_RISCO[classif]) return;

      if (isBrasil) {
        const sigla = String(item.sgUf ?? item.uf ?? item.sigla ?? "").toUpperCase();
        if (sigla) {
          map[sigla] = classif;
        }
        return;
      }

      const coIbge =
        item.coIbge ??
        item.co_ibge ??
        item.coMunicipio ??
        item.co_municipio ??
        item.municipioCodigo ??
        item.codigo ??
        item.ibge;

      if (coIbge) {
        map[String(coIbge).padStart(7, "0")] = classif;
      }
    });

    return map;
  }, [ranking, isBrasil]);

  const classificacaoPorNome = useMemo(() => {
    if (isBrasil) return {};

    const map = {};
    (ranking || []).forEach((item) => {
      const nome = item?.municipio || item?.nome || item?.nomeMunicipio;
      const classif = item?.classificacao ?? item?.classification ?? item?.status ?? fallbackClassif;
      if (nome && classif && COR_RISCO[classif]) {
        map[normalizarNome(nome)] = classif;
      }
    });
    return map;
  }, [ranking, isBrasil]);

  // ─────────────────────────────────────────────────────────────
  // 3. DEBUG LOGS
  // ─────────────────────────────────────────────────────────────
  useEffect(() => {
    if (ranking?.length > 0) {
      const amostra = ranking.slice(0, 2);
      console.log("[MapaEstado] Amostra do ranking:", amostra);
      console.log("[MapaEstado] Classificações mapeadas:", 
        Object.entries(classificacaoMap).slice(0, 5));
      console.log("[MapaEstado] Total com classificação:", 
        Object.keys(classificacaoMap).length, "de", ranking.length);
    }
    if (risco) {
      console.log("[MapaEstado] Risco agregado recebido:", risco);
    }
  }, [ranking, classificacaoMap, risco]);

  // ─────────────────────────────────────────────────────────────
  // 4. ESTILIZAR FEATURES
  // ─────────────────────────────────────────────────────────────
  const estilizarFeature = (feature) => {
    if (!feature?.properties) {
      return {
        fillColor: COR_RISCO.SEM_DADO,
        fillOpacity: 0.2,
        color: "#CCCCCC",
        weight: 0.5,
      };
    }

    // GeoJSON do IBGE tem código em `codarea` (7 dígitos)
    const cod = isBrasil
      ? String(feature.properties.sigla || "").toUpperCase()
      : String(feature.properties.id || feature.properties.codarea || "").padStart(7, "0");
    const nomeFeature = feature.properties.name || feature.properties.nomearea || "";
    const classif = classificacaoMap[cod] || classificacaoPorNome[normalizarNome(nomeFeature)] || fallbackClassif;
    const isDestaque = cod === String(coIbgeDestaque || "").padStart(7, "0");

    return {
      fillColor: COR_RISCO[classif],
      fillOpacity: classif === "SEM_DADO" ? 0.28 : 0.8,
      color: isDestaque ? "#1E40AF" : (isBrasil ? "#FFFFFF" : "#CBD5E1"),
      weight: isDestaque ? 3 : (isBrasil ? 0.8 : 0.7),
      dashArray: classif === "SEM_DADO" ? "2,2" : "",
    };
  };

  // ─────────────────────────────────────────────────────────────
  // 5. INTERAÇÃO COM FEATURES
  // ─────────────────────────────────────────────────────────────
  const onEachFeature = (feature, layer) => {
    if (!feature?.properties) return;

    const cod = isBrasil
      ? String(feature.properties.sigla || "").toUpperCase()
      : String(feature.properties.id || feature.properties.codarea || "").padStart(7, "0");
    const nomeFeature = feature.properties.name || feature.properties.nomearea || "";
    const classif = classificacaoMap[cod] || classificacaoPorNome[normalizarNome(nomeFeature)] || fallbackClassif;

    // Buscar dados do municipality/region no ranking
    const item = (ranking || []).find(m => {
      if (isBrasil) {
        const mUf = String(m.sgUf || m.uf || m.sigla || "").toUpperCase();
        return mUf && mUf === cod;
      }

      const mCod =
        m.coIbge || m.co_ibge || m.coMunicipio || m.co_municipio ||
        m.municipioCodigo || m.codigo || m.ibge;
      const nomeM = m.municipio || m.nome || m.nomeMunicipio;
      const matchCodigo = mCod && String(mCod).padStart(7, "0") === cod;
      const matchNome = nomeM && normalizarNome(nomeM) === normalizarNome(nomeFeature);
      return matchCodigo || matchNome;
    });

    // Tooltip
    const nome = feature.properties.name || feature.properties.nomearea || "Desconhecido";
    const tooltip = item
      ? `<div class="font-bold">${nome}</div>
         <div>${EMOJI_RISCO[classif]} ${classif}</div>
         <div class="text-xs text-gray-500">
           ${item.totalCasos || 0} casos · ${(item.incidencia100k ?? item.incidencia ?? 0).toFixed(1)}/100k
         </div>`
      : `<div class="font-bold">${nome}</div>
         <div>⚪ SEM_DADO</div>`;

    layer.bindTooltip(tooltip, { sticky: true });

    // Click para navegação entre níveis
    if (onRegionClick) {
      layer.on("click", () => onRegionClick(cod, item));
    }
  };

  // ─────────────────────────────────────────────────────────────
  // 6. RENDER
  // ─────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="h-80 bg-gray-100 rounded-xl flex items-center justify-center">
        <p className="text-sm text-gray-400 animate-pulse">
          🗺️ Carregando mapa...
        </p>
      </div>
    );
  }

  if (erro || !geojson) {
    return (
      <div className="h-80 bg-gray-50 rounded-xl flex flex-col items-center justify-center border border-gray-200">
        <p className="text-sm text-gray-400">⚠️ {erro || "Mapa indisponível"}</p>
        <p className="text-xs text-gray-300 mt-1">Tente recarregar a página</p>
      </div>
    );
  }

  return (
    <div>
      {/* MAPA */}
      <div className="rounded-xl overflow-hidden shadow-sm border border-gray-200" style={{ height: 420 }}>
        <MapContainer
          center={center}
          zoom={zoom}
          style={{ height: "100%", width: "100%" }}
          scrollWheelZoom={false}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution="© OpenStreetMap"
            opacity={0.25}
          />
          <GeoJSON
            key={JSON.stringify(classificacaoMap)}
            data={geojson}
            style={estilizarFeature}
            onEachFeature={onEachFeature}
          />
        </MapContainer>
      </div>

      {/* LEGENDA */}
      <div className="flex gap-3 justify-center mt-4 flex-wrap px-4 py-2 bg-white rounded-lg">
        {["EPIDEMIA", "ALTO", "MODERADO", "BAIXO", "SEM_DADO"].map((nivel) => (
          <div
            key={nivel}
            className="flex items-center gap-2 text-xs font-medium text-gray-700"
          >
            <span>{EMOJI_RISCO[nivel]}</span>
            <div
              className="w-4 h-4 rounded border border-gray-300"
              style={{ backgroundColor: COR_RISCO[nivel] }}
            />
            <span>{nivel}</span>
          </div>
        ))}
      </div>

      <div className="mt-2 text-xs text-gray-500 text-center">
        Modo: {isBrasil ? "Brasil (por estado)" : `Estado ${ufUpper} (por município)`} ·
        geometrias: {geojson?.features?.length || 0} ·
        com classificação: {Object.keys(classificacaoMap).length}
      </div>

      {/* INFO */}
      {Object.keys(classificacaoMap).length === 0 && ranking?.length > 0 && !risco && (
        <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded text-xs text-yellow-700">
          ⚠️ Dados carregados mas nenhuma classificação encontrada. Verifique os dados.
        </div>
      )}
      
      {risco && (
        <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <div className="text-sm font-semibold text-blue-900 mb-2">
            📊 Risco Futuro (14 dias)
          </div>
          <div className="flex flex-col gap-3 text-sm text-blue-800">
            <div>
              <span className="font-medium">Classificação:</span> {EMOJI_RISCO[risco.classificacao]} {risco.classificacao}
            </div>
            <div>
              <span className="font-medium">Score:</span> {risco.score}/8
            </div>
            {risco.fatores && risco.fatores.length > 0 && (
              <div>
                <span className="font-medium">Fatores:</span>
                <ul className="ml-4 mt-1 list-disc text-xs">
                  {risco.fatores.slice(0, 3).map((f, i) => (
                    <li key={i}>{f}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
