import { useState } from "react";

const UFS = ["AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
             "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
             "RS","RO","RR","SC","SP","SE","TO"];

export default function SearchBarAtual({
  onBuscar, municipioInicial = "", ufInicial = "MG", loading
}) {
  const [municipio, setMunicipio] = useState(municipioInicial);
  const [uf, setUf]               = useState(ufInicial);
  const [doenca, setDoenca]       = useState("dengue");

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!municipio.trim()) return;
    onBuscar(municipio.trim(), uf, doenca);
  };

  return (
    <div className="bg-white border-b border-gray-100 shadow-sm
                    sticky top-[56px] z-40">
      <form onSubmit={handleSubmit}
        className="max-w-4xl mx-auto px-4 py-3
                   flex gap-2 items-center flex-wrap">
        <input
          type="text"
          placeholder="Cidade (ex: Lavras)"
          value={municipio}
          onChange={e => setMunicipio(e.target.value)}
          className="flex-1 min-w-[140px] border border-gray-200
                     rounded-lg px-3 py-2 text-sm focus:outline-none
                     focus:ring-2 focus:ring-red-400"
        />
        <select value={uf} onChange={e => setUf(e.target.value)}
          className="w-20 border border-gray-200 rounded-lg
                     px-2 py-2 text-sm">
          {UFS.map(u => <option key={u} value={u}>{u}</option>)}
        </select>
        <select value={doenca} onChange={e => setDoenca(e.target.value)}
          className="w-32 border border-gray-200 rounded-lg
                     px-2 py-2 text-sm">
          <option value="dengue">Dengue</option>
          <option value="chikungunya">Chikungunya</option>
          <option value="zika">Zika</option>
        </select>
        <button type="submit"
          disabled={loading || !municipio.trim()}
          className="bg-red-600 hover:bg-red-700 text-white
                     px-4 py-2 rounded-lg text-sm font-medium
                     disabled:opacity-50">
          {loading ? "..." : "Buscar"}
        </button>
      </form>
    </div>
  );
}

