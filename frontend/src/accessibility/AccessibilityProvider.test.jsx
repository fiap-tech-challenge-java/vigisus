import { fireEvent, render, screen } from "@testing-library/react";
import { AccessibilityProvider, useAccessibility } from "./AccessibilityProvider";

const STORAGE_KEY = "vigisus_accessibility_v1";

function Probe() {
  const {
    theme,
    highContrast,
    fontPercent,
    toggleTheme,
    toggleHighContrast,
    increaseFont,
    decreaseFont,
    resetAccessibility,
  } = useAccessibility();

  return (
    <div>
      <p data-testid="theme">{theme}</p>
      <p data-testid="contrast">{String(highContrast)}</p>
      <p data-testid="font">{fontPercent}</p>
      <button type="button" onClick={toggleTheme}>
        toggle-theme
      </button>
      <button type="button" onClick={toggleHighContrast}>
        toggle-contrast
      </button>
      <button type="button" onClick={increaseFont}>
        increase-font
      </button>
      <button type="button" onClick={decreaseFont}>
        decrease-font
      </button>
      <button type="button" onClick={resetAccessibility}>
        reset
      </button>
    </div>
  );
}

describe("AccessibilityProvider", () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute("data-theme");
    document.documentElement.removeAttribute("data-contrast");
    document.documentElement.style.removeProperty("--vigi-font-scale");
  });

  it("aplica configuracao padrao e permite alternar tema/contraste/fonte", () => {
    render(
      <AccessibilityProvider>
        <Probe />
      </AccessibilityProvider>
    );

    expect(screen.getByTestId("theme")).toHaveTextContent("light");
    expect(screen.getByTestId("contrast")).toHaveTextContent("false");
    expect(screen.getByTestId("font")).toHaveTextContent("100");
    expect(document.documentElement.dataset.theme).toBe("light");
    expect(document.documentElement.dataset.contrast).toBe("normal");
    expect(document.documentElement.style.getPropertyValue("--vigi-font-scale")).toBe("1");

    fireEvent.click(screen.getByRole("button", { name: "toggle-theme" }));
    fireEvent.click(screen.getByRole("button", { name: "toggle-contrast" }));
    fireEvent.click(screen.getByRole("button", { name: "increase-font" }));

    expect(screen.getByTestId("theme")).toHaveTextContent("dark");
    expect(screen.getByTestId("contrast")).toHaveTextContent("true");
    expect(screen.getByTestId("font")).toHaveTextContent("115");
    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(document.documentElement.dataset.contrast).toBe("high");
    expect(document.documentElement.style.getPropertyValue("--vigi-font-scale")).toBe("1.15");

    fireEvent.click(screen.getByRole("button", { name: "decrease-font" }));
    fireEvent.click(screen.getByRole("button", { name: "reset" }));

    expect(screen.getByTestId("theme")).toHaveTextContent("light");
    expect(screen.getByTestId("contrast")).toHaveTextContent("false");
    expect(screen.getByTestId("font")).toHaveTextContent("100");
  });

  it("carrega configuracao persistida no localStorage", () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ theme: "dark", highContrast: true, fontScale: 1.3 })
    );

    render(
      <AccessibilityProvider>
        <Probe />
      </AccessibilityProvider>
    );

    expect(screen.getByTestId("theme")).toHaveTextContent("dark");
    expect(screen.getByTestId("contrast")).toHaveTextContent("true");
    expect(screen.getByTestId("font")).toHaveTextContent("130");
    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(document.documentElement.dataset.contrast).toBe("high");
    expect(document.documentElement.style.getPropertyValue("--vigi-font-scale")).toBe("1.3");
  });
});
