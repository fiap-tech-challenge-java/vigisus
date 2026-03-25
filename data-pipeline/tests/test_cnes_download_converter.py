from __future__ import annotations

import importlib.util
import json
import pathlib
import shutil
import sys
import unittest
import uuid
import zipfile


ROOT_DIR = pathlib.Path(__file__).resolve().parents[1]
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))


def _load_converter_module():
    module_path = ROOT_DIR / "cnes_download_converter.py"
    spec = importlib.util.spec_from_file_location("cnes_download_converter", module_path)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


class CnesDownloadConverterTest(unittest.TestCase):
    def _create_workdir(self) -> pathlib.Path:
        base_dir = ROOT_DIR / "tests_tmp"
        base_dir.mkdir(exist_ok=True)
        workdir = base_dir / f"tmp_{uuid.uuid4().hex}"
        workdir.mkdir(parents=True, exist_ok=False)
        self.addCleanup(lambda: shutil.rmtree(workdir, ignore_errors=True))
        return workdir

    def test_convert_csv_to_json_streaming_gera_json_valido(self):
        converter = _load_converter_module()

        workdir = self._create_workdir()
        zip_path = workdir / "cnes_sp.zip"
        out_dir = workdir / "out"

        csv_bytes = (
            "CO_CNES;NO_RAZAO_SOCIAL;TP_UNIDADE\n"
            "123;Hospital Central;1\n"
            "456;UPA Norte;2\n"
        ).encode("latin-1")

        with zipfile.ZipFile(zip_path, "w") as zf:
            zf.writestr("dados.csv", csv_bytes)

        json_path = converter.convert_csv_to_json(str(zip_path), str(out_dir))

        with open(json_path, "r", encoding="utf-8") as fp:
            payload = json.load(fp)

        self.assertEqual(2, len(payload))
        self.assertEqual("123", payload[0]["CO_CNES"])
        self.assertEqual("UPA Norte", payload[1]["NO_RAZAO_SOCIAL"])

    def test_convert_csv_to_json_falha_quando_zip_nao_tem_csv(self):
        converter = _load_converter_module()

        workdir = self._create_workdir()
        zip_path = workdir / "sem_csv.zip"

        with zipfile.ZipFile(zip_path, "w") as zf:
            zf.writestr("arquivo.txt", b"nao eh csv")

        with self.assertRaises(ValueError):
            converter.convert_csv_to_json(str(zip_path), str(workdir))


if __name__ == "__main__":
    unittest.main()
