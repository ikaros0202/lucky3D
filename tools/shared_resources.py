"""Install a pinned public resource package in a shared, verified user cache."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import tempfile
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
import zipfile


def digest(data):
    return hashlib.sha256(data).hexdigest()


def check_cache(directory, files):
    actual = {path.relative_to(directory).as_posix() for path in directory.rglob('*') if path.is_file()}
    if actual != set(files) or any(path.is_symlink() for path in directory.rglob('*')):
        raise ValueError('Resource cache has unexpected or missing files; choose a fresh FC3D_CACHE_DIR.')
    for name, expected in files.items():
        if digest((directory / name).read_bytes()) != expected:
            raise ValueError('Resource cache SHA-256 differs; choose a fresh FC3D_CACHE_DIR.')


def resolve(lock_path=None, cache_root=None):
    lock_path = Path(lock_path) if lock_path else Path(__file__).resolve().parents[1] / 'shared-resources.json'
    config = json.loads(lock_path.read_text(encoding='utf-8'))
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', config['version']):
        raise ValueError('Invalid resource version')
    if not re.fullmatch(r'[a-f0-9]{64}', config['sha256']):
        raise ValueError('Invalid package SHA-256')
    expected_url = f"https://github.com/ikaros0202/SanXu/releases/download/tools-v{config['version']}/fc3d-tools-{config['version']}.zip"
    if config['url'] != expected_url:
        raise ValueError('Unexpected resource download URL')
    files = config['files']
    if not files:
        raise ValueError('Empty resource manifest')
    for name, expected in files.items():
        path = PurePosixPath(name)
        if path.is_absolute() or '..' in path.parts or '\\' in name or ':' in name or path.as_posix() != name:
            raise ValueError('Unsafe resource path')
        if not re.fullmatch(r'[a-f0-9]{64}', expected):
            raise ValueError('Invalid file SHA-256')
    if cache_root is None:
        cache_root = os.environ.get('FC3D_CACHE_DIR') or Path.home() / '.cache/fc3d-tools'
    cache_root = Path(cache_root).expanduser().resolve()
    cache_root.mkdir(parents=True, exist_ok=True)
    directory = cache_root / (config['version'] + '-' + config['sha256'])
    guard = cache_root / ('.' + directory.name + '.lock')
    deadline = time.monotonic() + 30
    while True:
        try:
            guard.mkdir()
            break
        except FileExistsError:
            if time.monotonic() >= deadline:
                raise TimeoutError(f'Resource cache is locked: {guard}. Check for an active installer before removing a stale lock.')
            time.sleep(0.1)
    try:
        if directory.exists():
            check_cache(directory, files)
            return directory
        request = Request(config['url'], headers={'User-Agent': 'FC3D-resource-installer/1.0'})
        for attempt in range(3):
            try:
                with urlopen(request, timeout=30) as response:
                    payload = response.read(10 * 1024 * 1024 + 1)
                break
            except (URLError, TimeoutError) as error:
                if isinstance(error, HTTPError) and error.code != 429 and error.code < 500:
                    raise
                if attempt == 2:
                    raise
                time.sleep(attempt + 1)
        if len(payload) > 10 * 1024 * 1024 or digest(payload) != config['sha256']:
            raise ValueError('Resource package SHA-256 mismatch or package too large')
        with tempfile.TemporaryDirectory(prefix='.install-', dir=cache_root) as temporary:
            staged = Path(temporary) / 'resources'
            staged.mkdir()
            with zipfile.ZipFile(io.BytesIO(payload)) as archive:
                entries = archive.infolist()
                if len(entries) != len(files) or {entry.filename for entry in entries} != set(files):
                    raise ValueError('Resource archive entries differ from pinned manifest')
                if sum(entry.file_size for entry in entries) > 10 * 1024 * 1024:
                    raise ValueError('Resource archive uncompressed size too large')
                for entry in entries:
                    target = staged / entry.filename
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(archive.read(entry))
            check_cache(staged, files)
            staged.rename(directory)
        return directory
    finally:
        guard.rmdir()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--lock', type=Path)
    parser.add_argument('--cache', type=Path)
    args = parser.parse_args()
    print(resolve(args.lock, args.cache))
