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
  const res = await api.get(`/api/ranking?uf=${uf}&doenca=dengue&ano=2024&top=${top}&ordem=piores`);
  return res.data;
};

export const buscarRankingEstado = async (uf, ano = 2024) => {
  // top=853 cobre todos os municípios do maior estado (MG), retornando o estado completo
  const res = await api.get(`/api/ranking?uf=${uf}&doenca=dengue&ano=${ano}&top=853`);
  return res.data;
};

export default api;
