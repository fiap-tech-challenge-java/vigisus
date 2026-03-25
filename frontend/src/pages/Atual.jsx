import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import TopNav from "../components/TopNav";
import HeaderAlerta from "../components/HeaderAlerta";
import OQueFazerAgora from "../components/OQueFazerAgora";
import KpiCards from "../components/KpiCards";
import StatusRapido from "../components/StatusRapido";
import CurvaEpidemiologica from "../components/CurvaEpidemiologica";
import RiscoFuturo from "../components/RiscoFuturo";
import MapaEstado from "../components/MapaEstado";
import MapaHospitais from "../components/MapaHospitais";
import ResumoIa from "../components/ResumoIa";
import {
  buscarMunicipio,
  buscarRankingEstado,
  buscarBrasil,
  buscarHospitaisBrasilAgregado,
  buscarHospitaisEstadoRegiao,
  buscarRiscoBrasil,
  buscarRiscoEstado,
} from "../services/api";

const ANO_ATUAL = new Date().getFullYear();

function SectionTitle({ icone, titulo }) {
  return (
    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
      {icone} {titulo}
    </p>
  );
}

function SectionSkeleton({ linhas = 3 }) {
  return (
    <div className="bg-white rounded-xl p-4 animate-pulse space-y-3">
      {Array.from({ length: linhas }).map((_, i) => (
        <div
          key={i}
          className="h-4 bg-gray-200 rounded"
          style={{ width: `${70 + (i % 3) * 10}%` }}
        />
      ))}
    </div>
  );
}

function SectionErro() {
  return (
    <div className="bg-gray-100 border border-gray-200 rounded-xl p-4 text-sm text-gray-400 text-center">
      Dados indisponíveis
    </div>
  );
}

function mapHospital(h) {
  return {
    nome: h.noFantasia || h.nome || "Hospital sem nome",
    leitosSus: h.qtLeitosSus ?? h.leitosSus ?? null,
    telefone: h.nuTelefone || h.telefone || null,
    distanciaKm: h.distanciaKm ?? null,
    servicoInfectologia: h.servicoInfectologia ?? false,
    nuLatitude: h.nuLatitude ?? null,
    nuLongitude: h.nuLongitude ?? null,
  };
}

