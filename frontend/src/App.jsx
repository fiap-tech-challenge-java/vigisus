import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Atual     from "./pages/Atual";
import Historico from "./pages/Historico";
// import Home from "./pages/Home"; // COMENTADO — não deletar

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"          element={<Navigate to="/atual" replace />} />
        <Route path="/atual"     element={<Atual />} />
        <Route path="/historico" element={<Historico />} />
        <Route path="*"          element={<Navigate to="/atual" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
