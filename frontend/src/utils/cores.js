export const CORES = {
  EPIDEMIA:    { bg: "bg-red-600",    text: "text-white",  badge: "bg-red-100 text-red-800",       hex: "#DC2626" },
  ALTO:        { bg: "bg-orange-500", text: "text-white",  badge: "bg-orange-100 text-orange-800",  hex: "#EA580C" },
  MODERADO:    { bg: "bg-yellow-500", text: "text-white",  badge: "bg-yellow-100 text-yellow-800",  hex: "#CA8A04" },
  BAIXO:       { bg: "bg-green-600",  text: "text-white",  badge: "bg-green-100 text-green-800",    hex: "#16A34A" },
  INDISPONIVEL:{ bg: "bg-gray-400",   text: "text-white",  badge: "bg-gray-100 text-gray-600",      hex: "#6B7280" },
};

export const getCor = (classificacao) =>
  CORES[classificacao?.toUpperCase()] || CORES.INDISPONIVEL;

export const TENDENCIA = {
  CRESCENTE:   { icone: "↗", cor: "text-red-600",   label: "Crescente" },
  ESTAVEL:     { icone: "→", cor: "text-gray-500",  label: "Estável" },
  DECRESCENTE: { icone: "↘", cor: "text-green-600", label: "Decrescente" },
};