export default function Atual() {
  const [searchParams, setSearchParams] = useSearchParams();

  const hasUrlParams   = !!searchParams.get("municipio") || !!searchParams.get("uf");
  const municipioParam = searchParams.get("municipio");
  const ufParam        = searchParams.get("uf")        || "MG";
  const doencaParam    = searchParams.get("doenca")    || "dengue";

  const [dados, setDados]                = useState(null);
  const [rankingEstado, setRankingEstado] = useState([]);
  const [loading, setLoading]            = useState(false);
  const [erro, setErro]                  = useState(null);
  // "pending" = aguardando geo | "city" = carregar dados | "brasil" = estado neutro
  const [geoState, setGeoState]          = useState(hasUrlParams ? "city" : "pending");

  // Quando usuário pesquisa via TopNav enquanto estava no estado brasil
  useEffect(() => {
    if (hasUrlParams && geoState === "brasil") {
      setGeoState("city");
    }
  }, [hasUrlParams, geoState]);

  // Tenta detectar a cidade real do usuário uma única vez na abertura da página
  useEffect(() => {
    if (hasUrlParams) return; // já tem params → geoState já é "city"

    if (!navigator.geolocation) {
      setGeoState("brasil");
      return;
    }

    // Timeout de 5s → se geo demorar, exibe estado Brasil neutro
    const timer = setTimeout(() => setGeoState("brasil"), 5000);

    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        clearTimeout(timer);
        try {
          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse` +
            `?lat=${coords.latitude}&lon=${coords.longitude}&format=json`
          );
          const data = await res.json();
          const city =
            data.address?.city ||
            data.address?.town ||
            data.address?.municipality;
          // ISO3166-2-lvl4 retorna "BR-MG", "BR-SP", etc.
          const uf = (data.address?.["ISO3166-2-lvl4"] || "").replace("BR-", "");
          if (city && uf) {
            setSearchParams({ municipio: city, uf, doenca: "dengue" }, { replace: true });
            setGeoState("city");
          } else {
            setGeoState("brasil"); // coords válidas mas sem cidade reconhecida
          }
        } catch {
          setGeoState("brasil"); // Nominatim falhou
        }
      },
      () => {
        // Permissão negada ou erro de hardware → estado neutro Brasil
        clearTimeout(timer);
        setGeoState("brasil");
      },
      { timeout: 4500, maximumAge: 60_000 }
    );

    return () => clearTimeout(timer);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Carrega dados somente quando geoState === "city" ou geoState === "brasil"
  useEffect(() => {
    if (geoState === "city") {
      if (municipioParam) {
        carregar(municipioParam, ufParam, doencaParam);
      } else if (ufParam === "BR") {
        carregarBrasil(doencaParam);
      } else if (ufParam) {
        carregarEstado(ufParam, doencaParam);
      }
    } else if (geoState === "brasil" && !hasUrlParams) {
      carregarBrasil(doencaParam);
    }
  }, [municipioParam, ufParam, doencaParam, geoState, hasUrlParams]); // eslint-disable-line react-hooks/exhaustive-deps

  const carregar = async (municipio, uf, doenca) => {
    setLoading(true);
    setErro(null);
    try {
      const resp = await buscarMunicipio(municipio, uf, doenca, ANO_ATUAL);
      setDados(resp);
      const ufFinal = resp?.perfil?.uf || uf;
      const ranking = await buscarRankingEstado(ufFinal, ANO_ATUAL);
      setRankingEstado(ranking?.ranking || []);
    } catch {
      setErro("Município não encontrado. Verifique o nome e o estado.");
    } finally {
      setLoading(false);
    }
  };

  const carregarBrasil = async (doenca) => {
    setLoading(true);
    setErro(null);
    try {
      if (!searchParams.get("uf") || searchParams.get("uf") !== "BR") {
        setSearchParams({ uf: "BR", doenca }, { replace: true });
      }

      const [resp, riscoAg, hospitaisBrasil] = await Promise.all([
        buscarBrasil(doenca, ANO_ATUAL),
        buscarRiscoBrasil(),
        buscarHospitaisBrasilAgregado(),
      ]);
      
      setDados({
        perfil: {
          municipio: "Brasil",
          uf: "BR",
          total: resp.totalCasos,
          incidencia: resp.incidencia,
          classificacao: resp.classificacao,
          tendencia: resp.tendencia,
          semanas: resp.semanas,
          semanasAnoAnterior: resp.semanasAnoAnterior,
          doenca: doenca,
          ano: ANO_ATUAL,
        },
        textoIa: resp.textoIa,
        risco: riscoAg,
        encaminhamento: { hospitais: (hospitaisBrasil || []).map(mapHospital) },
        estadosPiores: resp.estadosPiores,
        municipiosPiores: resp.municipiosPiores,
      });
      setRankingEstado(resp.estadosPiores || []);
    } catch (e) {
      console.error("Erro ao carregar dados do Brasil:", e);
      setErro("Erro ao carregar dados do Brasil");
    } finally {
      setLoading(false);
    }
  };

  const carregarEstado = async (uf, doenca) => {
    setLoading(true);
    setErro(null);
    try {
      const [ranking, hospitais, riscoAg] = await Promise.all([
        buscarRankingEstado(uf, ANO_ATUAL),
        buscarHospitaisEstadoRegiao(uf),
        buscarRiscoEstado(uf),
      ]);
      
      // Calcula agregado do estado a partir do ranking
      const totalCasos = ranking?.ranking?.reduce((sum, m) => sum + (m.totalCasos || 0), 0) || 0;
      const populacao = ranking?.ranking?.reduce((sum, m) => sum + (m.populacao || 0), 0) || 1;
      const incidencia = populacao > 0 ? (totalCasos / populacao) * 100_000 : 0;
      
      const obterClassificacao = (incid) => {
        if (incid < 50) return "BAIXO";
        if (incid < 100) return "MODERADO";
        if (incid <= 300) return "ALTO";
        return "EPIDEMIA";
      };
      
      setDados({
        perfil: {
          municipio: uf,
          uf: uf,
          total: totalCasos,
          incidencia: incidencia,
          classificacao: obterClassificacao(incidencia),
          tendencia: "ESTAVEL",
          semanas: [],
          semanasAnoAnterior: [],
          doenca: doenca,
          ano: ANO_ATUAL,
        },
        textoIa: `Estado ${uf} registrou ${totalCasos} casos de ${doenca} em ${ANO_ATUAL}, com incidência de ${incidencia.toFixed(1)} por 100 mil habitantes.`,
        risco: riscoAg,
        encaminhamento: { hospitais: (hospitais || []).map(mapHospital) },
      });
      setRankingEstado(ranking?.ranking || []);
    } catch (e) {
      console.error("Erro ao carregar dados do estado:", e);
      setErro("Estado não encontrado. Verifique.");
    } finally {
      setLoading(false);
    }
  };

  const onBuscar = (municipio, uf, doenca) => {
    setSearchParams({ municipio, uf, doenca });
  };

  const perfil = dados?.perfil || {};

  const hoje = new Date();
  const em14dias = new Date(hoje.getTime() + 14 * 24 * 60 * 60 * 1000);
  const formatarData = (d) =>
    d.toLocaleDateString("pt-BR", { day: "2-digit", month: "short" });

  const perfilMapped = dados
    ? { ...perfil, totalCasos: perfil.total, incidencia100k: perfil.incidencia }
    : null;

  const encaminhamentoMapped = dados?.encaminhamento
    ? {
        ...dados.encaminhamento,
        hospitais: (dados.encaminhamento.hospitais || []).map(mapHospital),
      }
    : null;

  // Estado: aguardando geolocalização
  if (geoState === "pending") {
    return (
      <div className="min-h-screen bg-gray-50">
        <TopNav />
        <div className="flex flex-col items-center justify-center h-80 gap-3">
          <p className="text-gray-400 text-sm animate-pulse">
            🔍 Detectando sua localização...
          </p>
          <p className="text-xs text-gray-300">
            Ou pesquise um município acima
          </p>
        </div>
      </div>
    );
  }

  // Estado: localização negada / indisponível → carrega dados agregados do Brasil
  if (geoState === "brasil") {
    if (loading) {
      return (
        <div className="min-h-screen bg-gray-50">
          <TopNav />
          <div className="flex flex-col items-center justify-center h-[70vh] gap-5">
            <p className="text-gray-400 text-sm animate-pulse">
              🔍 Carregando dados do Brasil...
            </p>
          </div>
        </div>
      );
    }

    if (erro) {
      return (
        <div className="min-h-screen bg-gray-50">
          <TopNav />
          <div className="flex flex-col items-center justify-center h-[70vh] gap-5 text-center px-6">
            <div className="text-6xl">🇧🇷</div>
            <h2 className="text-xl font-bold text-gray-700">
              Dados Indisponíveis
            </h2>
            <p className="text-sm text-gray-500 max-w-sm">
              {erro}
            </p>
          </div>
        </div>
      );
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <TopNav />
      {erro && (
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="bg-red-50 border border-red-200 rounded-xl p-4
                          text-sm text-red-600">
            {erro}
          </div>
        </div>
      )}

      {/* Breadcrumb */}
      <div className="bg-white border-b border-gray-100 px-6 py-2 flex items-center justify-between">
        <p className="text-xs text-gray-400">
          {perfil?.municipio
            ? `${perfil.municipio} · ${perfil.uf} · ${perfil.doenca}`
            : "Carregando..."}
        </p>
        <span className="text-xs bg-blue-50 text-blue-600 px-2 py-1 rounded-full font-medium">
          📡 Atual & Previsão
        </span>
      </div>

      {/* 1. Cabeçalho com alerta */}
      {loading ? (
        <div className="px-6 py-5 bg-gray-200 animate-pulse">
          <div className="max-w-6xl mx-auto h-16 bg-gray-300 rounded" />
        </div>
      ) : erro ? (
        <div className="px-6 py-5">
          <SectionErro />
        </div>
      ) : (
        <HeaderAlerta perfil={perfilMapped} risco={dados?.risco} />
      )}

      <div className="max-w-6xl mx-auto px-6 py-6 space-y-8">

        {/* 2. O que fazer agora — tomada de decisão com IA */}
        <OQueFazerAgora
          perfil={perfilMapped}
          textoIa={dados?.textoIa}
          ranking={rankingEstado}
          risco={dados?.risco}
        />

        {/* 3. Status rápido — 3 métricas compactas */}
        <StatusRapido perfil={perfilMapped} risco={dados?.risco} />
        {/* 3. KPIs */}
        {loading ? (
          <SectionSkeleton linhas={2} />
        ) : erro ? (
          <SectionErro />
        ) : (
          <KpiCards perfil={perfilMapped} risco={dados?.risco} />
        )}

        {/* 4. Curva epidemiológica */}
        <section>
          <SectionTitle icone="📈" titulo="Evolução dos casos" />
          {loading ? (
            <SectionSkeleton linhas={5} />
          ) : erro ? (
            <SectionErro />
          ) : (
            <CurvaEpidemiologica perfil={perfil} />
          )}
        </section>

        {/* 5. Risco futuro (14 dias) */}
        <section>
          <SectionTitle
            icone="🌡️"
            titulo={`Risco climático — ${formatarData(hoje)} a ${formatarData(em14dias)}`}
          />
          {loading ? (
            <SectionSkeleton linhas={2} />
          ) : erro ? (
            <SectionErro />
          ) : (
            <RiscoFuturo risco={dados?.risco} />
          )}
        </section>

        {/* 6. Distribuição regional */}
        <section>
          <SectionTitle icone="🗺️" titulo="Distribuição regional" />
          {loading ? (
            <SectionSkeleton linhas={4} />
          ) : erro ? (
            <SectionErro />
          ) : (
            <MapaEstado
              uf={perfil?.uf}
              nivel={perfil?.uf === "BR" ? "brasil" : "estado"}
              coIbgeDestaque={perfil?.coIbge}
              ranking={rankingEstado}
              risco={dados?.risco}
            />
          )}
        </section>

        {/* 7. Infraestrutura / encaminhamento */}
        <section>
          <SectionTitle icone="🏥" titulo="Infraestrutura disponível" />
          {loading ? (
            <SectionSkeleton linhas={3} />
          ) : erro ? (
            <SectionErro />
          ) : (
            <MapaHospitais
              perfil={perfilMapped}
              encaminhamento={encaminhamentoMapped}
            />
          )}
        </section>

        {/* 8. Resumo IA */}
        <section>
          <SectionTitle icone="📋" titulo="Resumo operacional" />
          {loading ? (
            <SectionSkeleton linhas={4} />
          ) : erro ? (
            <SectionErro />
          ) : (
            <ResumoIa
              textoIa={dados?.textoIa}
              perfil={perfilMapped}
              ranking={rankingEstado}
              risco={dados?.risco}
            />
          )}
        </section>

      </div>
    </div>
  );
}
