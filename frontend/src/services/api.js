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

export default api;
