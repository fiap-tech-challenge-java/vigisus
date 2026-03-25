"""
run_all.py
Orquestra a execução completa do pipeline de ingestão de dados do VígiSUS.
Ordem de execução:
    1. ingest_municipios
    2. ingest_populacao
    3. ingest_planilhas_saude (ST/LT/SR/DENG em CSV/Excel)

Pode ser agendado via cron: 0 2 * * 0 (domingo às 2h)
"""

import logging
import time

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

STEPS = [
    ("ingest_municipios", "Municípios IBGE"),
    ("ingest_populacao", "População IBGE"),
    ("ingest_planilhas_saude", "Planilhas ST/LT/SR/DENG (CSV/Excel)"),
]


def run_step(module_name: str, label: str, step_index: int, total_steps: int) -> None:
    import importlib

    step_pct = (step_index / total_steps) * 100
    logger.info(
        ">>> [%d/%d | %.0f%%] Iniciando etapa: %s",
        step_index,
        total_steps,
        step_pct,
        label,
    )
    start = time.time()
    try:
        mod = importlib.import_module(module_name)
        mod.run()
        elapsed = time.time() - start
        logger.info(
            "<<< [%d/%d] Etapa '%s' concluída em %.1fs",
            step_index,
            total_steps,
            label,
            elapsed,
        )
    except Exception as exc:
        elapsed = time.time() - start
        logger.error(
            "<<< [%d/%d] Etapa '%s' falhou após %.1fs: %s",
            step_index,
            total_steps,
            label,
            elapsed,
            exc,
            exc_info=True,
        )
        raise


def run() -> None:
    logger.info("===== Pipeline VígiSUS iniciado =====")
    total_start = time.time()
    total_steps = len(STEPS)

    for idx, (module_name, label) in enumerate(STEPS, 1):
        run_step(module_name, label, idx, total_steps)
        overall_pct = (idx / total_steps) * 100
        logger.info("... Progresso geral do pipeline: %.0f%% (%d/%d)", overall_pct, idx, total_steps)

    total_elapsed = time.time() - total_start
    logger.info("===== Pipeline VígiSUS concluído em %.1fs =====", total_elapsed)


if __name__ == "__main__":
    run()
