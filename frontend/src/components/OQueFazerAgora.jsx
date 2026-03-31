import { useState } from "react";
import { getCor } from "../utils/cores";
import InsightsIaBloco from "./InsightsIaBloco";

const INSIGHTS = {
  EPIDEMIA: {
    medico: [
      "Alta incidencia - atencao reforcada a casos febris e sinais de alarme",
      "Monitorar dor abdominal, vomitos persistentes e hipotensao",
      "A curva sugere maior chance de sobrecarga assistencial no curto prazo",
    ],
    gestor: [
      "Verificar capacidade de leitos, hidracao e estoque de insumos",
      "Manter comunicacao ativa com a vigilancia epidemiologica",
      "Avaliar reforco de equipe e sala de situacao em periodos de pico",
    ],
    cidadao: [
      "Eliminar agua parada e reforcar repelente e barreiras fisicas",
      "Buscar atendimento em caso de febre com piora clinica",
      "A circulacao da doenca esta elevada no territorio analisado",
    ],
  },
  ALTO: {
    medico: [
      "Volume de casos acima da media historica",
      "Atencao a pacientes com mais de 3 dias de febre ou sinais de alarme",
      "Verificar disponibilidade de observacao e retorno precoce",
    ],
    gestor: [
      "Tendencia crescente pede monitoramento frequente",
      "Checar capacidade de atendimento e reposicao de insumos",
      "Manter referencia alinhada com hospitais da regiao",
    ],
    cidadao: [
      "Situacao de alerta - intensificar prevencao",
      "Usar repelente e reduzir exposicao a mosquitos",
      "Procurar UBS ou UPA se houver febre com dor no corpo",
    ],
  },
  MODERADO: {
    medico: [
      "Seguir protocolo padrao de atendimento",
      "Manter triagem de febre com contexto epidemiologico",
    ],
    gestor: [
      "Situacao estavel, mas ainda relevante",
      "Acompanhar curva e sinais de aceleracao nas semanas recentes",
    ],
    cidadao: [
      "Manter cuidados preventivos habituais",
      "Eliminar focos do mosquito de forma rotineira",
    ],
  },
  BAIXO: {
    medico: ["Protocolo padrao de atendimento"],
    gestor: ["Situacao mais favoravel - manter vigilancia de rotina"],
    cidadao: ["Manter prevencao basica"],
  },
};

const PERSONAS = [
  {
    key: "medico",
    icone: "🩺",
    titulo: "Atendimento",
    fundo: "bg-blue-50",
    tituloCor: "text-blue-800",
    bulletCor: "text-blue-500",
  },
  {
    key: "gestor",
    icone: "📋",
    titulo: "Gestao",
    fundo: "bg-orange-50",
    tituloCor: "text-orange-800",
    bulletCor: "text-orange-500",
  },
  {
    key: "cidadao",
    icone: "👥",
    titulo: "Populacao",
    fundo: "bg-green-50",
    tituloCor: "text-green-800",
    bulletCor: "text-green-500",
  },
];

const NIVEL_EMOJI = {
  EPIDEMIA: "alto",
  ALTO: "alerta",
  MODERADO: "moderado",
  BAIXO: "baixo",
};

export default function OQueFazerAgora({ perfil, textoIa, ranking = [], risco = null }) {
  const [expandido, setExpandido] = useState(false);

  const nivel = (perfil?.classificacao || "BAIXO").toUpperCase();
  const insights = INSIGHTS[nivel] || INSIGHTS.BAIXO;
  const cor = getCor(nivel);
  const emoji = NIVEL_EMOJI[nivel] || "neutro";

  return (
    <div>
      <div className={`rounded-xl border ${cor.borda} bg-white shadow-sm overflow-hidden vigi-focus-card`} tabIndex={0}>
        <div
          className="flex items-center justify-between px-5 py-4 border-b border-gray-100"
          aria-label={`O que fazer agora - nivel ${nivel}`}
        >
          <p className="text-sm font-bold text-gray-800">
            O que fazer agora?
          </p>
          <span className={`text-xs font-bold px-3 py-1 rounded-full ${cor.badge}`}>
            {nivel} {emoji}
          </span>
        </div>

        <div className="px-5 py-4 bg-red-50 border-b border-red-100">
          <p className="text-sm text-red-900 font-medium leading-relaxed">
            A leitura abaixo foi ampliada para explicar o que os numeros significam e onde pode haver pressao menos visivel na tela.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-0 divide-y md:divide-y-0 md:divide-x divide-gray-100">
          {PERSONAS.map((persona) => (
            <div
              key={persona.key}
              className={`p-4 ${persona.fundo} vigi-focus-card`}
              tabIndex={0}
              role="group"
              aria-label={`Card de orientacoes para ${persona.titulo}`}
            >
              <p className={`text-sm font-bold mb-2 ${persona.tituloCor}`}>
                <span aria-hidden="true">{persona.icone}</span> <span className="sr-only">:</span> {persona.titulo}
              </p>
              <ul className="space-y-1.5">
                {(insights[persona.key] || []).map((item, index) => (
                  <li key={index} className="flex gap-2 text-sm text-gray-700">
                    <span className={`mt-0.5 shrink-0 ${persona.bulletCor}`}>•</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="border-t border-gray-100 px-5 py-4 bg-gray-50">
          <InsightsIaBloco
            perfil={perfil}
            ranking={ranking}
            risco={risco}
            textoIa={textoIa}
            foco="acao"
          />

          {textoIa && (
            <>
              <p
                className={`mt-4 text-sm text-gray-600 leading-relaxed whitespace-pre-line ${
                  !expandido ? "line-clamp-3" : ""
                }`}
              >
                <span className="font-medium text-gray-500">Texto-base da IA:</span>{" "}
                {textoIa}
              </p>
              <button
                onClick={() => setExpandido(!expandido)}
                className="mt-1 text-xs text-blue-500 hover:underline"
              >
                {expandido ? "Ver menos" : "Ver analise completa"}
              </button>
            </>
          )}
        </div>
      </div>

      <p className="text-xs text-gray-400 mt-2 text-center italic">
        Orientacoes baseadas no contexto epidemiologico local. Decisoes clinicas e operacionais seguem responsabilidade do profissional habilitado.
      </p>
    </div>
  );
}
