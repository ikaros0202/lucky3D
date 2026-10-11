"""Compatibility entry point for the shared trial seed tool."""
from pathlib import Path
import runpy
_shared = Path(__file__).resolve().parents[2] / 'fc3d-resources/tools/fetch-caiba-trial-seed.py'
if not _shared.is_file():
    raise FileNotFoundError('Run tools/setup-shared-resources.ps1 first.')
if __name__ == '__main__':
    runpy.run_path(str(_shared), run_name='__main__')
else:
    globals().update(runpy.run_path(str(_shared)))
