import { useState } from "react";
import { getCor } from "../utils/cores";

const INSIGHTS = {
  EPIDEMIA: {
    medico: [
      "Alta incidência — atenção a casos febris",
      "Monitorar sinais de agravamento: dor abdominal, vômitos, hipotensão",
      "Tendência crescente sugere aumento de atendimentos nos próximos dias",
    ],
    gestor: [
      "Verificar capacidade de leitos e disponibilidade de insumos",
      "Comunicar à Vigilância Epidemiológica Municipal",
      "Considerar reforço de equipe e abertura de sala de situação",
    ],
    cidadao: [
      "Eliminar focos de água parada em casa e arredores",
      "Buscar atendimento se febre > 38°C",
      "Região com alta circulação da doença — redobrar cuidados",
    ],
  },
  ALTO: {
    medico: [
      "Volume de casos acima da média histórica",
      "Atenção a pacientes com mais de 3 dias de febre ou sinais de alarme",
      "Verificar disponibilidade de leitos de observação",
    ],
    gestor: [
      "Tendência crescente requer monitoramento diário",
      "Verificar capacidade de atendimento e estoque de insumos",
      "Manter comunicação ativa com hospitais de referência",
    ],
    cidadao: [
      "Situação de alerta — adotar medidas preventivas",
      "Usar repelente e roupas de manga longa",
      "Procurar UBS ou UPA em caso de febre com dor muscular",
    ],
  },
  MODERADO: {
    medico: [
      "Seguir protocolo padrão de atendimento",
      "Manter triagem de casos febris com histórico epidemiológico",
    ],
    gestor: [
      "Situação estável — manter monitoramento semanal",
      "Acompanhar evolução da curva epidemiológica",
    ],
    cidadao: [
      "Manter cuidados preventivos habituais",
      "Eliminar focos de mosquito em casa",
    ],
  },
  BAIXO: {
    medico: ["Protocolo padrão de atendimento"],
    gestor: ["Situação favorável — manter vigilância de rotina"],
    cidadao: ["Manter prevenção básica"],
  },
};

const PERSONAS = [
  {
    key: "medico",
    icone: "👩‍⚕️",
    titulo: "Atendimento",
    borda: "border-blue-400",
    fundo: "bg-blue-50",
    titulo_cor: "text-blue-800",
    bullet_cor: "text-blue-500",
  },
  {
    key: "gestor",
    icone: "🏥",
    titulo: "Gestão",
    borda: "border-orange-400",
    fundo: "bg-orange-50",
    titulo_cor: "text-orange-800",
    bullet_cor: "text-orange-500",
  },
  {
    key: "cidadao",
    icone: "👤",
    titulo: "População",
    borda: "border-green-400",
    fundo: "bg-green-50",
    titulo_cor: "text-green-800",
    bullet_cor: "text-green-500",
  },
];

const NIVEL_EMOJI = {
  EPIDEMIA: "🔴",
  ALTO: "🟠",
  MODERADO: "🟡",
  BAIXO: "🟢",
};

export default function OQueFazerAgora({ perfil, textoIa }) {
  const [expandido, setExpandido] = useState(false);

  const nivel = (perfil?.classificacao || "BAIXO").toUpperCase();
  const insights = INSIGHTS[nivel] || INSIGHTS.BAIXO;
  const cor = getCor(nivel);
  const emoji = NIVEL_EMOJI[nivel] || "⚪";

  return (
    <div>
      <div className={`rounded-xl border ${cor.borda} bg-white shadow-sm overflow-hidden`}>

        {/* Header */}
        <div
          className="flex items-center justify-between px-5 py-4 border-b border-gray-100"
          aria-label={`O que fazer agora — nível ${nivel}`}
        >
          <p className="text-sm font-bold text-gray-800">
            <span aria-hidden="true">🤖</span> O que fazer agora?
          </p>
          <span className={`text-xs font-bold px-3 py-1 rounded-full ${cor.badge}`}>
            {nivel} {emoji}
          </span>
        </div>

        {/* Personas */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-0 divide-y md:divide-y-0 md:divide-x divide-gray-100">
          {PERSONAS.map((p) => (
            <div key={p.key} className={`p-4 ${p.fundo}`}>
              <p className={`text-sm font-bold mb-2 ${p.titulo_cor}`}>
                {p.icone} {p.titulo}
              </p>
              <ul className="space-y-1.5">
                {(insights[p.key] || []).map((item, i) => (
                  <li key={i} className="flex gap-2 text-sm text-gray-700">
                    <span className={`mt-0.5 shrink-0 ${p.bullet_cor}`}>•</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* IA Analysis */}
        {textoIa && (
          <div className="border-t border-gray-100 px-5 py-4 bg-gray-50">
            <p
              className={`text-sm text-gray-600 leading-relaxed ${
                !expandido ? "line-clamp-2" : ""
              }`}
            >
              <span aria-hidden="true">🤖</span>{" "}
              <span className="font-medium text-gray-500">Análise IA:</span>{" "}
              &ldquo;{textoIa}&rdquo;
            </p>
            <button
              onClick={() => setExpandido(!expandido)}
              className="mt-1 text-xs text-blue-500 hover:underline"
            >
              {expandido ? "Ver menos ▲" : "Ver análise completa ▼"}
            </button>
          </div>
        )}
      </div>

      <p className="text-xs text-gray-400 mt-2 text-center italic">
        Orientações baseadas no contexto epidemiológico local.
        Decisões clínicas e operacionais são de responsabilidade do profissional habilitado.
      </p>
    </div>
  );
}
