import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import TopNav from "./TopNav";
import { AccessibilityProvider } from "../accessibility/AccessibilityProvider";

function renderTopNav(pathname = "/atual?uf=BR&doenca=dengue", loading = false) {
  return render(
    <AccessibilityProvider>
      <MemoryRouter initialEntries={[pathname]}>
        <Routes>
          <Route path="/atual" element={<TopNav loading={loading} />} />
          <Route path="/historico" element={<TopNav loading={loading} />} />
        </Routes>
      </MemoryRouter>
    </AccessibilityProvider>
  );
}

describe("TopNav acessivel", () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute("data-theme");
    document.documentElement.removeAttribute("data-contrast");
    document.documentElement.style.removeProperty("--vigi-font-scale");
  });

  it("exibe labels acessiveis e status de busca quando loading estiver ativo", () => {
    renderTopNav("/atual?uf=SP&doenca=dengue", true);

    expect(screen.getByLabelText("Municipio")).toBeInTheDocument();
    expect(screen.getByLabelText("Estado ou Brasil")).toBeInTheDocument();
    expect(screen.getByLabelText("Doenca")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Buscando..." })).toBeDisabled();
    expect(screen.getByText("Busca em andamento")).toBeInTheDocument();
  });

  it("abre painel de acessibilidade e permite fechar com ESC", async () => {
    renderTopNav();

    fireEvent.click(screen.getByRole("button", { name: "Acessibilidade" }));
    const dialog = screen.getByRole("dialog", { name: "Configuracoes de acessibilidade" });
    expect(dialog).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Tema escuro" }));
    expect(document.documentElement.dataset.theme).toBe("dark");

    fireEvent.click(screen.getByRole("button", { name: "Alto contraste" }));
    expect(document.documentElement.dataset.contrast).toBe("high");

    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => {
      expect(
        screen.queryByRole("dialog", { name: "Configuracoes de acessibilidade" })
      ).not.toBeInTheDocument();
    });
  });
});
