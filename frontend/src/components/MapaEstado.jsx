import { useEffect, useState } from "react";
import { MapContainer, TileLayer, GeoJSON } from "react-leaflet";
import "leaflet/dist/leaflet.css";

// Cores por classificação
const COR_MAPA = {
  EPIDEMIA: "#DC2626",  // vermelho
  ALTO:     "#EA580C",  // laranja
  MODERADO: "#CA8A04",  // amarelo
  BAIXO:    "#16A34A",  // verde
  SEM_DADO: "#D1D5DB",  // cinza
};

// Código numérico e coordenadas centrais aproximadas por UF
const ESTADOS = {
  AC: { cod: 12, center: [-9.0,  -70.8] },
  AL: { cod: 27, center: [-9.7,  -36.7] },
  AM: { cod: 13, center: [-4.0,  -62.0] },
  AP: { cod: 16, center: [1.4,   -51.8] },
  BA: { cod: 29, center: [-12.0, -42.0] },
  CE: { cod: 23, center: [-5.5,  -39.5] },
  DF: { cod: 53, center: [-15.8, -47.9] },
  ES: { cod: 32, center: [-19.6, -40.7] },
  GO: { cod: 52, center: [-16.0, -49.6] },
  MA: { cod: 21, center: [-5.4,  -45.4] },
  MG: { cod: 31, center: [-18.5, -44.5] },
  MS: { cod: 50, center: [-20.5, -54.6] },
  MT: { cod: 51, center: [-13.4, -56.0] },
  PA: { cod: 15, center: [-3.5,  -52.0] },
  PB: { cod: 25, center: [-7.1,  -36.6] },
  PE: { cod: 26, center: [-8.5,  -37.9] },
  PI: { cod: 22, center: [-7.7,  -42.6] },
  PR: { cod: 41, center: [-24.6, -51.5] },
  RJ: { cod: 33, center: [-22.3, -42.8] },
  RN: { cod: 24, center: [-5.8,  -36.7] },
  RO: { cod: 11, center: [-11.0, -62.8] },
  RR: { cod: 14, center: [2.2,   -61.2] },
  RS: { cod: 43, center: [-30.0, -53.0] },
  SC: { cod: 42, center: [-27.3, -50.3] },
  SE: { cod: 28, center: [-10.6, -37.4] },
  SP: { cod: 35, center: [-22.3, -48.7] },
  TO: { cod: 17, center: [-10.2, -48.4] },
};

export default function MapaEstado({ uf = "MG", coIbgeDestaque, ranking }) {
  const [geojson, setGeojson] = useState(null);

  const estado = ESTADOS[uf] || ESTADOS.MG;
  const codEstado = estado.cod;
  const center = estado.center;

  useEffect(() => {
    // Busca GeoJSON de municípios do estado no IBGE
    fetch(
      `https://servicodados.ibge.gov.br/api/v3/malhas/estados/${codEstado}` +
      `?formato=application/vnd.geo+json&resolucao=5&qualidade=minima`
    )
      .then(r => r.json())
      .then(setGeojson)
      .catch(() => {});
  }, [codEstado]);

  // Monta mapa de classificação por coIbge (normalizado para 7 dígitos)
  const classificacaoMap = {};
  (ranking || []).forEach(item => {
    const cod =
      item.coIbge      ?? item.co_ibge     ?? item.coMunicipio ??
      item.co_municipio ?? item.codigo     ?? item.ibge;

    const classif =
      item.classificacao ?? item.classification ?? item.status;

    if (cod && classif) {
      classificacaoMap[String(cod).padStart(7, "0")] = classif;
    }
  });

  useEffect(() => {
    if (ranking?.length > 0) {
      console.log("Ranking item[0]:", ranking[0]);
      console.log("classificacaoMap sample:",
        Object.entries(classificacaoMap).slice(0, 3));
    }
  }, [ranking]); // eslint-disable-line react-hooks/exhaustive-deps

  const estilizarFeature = (feature) => {
    // O GeoJSON do IBGE tem o código do município em properties.codarea (7 dígitos)
    const cod = String(feature?.properties?.codarea || "").padStart(7, '0');
    const classif = classificacaoMap[cod] || "SEM_DADO";
    const isDestaque = cod === String(coIbgeDestaque || "").padStart(7, '0');

    return {
      fillColor: COR_MAPA[classif] || COR_MAPA.SEM_DADO,
      fillOpacity: classif === "SEM_DADO" ? 0.15 : 0.75,
      color: isDestaque ? "#1E40AF" : "#FFFFFF",
      weight: isDestaque ? 3 : 0.5,
    };
  };

  const onEachFeature = (feature, layer) => {
    const cod = String(feature?.properties?.codarea || "").padStart(7, '0');
    const classif = classificacaoMap[cod];
    const municipio = (ranking || []).find(m => {
      const mCod =
        m.coIbge || m.co_ibge || m.coMunicipio || m.co_municipio ||
        m.municipioCodigo || m.codigo || m.ibge;
      return mCod && String(mCod).padStart(7, '0') === cod;
    });

    if (municipio) {
      layer.bindTooltip(
        `<strong>${municipio.municipio}</strong><br/>
         ${classif}<br/>
         Incidência: ${municipio.incidencia100k?.toFixed(0)}/100k`,
        { sticky: true }
      );
    }
  };

  if (!geojson) return (
    <div className="h-64 bg-gray-100 rounded-xl flex items-center justify-center">
      <p className="text-sm text-gray-400">Carregando mapa do estado...</p>
    </div>
  );

  return (
    <div>
      <div className="rounded-xl overflow-hidden shadow" style={{ height: 400 }}>
        <MapContainer
          center={center}
          zoom={6}
          style={{ height: "100%", width: "100%" }}
          scrollWheelZoom={false}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution="© OpenStreetMap"
            opacity={0.3}
          />
          <GeoJSON
            key={JSON.stringify(classificacaoMap)}
            data={geojson}
            style={estilizarFeature}
            onEachFeature={onEachFeature}
          />
        </MapContainer>
      </div>

      {/* Legenda */}
      <div className="flex gap-4 justify-center mt-3 flex-wrap">
        {Object.entries(COR_MAPA).filter(([k]) => k !== "SEM_DADO").map(([key, cor]) => (
          <div key={key} className="flex items-center gap-1.5 text-xs text-gray-600">
            <div className="w-3 h-3 rounded-full" style={{ backgroundColor: cor }} />
            {key}
          </div>
        ))}
        <div className="flex items-center gap-1.5 text-xs text-gray-400">
          <div className="w-3 h-3 rounded-full bg-gray-300" />
          Sem dado
        </div>
      </div>
    </div>
  );
}
