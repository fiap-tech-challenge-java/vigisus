import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

const blueIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png',
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

const redIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

function MapaHospitais({ municipio, lat, lng, hospitais }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);

  const lista = (hospitais || []).slice().sort((a, b) => {
    const da = parseFloat(a.distancia_km ?? a.distancia ?? 9999);
    const db = parseFloat(b.distancia_km ?? b.distancia ?? 9999);
    return da - db;
  });

  useEffect(() => {
    if (!mapRef.current) return;

    const centerLat = lat || -15.7801;
    const centerLng = lng || -47.9292;

    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }

    const map = L.map(mapRef.current).setView([centerLat, centerLng], 11);
    mapInstanceRef.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(map);

    const bounds = [];

    const munMarker = L.marker([centerLat, centerLng], { icon: blueIcon })
      .addTo(map)
      .bindPopup(`<b>${municipio || 'Município buscado'}</b>`);
    bounds.push([centerLat, centerLng]);

    if (lista.length > 0) {
      lista.forEach((h) => {
        if (h.lat && h.lng) {
          const distText = h.distancia_km != null ? `${h.distancia_km} km` : h.distancia || '';
          const telHtml = h.telefone
            ? `<br/><a href="tel:${h.telefone}" style="color:#009EE3">${h.telefone}</a>`
            : '';
          L.marker([h.lat, h.lng], { icon: redIcon })
            .addTo(map)
            .bindPopup(
              `<b>${h.nome || 'Hospital'}</b><br/>` +
              `${distText ? `Distância: ${distText}<br/>` : ''}` +
              `${h.leitos_sus != null ? `Leitos SUS: ${h.leitos_sus}<br/>` : h.leitosSus != null ? `Leitos SUS: ${h.leitosSus}<br/>` : ''}` +
              telHtml
            );
          bounds.push([h.lat, h.lng]);
        }
      });

      if (bounds.length > 1) {
        map.fitBounds(bounds, { padding: [40, 40] });
      }
    } else {
      munMarker.openPopup();
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, [lat, lng, municipio, hospitais]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-bold text-gray-800 mb-4">🏥 Hospitais com estrutura disponível</h2>
      <div className="flex flex-col md:flex-row gap-4">
        {/* Mapa */}
        <div className="flex-1 min-w-0">
          <div ref={mapRef} style={{ height: '400px', borderRadius: '8px' }} />
          <div className="flex gap-4 text-xs text-gray-500 mt-2">
            <span>🔵 Município buscado</span>
            <span>🔴 Hospitais SUS</span>
          </div>
        </div>

        {/* Lista */}
        <div className="w-full md:w-80 flex flex-col gap-2 overflow-y-auto" style={{ maxHeight: '420px' }}>
          {lista.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-8">
              Nenhum hospital encontrado no raio de 300km
            </p>
          ) : (
            lista.map((h, idx) => {
              const dist = h.distancia_km ?? h.distancia;
              const leitos = h.leitos_sus ?? h.leitosSus;
              const tel = h.telefone;
              const infectologia = h.infectologia ?? h.temInfectologia;
              const isFirst = idx === 0;

              return (
                <div
                  key={idx}
                  className={`rounded-lg border p-3 text-sm ${
                    isFirst
                      ? 'border-red-400 bg-red-50 shadow-sm'
                      : 'border-gray-200 bg-gray-50'
                  }`}
                >
                  <p className="font-semibold text-gray-900 truncate">
                    {idx + 1}. {h.nome || 'Hospital'}
                    {isFirst && (
                      <span className="ml-1 text-xs text-red-600 font-bold">(mais próximo)</span>
                    )}
                  </p>
                  <div className="flex flex-wrap gap-2 mt-1 text-gray-600 text-xs">
                    {dist != null && <span>📍 {dist} km</span>}
                    {leitos != null && <span>🛏 {leitos} leitos SUS</span>}
                    {infectologia != null && (
                      <span>Infectologia: {infectologia ? '✅' : '—'}</span>
                    )}
                  </div>
                  {tel && (
                    <a
                      href={`tel:${tel}`}
                      className="text-blue-600 text-xs mt-0.5 block hover:underline"
                    >
                      📞 {tel}
                    </a>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}

export default MapaHospitais;
