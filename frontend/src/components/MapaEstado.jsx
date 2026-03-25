import { useEffect, useMemo, useState } from "react";
import { GeoJSON, MapContainer, TileLayer } from "react-leaflet";
import "leaflet/dist/leaflet.css";

const COR_RISCO = {
  EPIDEMIA: "#DC2626",
  ALTO: "#EA580C",
  MODERADO: "#CA8A04",
  BAIXO: "#16A34A",
  SEM_DADO: "#D1D5DB",
};

const ESTADOS = {
  AC: { cod: 12, center: [-9.0, -70.8] },
  AL: { cod: 27, center: [-9.7, -36.7] },
  AM: { cod: 13, center: [-4.0, -62.0] },
  AP: { cod: 16, center: [1.4, -51.8] },
  BA: { cod: 29, center: [-12.0, -42.0] },
  CE: { cod: 23, center: [-5.5, -39.5] },
  DF: { cod: 53, center: [-15.8, -47.9] },
  ES: { cod: 32, center: [-19.6, -40.7] },
  GO: { cod: 52, center: [-16.0, -49.6] },
  MA: { cod: 21, center: [-5.4, -45.4] },
  MG: { cod: 31, center: [-18.5, -44.5] },
  MS: { cod: 50, center: [-20.5, -54.6] },
  MT: { cod: 51, center: [-13.4, -56.0] },
  PA: { cod: 15, center: [-3.5, -52.0] },
  PB: { cod: 25, center: [-7.1, -36.6] },
  PE: { cod: 26, center: [-8.5, -37.9] },
  PI: { cod: 22, center: [-7.7, -42.6] },
  PR: { cod: 41, center: [-24.6, -51.5] },
  RJ: { cod: 33, center: [-22.3, -42.8] },
  RN: { cod: 24, center: [-5.8, -36.7] },
  RO: { cod: 11, center: [-11.0, -62.8] },
  RR: { cod: 14, center: [2.2, -61.2] },
  RS: { cod: 43, center: [-30.0, -53.0] },
  SC: { cod: 42, center: [-27.3, -50.3] },
  SE: { cod: 28, center: [-10.6, -37.4] },
  SP: { cod: 35, center: [-22.3, -48.7] },
  TO: { cod: 17, center: [-10.2, -48.4] },
};

const GEOJSON_CACHE = new Map();
const NIVEIS = ["EPIDEMIA", "ALTO", "MODERADO", "BAIXO", "SEM_DADO"];
const LABEL_NIVEL = {
  EPIDEMIA: "Epidemia",
  ALTO: "Alto",
  MODERADO: "Moderado",
  BAIXO: "Baixo",
  SEM_DADO: "Sem dado",
};

