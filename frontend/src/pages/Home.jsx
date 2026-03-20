import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buscarPorPergunta } from '../services/api';

const UFS = [
  'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
  'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO',
];

const DOENCAS = [
  { value: 'dengue',       label: 'Dengue' },
  { value: 'chikungunya',  label: 'Chikungunya' },
  { value: 'zika',         label: 'Zika' },
];

const ANOS = ['2025', '2024', '2023', '2022', '2021'];

function Home() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState('');

  const [municipio, setMunicipio] = useState('');
  const [uf, setUf] = useState('');
  const [doenca, setDoenca] = useState('dengue');
  const [ano, setAno] = useState('2024');
  const [perguntaLivre, setPerguntaLivre] = useState('');

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

  const handleFormSubmit = (e) => {
    e.preventDefault();
    const partes = [doenca];
    if (municipio.trim()) partes.push('em', municipio.trim());
    if (uf) partes.push(uf);
    if (ano) partes.push(ano);
    handleSearch(partes.join(' '));
  };

  const handleLivreSubmit = (e) => {
    e.preventDefault();
    if (perguntaLivre.trim()) handleSearch(perguntaLivre.trim());
  };

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center px-4"
      style={{ background: 'linear-gradient(135deg, #0f172a 0%, #1e3a5f 60%, #0f2027 100%)' }}
    >
      {/* Logo */}
      <div className="flex items-center gap-3 mb-8">
        <div className="w-14 h-14 bg-red-600 rounded-full flex items-center justify-center shadow-lg">
          <span className="text-white text-3xl font-extrabold">V</span>
        </div>
        <div>
          <h1 className="text-4xl font-extrabold text-white tracking-tight">VígiSUS</h1>
          <p className="text-red-400 text-sm font-semibold uppercase tracking-widest">
            Vigilância Epidemiológica
          </p>
        </div>
      </div>

      {/* Card de busca */}
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg p-8">
        <h2 className="text-lg font-bold text-gray-800 mb-5 text-center">
          Consultar situação epidemiológica
        </h2>

        <form onSubmit={handleFormSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 sm:col-span-1">
              <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">
                Município
              </label>
              <input
                type="text"
                value={municipio}
                onChange={(e) => setMunicipio(e.target.value)}
                placeholder="Ex: Ipatinga"
                disabled={loading}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-400 focus:border-transparent disabled:opacity-50"
              />
            </div>
            <div className="col-span-2 sm:col-span-1">
              <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">
                Estado (UF)
              </label>
              <select
                value={uf}
                onChange={(e) => setUf(e.target.value)}
                disabled={loading}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-400 focus:border-transparent disabled:opacity-50 bg-white"
              >
                <option value="">Todos</option>
                {UFS.map((u) => (
                  <option key={u} value={u}>{u}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">
                Doença
              </label>
              <select
                value={doenca}
                onChange={(e) => setDoenca(e.target.value)}
                disabled={loading}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-400 focus:border-transparent disabled:opacity-50 bg-white"
              >
                {DOENCAS.map((d) => (
                  <option key={d.value} value={d.value}>{d.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">
                Ano
              </label>
              <select
                value={ano}
                onChange={(e) => setAno(e.target.value)}
                disabled={loading}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-400 focus:border-transparent disabled:opacity-50 bg-white"
              >
                {ANOS.map((a) => (
                  <option key={a} value={a}>{a}</option>
                ))}
              </select>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || !municipio.trim()}
            className="w-full py-3 bg-red-600 hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-base rounded-lg transition-colors shadow-md"
          >
            {loading ? 'Buscando...' : 'Buscar'}
          </button>
        </form>

        {/* Divisor */}
        <div className="flex items-center gap-3 my-5">
          <hr className="flex-1 border-gray-200" />
          <span className="text-xs text-gray-400">ou</span>
          <hr className="flex-1 border-gray-200" />
        </div>

        {/* Busca livre */}
        <form onSubmit={handleLivreSubmit} className="flex gap-2">
          <input
            type="text"
            value={perguntaLivre}
            onChange={(e) => setPerguntaLivre(e.target.value)}
            placeholder="Digite livremente: dengue em Lavras MG 2024"
            disabled={loading}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-gray-400 focus:border-transparent disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={loading || !perguntaLivre.trim()}
            className="px-4 py-2 bg-gray-700 hover:bg-gray-800 disabled:opacity-50 text-white text-sm font-semibold rounded-lg transition-colors"
          >
            →
          </button>
        </form>

        {erro && (
          <p className="text-red-500 text-sm text-center mt-3">{erro}</p>
        )}
      </div>

      <p className="text-center text-gray-500 text-xs mt-6 max-w-sm leading-relaxed">
        Dados públicos do DATASUS, IBGE e Open-Meteo.
        Sem cadastro. Sem login. Para todos.
      </p>
    </div>
  );
}

export default Home;
