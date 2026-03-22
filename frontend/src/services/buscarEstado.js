import api from './api';

// Busca agregado de um estado
export const buscarEstadoAgregado = async (uf, doenca = 'dengue', ano = null) => {
  const params = new URLSearchParams({ doenca, top: 853 });  // top=853 pega todos os municípios
  if (ano) params.append('ano', ano);
  
  try {
    const res = await api.get(`/api/ranking?uf=${uf}&${params}`);
    return res.data;
  } catch (err) {
    console.error('Erro ao buscar ranking do estado:', err);
    throw err;
  }
};

export default buscarEstadoAgregado;
