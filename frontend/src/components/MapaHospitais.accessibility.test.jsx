import { render, screen } from "@testing-library/react";
import MapaHospitais from "./MapaHospitais";

jest.mock("leaflet", () => {
  const Default = {
    prototype: { _getIconUrl: jest.fn() },
    mergeOptions: jest.fn(),
  };

  return {
    __esModule: true,
    default: {
      Icon: { Default },
      divIcon: jest.fn(() => ({ mocked: true })),
    },
  };
});

jest.mock("react-leaflet", () => ({
  MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
  TileLayer: () => null,
  Marker: ({ children }) => <div>{children}</div>,
  Popup: ({ children }) => <div>{children}</div>,
  useMap: () => ({ fitBounds: jest.fn() }),
}));

describe("MapaHospitais acessivel", () => {
  it("exibe legenda e tabela de hospitais para navegacao assistiva", () => {
    render(
      <MapaHospitais
        perfil={{ municipio: "Campinas", nuLatitude: -22.9, nuLongitude: -47.06 }}
        encaminhamento={{
          hospitais: [
            {
              nome: "Hospital A",
              distanciaKm: 3.5,
              leitosSus: 12,
              servicoInfectologia: true,
              telefone: "1999999999",
              nuLatitude: -22.91,
              nuLongitude: -47.04,
            },
          ],
        }}
      />
    );

    expect(
      screen.getByRole("heading", { name: "Legenda do mapa de hospitais" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Tabela acessivel de hospitais" })
    ).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Hospital" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Hospital A" })).toBeInTheDocument();
  });
});
