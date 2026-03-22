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
  const ANO_ATUAL = new Date().getFullYear();
  try {
    const res = await api.get(`/api/ranking?uf=${uf}&doenca=dengue&ano=${ANO_ATUAL}&top=${top}&ordem=piores`);
    return res.data;
  } catch (err) {
    console.warn("buscarSituacaoAtual falhou:", err?.response?.status, err?.message);
    return { ranking: [] };
  }
};

export const buscarRankingEstado = async (uf, ano = new Date().getFullYear()) => {
  // top=853 cobre todos os municípios do maior estado (MG), retornando o estado completo
  const res = await api.get(`/api/ranking?uf=${uf}&doenca=dengue&ano=${ano}&top=853`);
  return res.data;
};

export default api;
