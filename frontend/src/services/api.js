import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
});

export const buscarPorPergunta = (pergunta) =>
  api.post('/api/busca', { pergunta });

export const buscarPerfil = (coIbge, ano) =>
  api.get(`/api/perfil/${coIbge}`, { params: { ano } });

export const buscarRisco = (coIbge) =>
  api.get(`/api/previsao-risco/${coIbge}`);

export const buscarHospitais = (coIbge, grav) =>
  api.get('/api/encaminhar', { params: { municipio: coIbge, condicao: grav } });

// Busca ranking dos municípios com pior situação
export const buscarSituacaoAtual = async (uf = "MG", top = 6) => {
  try {
    const res = await api.get(`/api/ranking?uf=${uf}&doenca=dengue&ano=2024&top=${top}&ordem=piores`);
    return res.data;
  } catch (err) {
    console.warn("buscarSituacaoAtual falhou:", err?.response?.status, err?.message);
    return { ranking: [] };
  }
};

export const buscarRankingEstado = async (uf, ano = new Date().getFullYear(), doenca = "dengue") => {
  // top=853 cobre todos os municípios do maior estado (MG), retornando o estado completo
  const res = await api.get(`/api/ranking?uf=${uf}&doenca=${doenca}&ano=${ano}&top=853`);
  return res.data;
};

export const buscarHistoricoEstado = async (uf, ano = new Date().getFullYear(), doenca = "dengue") => {
  const res = await api.get(`/api/ranking/estado-historico?uf=${uf}&doenca=${doenca}&ano=${ano}`);
  return res.data;
};

export const buscarMunicipio = async (
  municipio, uf, doenca = "dengue", ano = null
) => {
  const params = new URLSearchParams({ municipio, uf, doenca });
  if (ano) params.append("ano", ano);
  const res = await api.get(`/api/busca/perfil-direto?${params}`);
  return res.data;
};

export const buscarBrasil = async (doenca = "dengue", ano = null) => {
  const params = new URLSearchParams({ doenca });
  if (ano) params.append("ano", ano);
  const res = await api.get(`/api/brasil/casos?${params}`);
  return res.data;
};

export const buscarRiscoBrasil = async () => {
  try {
    const res = await api.get('/api/previsao-risco/brasil/risco-agregado');
    return res.data;
  } catch (err) {
    console.warn("buscarRiscoBrasil falhou:", err?.response?.status, err?.message);
    return null;
  }
};

export const buscarRiscoEstado = async (uf) => {
  try {
    const res = await api.get(`/api/previsao-risco/estado/${uf}/risco-agregado`);
    return res.data;
  } catch (err) {
    console.warn("buscarRiscoEstado falhou:", err?.response?.status, err?.message);
    return null;
  }
};

export const buscarHospitaisBrasilAgregado = async () => {
  try {
    const res = await api.get('/api/previsao-risco/brasil/hospitais-capitais');
    return res.data || [];
  } catch (err) {
    console.warn("buscarHospitaisBrasilAgregado falhou:", err?.response?.status, err?.message);
    return [];
  }
};

export const buscarHospitaisEstadoRegiao = async (uf) => {
  try {
    const res = await api.get(`/api/previsao-risco/estado/${uf}/hospitais-regiao`);
    return res.data || [];
  } catch (err) {
    console.warn("buscarHospitaisEstadoRegiao falhou:", err?.response?.status, err?.message);
    return [];
  }
};

export default api;
