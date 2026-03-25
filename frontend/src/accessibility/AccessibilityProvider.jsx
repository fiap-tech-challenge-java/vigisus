import { createContext, useContext, useEffect, useMemo, useState } from "react";

const STORAGE_KEY = "vigisus_accessibility_v1";
const FONT_SCALES = [1, 1.15, 1.3];

const DEFAULT_SETTINGS = {
  theme: "light",
  highContrast: false,
  fontScale: 1,
};

const AccessibilityContext = createContext(null);

function lerConfiguracao() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return DEFAULT_SETTINGS;
    }

    const parsed = JSON.parse(raw);
    const theme = parsed?.theme === "dark" ? "dark" : "light";
    const highContrast = Boolean(parsed?.highContrast);
    const fontScale = FONT_SCALES.includes(parsed?.fontScale)
      ? parsed.fontScale
      : DEFAULT_SETTINGS.fontScale;

    return { theme, highContrast, fontScale };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function AccessibilityProvider({ children }) {
  const [settings, setSettings] = useState(lerConfiguracao);

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  }, [settings]);

  useEffect(() => {
    const html = document.documentElement;
    html.dataset.theme = settings.theme;
    html.dataset.contrast = settings.highContrast ? "high" : "normal";
    html.style.setProperty("--vigi-font-scale", String(settings.fontScale));
  }, [settings]);

  const value = useMemo(() => {
    return {
      ...settings,
      fontPercent: Math.round(settings.fontScale * 100),
      toggleTheme: () =>
        setSettings((current) => ({
          ...current,
          theme: current.theme === "dark" ? "light" : "dark",
        })),
      toggleHighContrast: () =>
        setSettings((current) => ({
          ...current,
          highContrast: !current.highContrast,
        })),
      increaseFont: () =>
        setSettings((current) => {
          const fontScaleIndex = FONT_SCALES.indexOf(current.fontScale);
          return {
            ...current,
            fontScale: FONT_SCALES[Math.min(fontScaleIndex + 1, FONT_SCALES.length - 1)],
          };
        }),
      decreaseFont: () =>
        setSettings((current) => {
          const fontScaleIndex = FONT_SCALES.indexOf(current.fontScale);
          return {
            ...current,
            fontScale: FONT_SCALES[Math.max(fontScaleIndex - 1, 0)],
          };
        }),
      resetAccessibility: () => setSettings(DEFAULT_SETTINGS),
    };
  }, [settings]);

  return (
    <AccessibilityContext.Provider value={value}>
      {children}
    </AccessibilityContext.Provider>
  );
}

export function useAccessibility() {
  const context = useContext(AccessibilityContext);
  if (!context) {
    throw new Error("useAccessibility deve ser usado dentro de AccessibilityProvider.");
  }
  return context;
}
