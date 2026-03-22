import { useState } from "react";

const UFS = ["AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
             "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
             "RS","RO","RR","SC","SP","SE","TO"];
const ANOS = [2024, 2023, 2022, 2021, 2020, 2019, 2018];

export default function SearchBarHistorico({
  onBuscar, municipioInicial = "", ufInicial = "MG",
  anoInicial = 2024, loading
}) {
  const [municipio, setMunicipio] = useState(municipioInicial);
  const [uf, setUf]               = useState(ufInicial);
  const [doenca, setDoenca]       = useState("dengue");
  const [ano, setAno]             = useState(anoInicial);

  const handleSubmit = (e) => {
    e.preventDefault();
    onBuscar(municipio.trim() || null, uf, doenca, ano);
  };

  return (
    <div className="bg-gray-800 border-b border-gray-700
                    sticky top-[56px] z-40">
      <form onSubmit={handleSubmit}
        className="max-w-4xl mx-auto px-4 py-3
                   flex gap-2 items-center flex-wrap">
        <input
          type="text"
          placeholder="Cidade (vazio = estado inteiro)"
          value={municipio}
          onChange={e => setMunicipio(e.target.value)}
          className="flex-1 min-w-[140px] bg-gray-700 border
                     border-gray-600 rounded-lg px-3 py-2 text-sm
                     text-white placeholder-gray-400 focus:outline-none
                     focus:ring-2 focus:ring-blue-400"
        />
        <select value={uf} onChange={e => setUf(e.target.value)}
          className="w-20 bg-gray-700 border border-gray-600
                     rounded-lg px-2 py-2 text-sm text-white">
          {UFS.map(u => <option key={u} value={u}>{u}</option>)}
        </select>
        <select value={doenca} onChange={e => setDoenca(e.target.value)}
          className="w-32 bg-gray-700 border border-gray-600
                     rounded-lg px-2 py-2 text-sm text-white">
          <option value="dengue">Dengue</option>
          <option value="chikungunya">Chikungunya</option>
          <option value="zika">Zika</option>
        </select>
        <select value={ano}
          onChange={e => setAno(Number(e.target.value))}
          className="w-24 bg-gray-700 border border-gray-600
                     rounded-lg px-2 py-2 text-sm text-white">
          {ANOS.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
        <button type="submit" disabled={loading}
          className="bg-blue-600 hover:bg-blue-700 text-white
                     px-4 py-2 rounded-lg text-sm font-medium
                     disabled:opacity-50">
          {loading ? "..." : "Buscar"}
        </button>
      </form>
    </div>
  );
}

