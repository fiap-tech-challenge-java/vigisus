import React from 'react';

function TextoIa({ texto }) {
  if (!texto) return null;

  return (
    <div className="bg-blue-50 border-l-4 border-sus-blue rounded-lg p-6 shadow-sm">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-sus-blue text-xl">🤖</span>
        <h2 className="text-lg font-bold text-gray-800">Análise da IA</h2>
      </div>
      <p className="text-gray-700 leading-relaxed whitespace-pre-line">{texto}</p>
    </div>
  );
}

export default TextoIa;