const normalizarNome = (valor) =>
  String(valor || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();

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
  const fallbackClassificacao = "SEM_DADO";

  useEffect(() => {
    if (!isBrasil && !codEstado) {
      return;
    }

    const apiUrl = isBrasil
      ? "https://raw.githubusercontent.com/codeforamerica/click_that_hood/master/public/data/brazil-states.geojson"
      : `https://raw.githubusercontent.com/tbrugz/geodata-br/master/geojson/geojs-${String(codEstado).padStart(2, "0")}-mun.json`;

    const controller = new AbortController();
    let ativo = true;

    const carregar = async () => {
      setLoading(true);
      setErro(null);

      if (GEOJSON_CACHE.has(apiUrl)) {
        if (ativo) {
          setGeojson(GEOJSON_CACHE.get(apiUrl));
          setLoading(false);
        }
        return;
      }

      try {
        const response = await fetch(apiUrl, { signal: controller.signal });
        if (!response.ok) {
          throw new Error(`Falha ao carregar GeoJSON (${response.status})`);
        }
        const data = await response.json();
        GEOJSON_CACHE.set(apiUrl, data);
        if (ativo) {
          setGeojson(data);
        }
      } catch {
        if (controller.signal.aborted) {
          return;
        }
        if (ativo) {
          setErro("Erro ao carregar mapa");
          setGeojson(null);
        }
      } finally {
        if (ativo && !controller.signal.aborted) {
          setLoading(false);
        }
      }
    };

    carregar();

    return () => {
      ativo = false;
      controller.abort();
    };
  }, [codEstado, isBrasil]);

  const classificacaoMap = useMemo(() => {
    const map = {};

    (ranking || []).forEach((item) => {
      if (!item) {
        return;
      }

      const classificacao =
        item.classificacao ??
        item.classification ??
        item.status ??
        fallbackClassificacao;

      if (!classificacao || !COR_RISCO[classificacao]) {
        return;
      }

      if (isBrasil) {
        const sigla = String(item.sgUf ?? item.uf ?? item.sigla ?? "").toUpperCase();
        if (sigla) {
          map[sigla] = classificacao;
        }
        return;
      }

      const codigo =
        item.coIbge ??
        item.co_ibge ??
        item.coMunicipio ??
        item.co_municipio ??
        item.municipioCodigo ??
        item.codigo ??
        item.ibge;

      if (codigo) {
        map[String(codigo).padStart(7, "0")] = classificacao;
      }
    });

    return map;
  }, [ranking, isBrasil]);

  const classificacaoPorNome = useMemo(() => {
    if (isBrasil) {
      return {};
    }

    const map = {};
    (ranking || []).forEach((item) => {
      const nome = item?.municipio || item?.nome || item?.nomeMunicipio;
      const classificacao =
        item?.classificacao ??
        item?.classification ??
        item?.status ??
        fallbackClassificacao;

      if (nome && classificacao && COR_RISCO[classificacao]) {
        map[normalizarNome(nome)] = classificacao;
      }
    });

    return map;
  }, [ranking, isBrasil]);

  const contagemPorNivel = useMemo(() => {
    const contagem = { EPIDEMIA: 0, ALTO: 0, MODERADO: 0, BAIXO: 0, SEM_DADO: 0 };
    Object.values(classificacaoMap).forEach((nivelClassificacao) => {
      if (contagem[nivelClassificacao] != null) {
        contagem[nivelClassificacao] += 1;
      }
    });
    return contagem;
  }, [classificacaoMap]);

  const estilizarFeature = (feature) => {
    if (!feature?.properties) {
      return {
        fillColor: COR_RISCO.SEM_DADO,
        fillOpacity: 0.2,
        color: "#CCCCCC",
        weight: 0.5,
      };
    }

    const cod = isBrasil
      ? String(feature.properties.sigla || "").toUpperCase()
      : String(feature.properties.id || feature.properties.codarea || "").padStart(7, "0");
    const nomeFeature = feature.properties.name || feature.properties.nomearea || "";
    const classificacao =
      classificacaoMap[cod] ||
      classificacaoPorNome[normalizarNome(nomeFeature)] ||
      fallbackClassificacao;
    const isDestaque = cod === String(coIbgeDestaque || "").padStart(7, "0");

    return {
      fillColor: COR_RISCO[classificacao],
      fillOpacity: classificacao === "SEM_DADO" ? 0.28 : 0.8,
      color: isDestaque ? "#1E40AF" : isBrasil ? "#FFFFFF" : "#CBD5E1",
      weight: isDestaque ? 3 : isBrasil ? 0.8 : 0.7,
      dashArray: classificacao === "SEM_DADO" ? "2,2" : "",
    };
  };

  const onEachFeature = (feature, layer) => {
    if (!feature?.properties) {
      return;
    }

    const cod = isBrasil
      ? String(feature.properties.sigla || "").toUpperCase()
      : String(feature.properties.id || feature.properties.codarea || "").padStart(7, "0");
    const nomeFeature = feature.properties.name || feature.properties.nomearea || "";
    const classificacao =
      classificacaoMap[cod] ||
      classificacaoPorNome[normalizarNome(nomeFeature)] ||
      fallbackClassificacao;

    const item = (ranking || []).find((registro) => {
      if (isBrasil) {
        const ufRegistro = String(registro.sgUf || registro.uf || registro.sigla || "").toUpperCase();
        return ufRegistro && ufRegistro === cod;
      }

      const codigoRegistro =
        registro.coIbge ||
        registro.co_ibge ||
        registro.coMunicipio ||
        registro.co_municipio ||
        registro.municipioCodigo ||
        registro.codigo ||
        registro.ibge;

      const nomeRegistro = registro.municipio || registro.nome || registro.nomeMunicipio;
      const matchCodigo = codigoRegistro && String(codigoRegistro).padStart(7, "0") === cod;
      const matchNome = nomeRegistro && normalizarNome(nomeRegistro) === normalizarNome(nomeFeature);
      return matchCodigo || matchNome;
    });

    const nome = feature.properties.name || feature.properties.nomearea || "Desconhecido";
    const tooltip = item
      ? `<div class="font-bold">${nome}</div>
         <div>${LABEL_NIVEL[classificacao] || classificacao}</div>
         <div class="text-xs text-gray-500">
           ${item.totalCasos || 0} casos - ${(item.incidencia100k ?? item.incidencia ?? 0).toFixed(1)}/100k
         </div>`
      : `<div class="font-bold">${nome}</div>
         <div>Sem dado</div>`;

    layer.bindTooltip(tooltip, { sticky: true });

    if (onRegionClick) {
      layer.on("click", () => onRegionClick(cod, item));
    }
  };

  if (loading) {
    return (
      <div className="h-80 bg-gray-100 rounded-xl flex items-center justify-center" role="status" aria-live="polite">
        <p className="text-sm text-gray-400 animate-pulse">Carregando mapa...</p>
      </div>
    );
  }

  if (erro || !geojson) {
    return (
      <div className="h-80 bg-gray-50 rounded-xl flex flex-col items-center justify-center border border-gray-200" role="alert">
        <p className="text-sm text-gray-400">{erro || "Mapa indisponivel"}</p>
        <p className="text-xs text-gray-300 mt-1">Tente recarregar a pagina</p>
      </div>
    );
  }

  return (
    <div>
      <div className="rounded-xl overflow-hidden shadow-sm border border-gray-200" style={{ height: 420 }} aria-hidden="true">
        <MapContainer
          center={center}
          zoom={zoom}
          preferCanvas
          style={{ height: "100%", width: "100%" }}
          scrollWheelZoom={false}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution="OpenStreetMap"
            opacity={0.25}
          />
          <GeoJSON data={geojson} style={estilizarFeature} onEachFeature={onEachFeature} />
        </MapContainer>
      </div>

      <section className="mt-4 vigi-card p-4" aria-label="Legenda do mapa epidemiologico">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Legenda do mapa</h3>
        <ul className="grid grid-cols-2 md:grid-cols-5 gap-2">
          {NIVEIS.map((nivelItem) => (
            <li key={nivelItem} className="flex items-center gap-2 text-xs text-gray-700">
              <span
                className="inline-block w-3.5 h-3.5 rounded border border-gray-300"
                style={{ backgroundColor: COR_RISCO[nivelItem] }}
                aria-hidden="true"
              />
              <span>{LABEL_NIVEL[nivelItem]}</span>
            </li>
          ))}
        </ul>
        <p className="text-xs text-gray-500 mt-3">
          Modo: {isBrasil ? "Brasil (por estado)" : `Estado ${ufUpper} (por municipio)`}. Regioes com classificacao:{" "}
          {Object.keys(classificacaoMap).length}.
        </p>
      </section>

      <section className="mt-3 vigi-card p-4" aria-label="Resumo textual do mapa">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Resumo textual para acessibilidade</h3>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-2 text-xs">
          {NIVEIS.map((nivelItem) => (
            <div key={nivelItem} className="px-3 py-2 rounded-md bg-gray-50 border border-gray-200">
              <p className="font-semibold text-gray-700">{LABEL_NIVEL[nivelItem]}</p>
              <p className="text-gray-500">{contagemPorNivel[nivelItem] || 0} regioes</p>
            </div>
          ))}
        </div>
      </section>

      {risco && (
        <section className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg" aria-label="Risco futuro agregado">
          <h3 className="text-sm font-semibold text-blue-900 mb-2">
            Risco Futuro (14 dias)
          </h3>
          <div className="flex flex-col gap-2 text-sm text-blue-800">
            <p>
              <span className="font-medium">Classificacao:</span> {risco.classificacao}
            </p>
            <p>
              <span className="font-medium">Score:</span> {risco.score}/8
            </p>
            {risco.fatores && risco.fatores.length > 0 && (
              <div>
                <p className="font-medium">Fatores:</p>
                <ul className="ml-4 mt-1 list-disc text-xs">
                  {risco.fatores.slice(0, 3).map((fator, index) => (
                    <li key={index}>{fator}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
