const INSIGHTS = {
  EPIDEMIA: {
    medico: [
      "Alta incidência — atenção redobrada a casos febris",
      "Tendência crescente sugere aumento de atendimentos nos próximos dias",
      "Monitorar sinais de agravamento: dor abdominal, vômitos, hipotensão",
    ],
    gestor: [
      "Incidência elevada indica sobrecarga potencial nas unidades",
      "Cenário sugere necessidade de reforço de equipe e observação",
      "Verificar disponibilidade de insumos: soro, NS1, hemograma",
      "Considerar comunicação à Vigilância Epidemiológica Municipal",
    ],
    cidadao: [
      "Região com alta circulação da doença",
      "Eliminar focos de água parada em casa e arredores",
      "Buscar atendimento imediatamente em caso de febre acima de 38°C",
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
    titulo: "Para atendimento",
    borda: "border-blue-400",
    fundo: "bg-blue-50",
    titulo_cor: "text-blue-800",
    bullet_cor: "text-blue-500",
  },
  {
    key: "gestor",
    icone: "🏥",
    titulo: "Para gestão",
    borda: "border-orange-400",
    fundo: "bg-orange-50",
    titulo_cor: "text-orange-800",
    bullet_cor: "text-orange-500",
  },
  {
    key: "cidadao",
    icone: "👤",
    titulo: "Para a população",
    borda: "border-green-400",
    fundo: "bg-green-50",
    titulo_cor: "text-green-800",
    bullet_cor: "text-green-500",
  },
];

export default function InterpretacaoOperacional({ perfil }) {
  const nivel = perfil?.classificacao || "BAIXO";
  const insights = INSIGHTS[nivel] || INSIGHTS.BAIXO;

  return (
    <div className="px-6 py-5 max-w-6xl mx-auto">
      <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
        🧠 O que isso significa
      </p>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {PERSONAS.map((p) => (
          <div key={p.key}
            className={`rounded-xl border-l-4 ${p.borda} ${p.fundo} p-4`}>
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

      <p className="text-xs text-gray-400 mt-3 text-center italic">
        Orientações baseadas no contexto epidemiológico local.
        Decisões clínicas e operacionais são de responsabilidade do profissional habilitado.
      </p>
    </div>
  );
}
