import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import SearchBar from '../components/SearchBar';
import { buscarPorPergunta } from '../services/api';

function Home() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState('');

  const handleSearch = async (pergunta) => {
    setLoading(true);
    setErro('');
    try {
      const response = await buscarPorPergunta(pergunta);
      navigate('/resultado', { state: { dados: response.data, pergunta } });
    } catch (err) {
      setErro('Não foi possível processar sua busca. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4">
      <div className="flex flex-col items-center gap-6 w-full max-w-2xl">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-sus-green rounded-full flex items-center justify-center">
            <span className="text-white text-2xl font-bold">V</span>
          </div>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">VígiSUS</h1>
            <p className="text-sus-green text-sm font-medium">Vigilância Epidemiológica do SUS</p>
          </div>
        </div>

        <h2 className="text-xl text-gray-600 text-center">
          Consulte dados epidemiológicos de qualquer município brasileiro
        </h2>

        <SearchBar onSubmit={handleSearch} loading={loading} />

        {erro && (
          <p className="text-red-500 text-sm text-center">{erro}</p>
        )}

        <p className="text-center text-gray-400 text-sm max-w-md leading-relaxed">
          Dados públicos do DATASUS, IBGE e Open-Meteo.
          <br />
          Sem cadastro. Sem login. Para todos.
        </p>

        <div className="flex flex-wrap justify-center gap-2 mt-2">
          {[
            'dengue em Lavras MG 2024',
            'leptospirose São Paulo SP 2023',
            'chikungunya Rio de Janeiro RJ 2024',
          ].map((exemplo) => (
            <button
              key={exemplo}
              onClick={() => handleSearch(exemplo)}
              disabled={loading}
              className="text-xs px-3 py-1.5 bg-white border border-gray-200 rounded-full text-gray-500 hover:border-sus-blue hover:text-sus-blue transition-colors disabled:opacity-50"
            >
              {exemplo}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

export default Home;
