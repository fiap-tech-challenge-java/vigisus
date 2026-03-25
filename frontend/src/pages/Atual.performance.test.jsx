import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Atual from "./Atual";
import { AccessibilityProvider } from "../accessibility/AccessibilityProvider";

jest.mock("../services/api", () => ({
  buscarMunicipio: jest.fn(),
  buscarRankingEstado: jest.fn(),
  buscarHistoricoEstado: jest.fn(),
  buscarBrasil: jest.fn(),
  buscarHospitaisBrasilAgregado: jest.fn(),
  buscarHospitaisEstadoRegiao: jest.fn(),
  buscarRiscoBrasil: jest.fn(),
  buscarRiscoEstado: jest.fn(),
}));

jest.mock("../components/TopNav", () => (props) => (
  <div data-testid="topnav">{props.loading ? "loading" : "idle"}</div>
));
jest.mock("../components/HeaderAlerta", () => () => <div>Header</div>);
jest.mock("../components/OQueFazerAgora", () => () => <div>Acoes</div>);
jest.mock("../components/KpiCards", () => () => <div>KPIs</div>);
jest.mock("../components/StatusRapido", () => () => <div>Status</div>);
jest.mock("../components/CurvaEpidemiologica", () => () => <div>Curva</div>);
jest.mock("../components/RiscoFuturo", () => () => <div>Risco</div>);
jest.mock("../components/MapaEstado", () => () => <div>Mapa</div>);
jest.mock("../components/MapaHospitais", () => () => <div>Hospitais</div>);
jest.mock("../components/ResumoIa", () => () => <div>Resumo</div>);

const api = require("../services/api");

describe("Atual - carregamento inicial", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    api.buscarBrasil.mockResolvedValue({
      totalCasos: 1000,
      incidencia: 120,
      classificacao: "ALTO",
      tendencia: "ESTAVEL",
      semanas: [],
      semanasAnoAnterior: [],
      textoIa: "Resumo Brasil",
      estadosPiores: [],
      municipiosPiores: [],
    });
    api.buscarRiscoBrasil.mockResolvedValue(null);
    api.buscarHospitaisBrasilAgregado.mockResolvedValue([]);

    Object.defineProperty(window.navigator, "geolocation", {
      configurable: true,
      value: {
        getCurrentPosition: jest.fn(),
      },
    });
  });

  it("carrega dados do Brasil sem bloquear por geolocalizacao pendente", async () => {
    render(
      <AccessibilityProvider>
        <MemoryRouter initialEntries={["/atual"]}>
          <Routes>
            <Route path="/atual" element={<Atual />} />
          </Routes>
        </MemoryRouter>
      </AccessibilityProvider>
    );

    await waitFor(() => {
      expect(api.buscarBrasil).toHaveBeenCalledTimes(1);
    });

    expect(screen.getByText(/Detectando sua localizacao em segundo plano/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Brasil - BR - dengue")).toBeInTheDocument();
    });
  });
});
