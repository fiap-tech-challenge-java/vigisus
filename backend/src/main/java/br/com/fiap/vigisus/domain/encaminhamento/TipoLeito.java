package br.com.fiap.vigisus.domain.encaminhamento;

public record TipoLeito(String codigo) {

    public static final String CODIGO_CLINICO = "74";
    public static final String CODIGO_UTI = "81";

    public TipoLeito {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("codigo de leito nao pode ser vazio");
        }
        codigo = codigo.trim();
    }

    public static TipoLeito of(String codigo) {
        return new TipoLeito(codigo);
    }

    public static TipoLeito clinico() {
        return new TipoLeito(CODIGO_CLINICO);
    }

    public static TipoLeito uti() {
        return new TipoLeito(CODIGO_UTI);
    }

    public static TipoLeito porGravidade(String gravidade) {
        if (gravidade == null) {
            return clinico();
        }
        String valor = gravidade.strip().toLowerCase();
        if ("grave".equals(valor) || "critica".equals(valor) || "cr\u00edtica".equals(valor)) {
            return uti();
        }
        return clinico();
    }

    public boolean ehClinico() {
        return CODIGO_CLINICO.equals(codigo);
    }

    public boolean ehUti() {
        return CODIGO_UTI.equals(codigo);
    }
}
