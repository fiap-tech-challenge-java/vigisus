import { useEffect, useMemo } from "react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

const ICONE_ORIGEM = L.divIcon({
  className: "vigisus-marker vigisus-marker--origem",
  html: "<span></span>",
  iconSize: [16, 16],
  iconAnchor: [8, 8],
  popupAnchor: [0, -8],
});

const ICONE_HOSPITAL = L.divIcon({
  className: "vigisus-marker vigisus-marker--hospital",
  html: "<span></span>",
  iconSize: [16, 16],
  iconAnchor: [8, 8],
  popupAnchor: [0, -8],
});

function FitBounds({ coords }) {
  const map = useMap();

  useEffect(() => {
    if (coords.length > 1) {
      map.fitBounds(coords, { padding: [40, 40] });
    }
  }, [coords, map]);

  return null;
}

const DEFAULT_LAT = -15.78;
const DEFAULT_LON = -47.93;

export default function MapaHospitais({ perfil, encaminhamento }) {
  const hospitais = useMemo(() => encaminhamento?.hospitais || [], [encaminhamento]);
  const latOrigem = perfil?.nuLatitude || DEFAULT_LAT;
  const lonOrigem = perfil?.nuLongitude || DEFAULT_LON;

  const hospitaisComCoordenadas = useMemo(
    () => hospitais.filter((hospital) => hospital.nuLatitude && hospital.nuLongitude),
    [hospitais]
  );

  const coords = useMemo(
    () => [
      [latOrigem, lonOrigem],
      ...hospitaisComCoordenadas.map((hospital) => [hospital.nuLatitude, hospital.nuLongitude]),
    ],
    [latOrigem, lonOrigem, hospitaisComCoordenadas]
  );

  return (
    <div className="mx-6 max-w-6xl md:mx-auto mt-4 space-y-4">
      <section className="vigi-card p-4" aria-label="Legenda do mapa de hospitais">
        <h3 className="text-sm font-semibold text-gray-700 mb-2">Legenda do mapa de hospitais</h3>
        <ul className="flex flex-wrap gap-4 text-xs text-gray-600">
          <li className="flex items-center gap-2">
            <span className="vigisus-legend-dot vigisus-legend-dot--origem" aria-hidden="true" />
            Municipio consultado
          </li>
          <li className="flex items-center gap-2">
            <span className="vigisus-legend-dot vigisus-legend-dot--hospital" aria-hidden="true" />
            Hospital de referencia
          </li>
        </ul>
      </section>

      <div className="flex flex-col md:flex-row gap-4">
        <div className="flex-1 rounded-xl overflow-hidden shadow border border-gray-200" style={{ height: 380 }} aria-hidden="true">
          <MapContainer
            center={[latOrigem, lonOrigem]}
            zoom={8}
            preferCanvas
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution="OpenStreetMap"
            />
            <FitBounds coords={coords} />

            <Marker position={[latOrigem, lonOrigem]} icon={ICONE_ORIGEM}>
              <Popup>
                <strong>{perfil?.municipio}</strong>
                <br />
                Municipio buscado
              </Popup>
            </Marker>

            {hospitaisComCoordenadas.map((hospital, index) => (
              <Marker
                key={`${hospital.nome || "hospital"}-${hospital.nuLatitude}-${hospital.nuLongitude}-${index}`}
                position={[hospital.nuLatitude, hospital.nuLongitude]}
                icon={ICONE_HOSPITAL}
              >
                <Popup>
                  <strong>{hospital.nome}</strong>
                  <br />
                  {typeof hospital.distanciaKm === "number" && (
                    <>
                      Distancia: {hospital.distanciaKm.toFixed(1)} km
                      <br />
                    </>
                  )}
                  {hospital.leitosSus != null && (
                    <>
                      Leitos SUS: {hospital.leitosSus}
                      <br />
                    </>
                  )}
                  {hospital.servicoInfectologia && (
                    <>
                      Infectologia: sim
                      <br />
                    </>
                  )}
                  {hospital.telefone ? `Telefone: ${hospital.telefone}` : "Telefone nao informado"}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>

        <div className="w-full md:w-80 flex flex-col gap-3 overflow-y-auto" style={{ maxHeight: 380 }}>
          {hospitais.length === 0 ? (
            <div className="bg-white rounded-xl shadow p-4 text-sm text-gray-400 text-center">
              Nenhum hospital encontrado para esta regiao
            </div>
          ) : (
            hospitais.map((hospital, index) => (
              <article
                key={`${hospital.nome || "hospital-card"}-${hospital.nuLatitude}-${hospital.nuLongitude}-${index}`}
                className={`bg-white rounded-xl shadow p-4 border-l-4 ${
                  index === 0 ? "border-red-500" : "border-gray-200"
                }`}
                aria-label={`Hospital ${hospital.nome || "sem nome"}`}
              >
                <div className="flex items-start justify-between mb-1">
                  <p className="text-sm font-semibold text-gray-800 leading-tight">
                    {index + 1}. {hospital.nome}
                  </p>
                  {index === 0 && (
                    <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full ml-2 shrink-0">
                      Mais proximo
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
                  {typeof hospital.distanciaKm === "number" && (
                    <span>Distancia: {hospital.distanciaKm.toFixed(1)} km</span>
                  )}
                  {hospital.leitosSus != null && <span>Leitos SUS: {hospital.leitosSus}</span>}
                  {hospital.servicoInfectologia && <span>Infectologia</span>}
                </div>
                {hospital.telefone && (
                  <a
                    href={`tel:${hospital.telefone}`}
                    className="mt-2 inline-block text-xs text-blue-600 hover:underline"
                  >
                    Telefone: {hospital.telefone}
                  </a>
                )}
              </article>
            ))
          )}
        </div>
      </div>

      <section className="vigi-card p-4" aria-label="Tabela acessivel de hospitais">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Tabela acessivel de hospitais</h3>
        {hospitais.length === 0 ? (
          <p className="text-sm text-gray-500">Sem hospitais disponiveis para exibir.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-left border-b border-gray-200">
                  <th className="py-2 pr-3">Hospital</th>
                  <th className="py-2 pr-3">Distancia (km)</th>
                  <th className="py-2 pr-3">Leitos SUS</th>
                  <th className="py-2 pr-3">Infectologia</th>
                  <th className="py-2 pr-3">Telefone</th>
                </tr>
              </thead>
              <tbody>
                {hospitais.map((hospital, index) => (
                  <tr key={`${hospital.nome || "hospital-row"}-${index}`} className="border-b border-gray-100">
                    <td className="py-2 pr-3">{hospital.nome || "-"}</td>
                    <td className="py-2 pr-3">
                      {typeof hospital.distanciaKm === "number" ? hospital.distanciaKm.toFixed(1) : "-"}
                    </td>
                    <td className="py-2 pr-3">{hospital.leitosSus ?? "-"}</td>
                    <td className="py-2 pr-3">{hospital.servicoInfectologia ? "Sim" : "Nao"}</td>
                    <td className="py-2 pr-3">{hospital.telefone || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
