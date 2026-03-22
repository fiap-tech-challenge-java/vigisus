import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Home from './pages/Home';
import Atual from './pages/Atual';
import Historico from './pages/Historico';
import Resultado from './pages/Resultado';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/atual" element={<Atual />} />
        <Route path="/historico" element={<Historico />} />
        <Route path="/resultado" element={<Resultado />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
