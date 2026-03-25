import { Suspense, lazy } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AccessibilityProvider } from "./accessibility/AccessibilityProvider";

const Atual = lazy(() => import("./pages/Atual"));
const Historico = lazy(() => import("./pages/Historico"));

function RouteFallback() {
  return (
    <div className="min-h-screen vigi-page flex items-center justify-center">
      <p className="text-sm text-gray-400 animate-pulse">Carregando aplicacao...</p>
    </div>
  );
}

export default function App() {
  return (
    <AccessibilityProvider>
      <a href="#main-content" className="vigi-skip-link">
        Pular para o conteudo principal
      </a>
      <BrowserRouter>
        <Suspense fallback={<RouteFallback />}>
          <Routes>
            <Route path="/" element={<Navigate to="/atual" replace />} />
            <Route path="/atual" element={<Atual />} />
            <Route path="/historico" element={<Historico />} />
            <Route path="*" element={<Navigate to="/atual" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AccessibilityProvider>
  );
}
