"""Run the pinned trial seed tool from the verified user cache."""
import importlib.util
from pathlib import Path
import runpy
_loader_spec = importlib.util.spec_from_file_location('_fc3d_resources', Path(__file__).with_name('shared_resources.py'))
_loader = importlib.util.module_from_spec(_loader_spec)
_loader_spec.loader.exec_module(_loader)
_shared = _loader.resolve() / 'tools/fetch-caiba-trial-seed.py'
if __name__ == '__main__':
    runpy.run_path(str(_shared), run_name='__main__')
else:
    globals().update(runpy.run_path(str(_shared)))
