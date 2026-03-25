import { render, screen } from "@testing-library/react";
import MapaEstado from "./MapaEstado";

jest.mock("react-leaflet", () => ({
  MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
  TileLayer: () => null,
  GeoJSON: () => null,
}));

describe("MapaEstado acessivel", () => {
  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        type: "FeatureCollection",
        features: [{ type: "Feature", properties: { sigla: "SP", name: "Sao Paulo" } }],
      }),
    });
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  it("mostra legenda e resumo textual para leitor de tela", async () => {
    render(
      <MapaEstado
        uf="BR"
        nivel="brasil"
        ranking={[{ sgUf: "SP", classificacao: "ALTO", totalCasos: 100, incidencia100k: 52.1 }]}
      />
    );

    expect(await screen.findByRole("heading", { name: "Legenda do mapa" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Resumo textual para acessibilidade" })).toBeInTheDocument();
    expect(screen.getAllByText("Alto").length).toBeGreaterThan(0);
    expect(screen.getByText("1 regioes")).toBeInTheDocument();
  });
});
