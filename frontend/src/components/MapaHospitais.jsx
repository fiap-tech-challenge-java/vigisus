import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import { useEffect } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Fix default icon paths broken by webpack
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// Ícones customizados
const ICONE_ORIGEM = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png",
  shadowUrl: markerShadow,
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
  shadowSize: [41, 41],
});
const ICONE_HOSPITAL = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
  shadowUrl: markerShadow,
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

// Ajusta zoom para mostrar todos os marcadores
function FitBounds({ coords }) {
  const map = useMap();
  useEffect(() => {
    if (coords.length > 1) {
      map.fitBounds(coords, { padding: [40, 40] });
    }
  }, [coords, map]);
  return null;
}

// Default coordinates for Brasília/DF when municipality lat/lon is unavailable
const DEFAULT_LAT = -15.78;
const DEFAULT_LON = -47.93;

export default function MapaHospitais({ perfil, encaminhamento }) {
  const hospitais = encaminhamento?.hospitais || [];

  const latOrigem = perfil?.nuLatitude  || DEFAULT_LAT;
  const lonOrigem = perfil?.nuLongitude || DEFAULT_LON;

  const coords = [
    [latOrigem, lonOrigem],
    ...hospitais
      .filter(h => h.nuLatitude && h.nuLongitude)
      .map(h => [h.nuLatitude, h.nuLongitude])
  ];

  return (
    <div className="mx-6 max-w-6xl md:mx-auto mt-4">
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
        🏥 Hospitais com estrutura disponível
      </h2>

      <div className="flex flex-col md:flex-row gap-4">

        {/* MAPA */}
        <div className="flex-1 rounded-xl overflow-hidden shadow" style={{ height: 380 }}>
          <MapContainer
            center={[latOrigem, lonOrigem]}
            zoom={8}
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution="© OpenStreetMap"
            />
            <FitBounds coords={coords} />

            {/* Marcador do município */}
            <Marker position={[latOrigem, lonOrigem]} icon={ICONE_ORIGEM}>
              <Popup>
                <strong>{perfil?.municipio}</strong><br/>
                Município buscado
              </Popup>
            </Marker>

            {/* Marcadores dos hospitais */}
            {hospitais.filter(h => h.nuLatitude).map((h, i) => (
              <Marker
                key={i}
                position={[h.nuLatitude, h.nuLongitude]}
                icon={ICONE_HOSPITAL}
              >
                <Popup>
                  <strong>{h.nome}</strong><br/>
                  📏 {h.distanciaKm} km<br/>
                  🛏️ {h.leitosSus} leitos SUS<br/>
                  {h.servicoInfectologia && "🦠 Infectologia ✅"}<br/>
                  📞 {h.telefone}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        {/* LISTA LATERAL */}
        <div className="w-full md:w-80 flex flex-col gap-3 overflow-y-auto" style={{ maxHeight: 380 }}>
          {hospitais.length === 0 ? (
            <div className="bg-white rounded-xl shadow p-4 text-sm text-gray-400 text-center">
              Nenhum hospital encontrado no raio de 300km
            </div>
          ) : (
            hospitais.map((h, i) => (
              <div
                key={i}
                className={`bg-white rounded-xl shadow p-4 border-l-4 ${
                  i === 0 ? "border-red-500" : "border-gray-200"
                }`}
              >
                <div className="flex items-start justify-between mb-1">
                  <p className="text-sm font-semibold text-gray-800 leading-tight">
                    {i + 1}. {h.nome}
                  </p>
                  {i === 0 && (
                    <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full ml-2 shrink-0">
                      Mais próximo
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
                  <span>📏 {h.distanciaKm} km</span>
                  <span>🛏️ {h.leitosSus} leitos SUS</span>
                  {h.servicoInfectologia && <span>🦠 Infectologia</span>}
                </div>
                {h.telefone && (
                  <a
                    href={`tel:${h.telefone}`}
                    className="mt-2 inline-block text-xs text-blue-600 hover:underline"
                  >
                    📞 {h.telefone}
                  </a>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
