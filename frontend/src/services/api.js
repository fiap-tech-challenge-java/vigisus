import axios from "axios";

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || "http://localhost:8080",
});

const withSignal = (signal) => (signal ? { signal } : {});

export const buscarPorPergunta = (pergunta) =>
  api.post("/api/busca", { pergunta });

export const buscarPerfil = (coIbge, ano, doenca = "dengue", signal) =>
  api.get(`/api/perfil/${coIbge}`, {
    params: { ano, doenca },
    ...withSignal(signal),
  });

export const buscarRisco = (coIbge, signal) =>
  api.get(`/api/previsao-risco/${coIbge}`, withSignal(signal));

export const buscarHospitais = (coIbge, grav, signal) =>
  api.get("/api/encaminhar", {
    params: { municipio: coIbge, condicao: grav },
    ...withSignal(signal),
  });

// Busca ranking dos municipios com pior situacao
export const buscarSituacaoAtual = async (
  uf = "MG",
  top = 6,
  ano = new Date().getFullYear(),
  signal
) => {
  try {
    const res = await api.get(
      `/api/ranking?uf=${uf}&doenca=dengue&ano=${ano}&top=${top}&ordem=piores`,
      withSignal(signal)
    );
    return res.data;
  } catch (err) {
    console.warn("buscarSituacaoAtual falhou:", err?.response?.status, err?.message);
    return { ranking: [] };
  }
};

export const buscarRankingEstado = async (
  uf,
  ano = new Date().getFullYear(),
  doenca = "dengue",
  signal
) => {
  // top=853 cobre todos os municipios do maior estado (MG), retornando o estado completo
  const res = await api.get(
    `/api/ranking?uf=${uf}&doenca=${doenca}&ano=${ano}&top=853`,
    withSignal(signal)
  );
  return res.data;
};

export const buscarHistoricoEstado = async (
  uf,
  ano = new Date().getFullYear(),
  doenca = "dengue",
  signal
) => {
  const res = await api.get(
    `/api/ranking/estado-historico?uf=${uf}&doenca=${doenca}&ano=${ano}`,
    withSignal(signal)
  );
  return res.data;
};

export const buscarMunicipio = async (
  municipio,
  uf,
  doenca = "dengue",
  ano = null,
  signal
) => {
  const params = new URLSearchParams({ municipio, uf, doenca });
  if (ano) params.append("ano", ano);
  const res = await api.get(`/api/busca/perfil-direto?${params}`, withSignal(signal));
  return res.data;
};

export const buscarBrasil = async (doenca = "dengue", ano = null, signal) => {
  const params = new URLSearchParams({ doenca });
  if (ano) params.append("ano", ano);
  const res = await api.get(`/api/brasil/casos?${params}`, withSignal(signal));
  return res.data;
};

export const buscarRiscoBrasil = async (signal) => {
  try {
    const res = await api.get(
      "/api/previsao-risco/brasil/risco-agregado",
      withSignal(signal)
    );
    return res.data;
  } catch (err) {
    console.warn("buscarRiscoBrasil falhou:", err?.response?.status, err?.message);
    return null;
  }
};

export const buscarRiscoEstado = async (uf, signal) => {
  try {
    const res = await api.get(
      `/api/previsao-risco/estado/${uf}/risco-agregado`,
      withSignal(signal)
    );
    return res.data;
  } catch (err) {
    console.warn("buscarRiscoEstado falhou:", err?.response?.status, err?.message);
    return null;
  }
};

export const buscarHospitaisBrasilAgregado = async (signal) => {
  try {
    const res = await api.get(
      "/api/previsao-risco/brasil/hospitais-capitais",
      withSignal(signal)
    );
    return res.data || [];
  } catch (err) {
    console.warn("buscarHospitaisBrasilAgregado falhou:", err?.response?.status, err?.message);
    return [];
  }
};

export const buscarHospitaisEstadoRegiao = async (uf, signal) => {
  try {
    const res = await api.get(
      `/api/previsao-risco/estado/${uf}/hospitais-regiao`,
      withSignal(signal)
    );
    return res.data || [];
  } catch (err) {
    console.warn("buscarHospitaisEstadoRegiao falhou:", err?.response?.status, err?.message);
    return [];
  }
};

export default api;
