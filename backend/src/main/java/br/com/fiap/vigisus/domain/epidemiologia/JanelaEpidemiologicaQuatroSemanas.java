package br.com.fiap.vigisus.domain.epidemiologia;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

public final class JanelaEpidemiologicaQuatroSemanas {

    private final int anoAtual;
    private final int anoAnterior;
    private final List<Integer> semanasAnoAtual;
    private final List<Integer> semanasAnoAnterior;
    private final List<SemanaEpidemiologica> semanasOrdenadas;

    private JanelaEpidemiologicaQuatroSemanas(
            int anoAtual,
            int anoAnterior,
            List<Integer> semanasAnoAtual,
            List<Integer> semanasAnoAnterior,
            List<SemanaEpidemiologica> semanasOrdenadas
    ) {
        this.anoAtual = anoAtual;
        this.anoAnterior = anoAnterior;
        this.semanasAnoAtual = List.copyOf(semanasAnoAtual);
        this.semanasAnoAnterior = List.copyOf(semanasAnoAnterior);
        this.semanasOrdenadas = List.copyOf(semanasOrdenadas);
    }

    public static JanelaEpidemiologicaQuatroSemanas daData(LocalDate dataReferencia) {
        WeekFields camposIso = WeekFields.ISO;
        int semanaAtual = dataReferencia.get(camposIso.weekOfWeekBasedYear());
        int anoAtual = dataReferencia.get(camposIso.weekBasedYear());
        int anoAnterior = anoAtual - 1;
        int ultimaSemanaAnoAnterior = ultimaSemanaIso(anoAnterior);

        List<Integer> semanasAnoAtual = new ArrayList<>();
        List<Integer> semanasAnoAnterior = new ArrayList<>();
        List<SemanaEpidemiologica> semanasOrdenadas = new ArrayList<>();

        for (int deslocamento = 3; deslocamento >= 0; deslocamento--) {
            int semanaCalculada = semanaAtual - deslocamento;
            if (semanaCalculada <= 0) {
                int semanaAnoAnterior = ultimaSemanaAnoAnterior + semanaCalculada;
                semanasAnoAnterior.add(semanaAnoAnterior);
                semanasOrdenadas.add(new SemanaEpidemiologica(anoAnterior, semanaAnoAnterior));
            } else {
                semanasAnoAtual.add(semanaCalculada);
                semanasOrdenadas.add(new SemanaEpidemiologica(anoAtual, semanaCalculada));
            }
        }

        return new JanelaEpidemiologicaQuatroSemanas(
                anoAtual,
                anoAnterior,
                semanasAnoAtual,
                semanasAnoAnterior,
                semanasOrdenadas
        );
    }

    public int anoAtual() {
        return anoAtual;
    }

    public int anoAnterior() {
        return anoAnterior;
    }

    public List<Integer> semanasAnoAtual() {
        return semanasAnoAtual;
    }

    public List<Integer> semanasAnoAnterior() {
        return semanasAnoAnterior;
    }

    public List<SemanaEpidemiologica> semanasOrdenadas() {
        return semanasOrdenadas;
    }

    public List<Integer> semanasMesmoPeriodoAnoAnterior() {
        if (semanasAnoAtual.isEmpty()) {
            return semanasAnoAnterior;
        }
        return semanasAnoAtual;
    }

    private static int ultimaSemanaIso(int ano) {
        LocalDate vinteEOitoDezembro = LocalDate.of(ano, 12, 28);
        return vinteEOitoDezembro.get(WeekFields.ISO.weekOfWeekBasedYear());
    }
}
