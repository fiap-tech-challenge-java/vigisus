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

    L.marker([centerLat, centerLng], { icon: blueIcon })
      .addTo(map)
      .bindPopup(`<b>${municipio || 'Município buscado'}</b>`)
      .openPopup();

    if (hospitais && hospitais.length > 0) {
      hospitais.forEach((h) => {
        if (h.lat && h.lng) {
          L.marker([h.lat, h.lng], { icon: redIcon })
            .addTo(map)
            .bindPopup(
              `<b>${h.nome || 'Hospital'}</b><br/>` +
              `${h.distancia ? `Distância: ${h.distancia}<br/>` : ''}` +
              `${h.leitosSus !== undefined ? `Leitos SUS: ${h.leitosSus}<br/>` : ''}` +
              `${h.telefone ? `Tel: ${h.telefone}` : ''}`
            );
        }
      });
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, [lat, lng, municipio, hospitais]);

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-bold text-gray-800 mb-4">🏥 Hospitais com Leitos Disponíveis</h2>
      <div className="flex gap-4 text-sm text-gray-500 mb-3">
        <span>🔵 Município buscado</span>
        <span>🔴 Hospitais SUS</span>
      </div>
      <div ref={mapRef} style={{ height: '400px', borderRadius: '8px' }} />
    </div>
  );
}

export default MapaHospitais;
