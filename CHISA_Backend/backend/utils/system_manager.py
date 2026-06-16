import json
from pathlib import Path

from pydantic import ValidationError

from ..schema.state import FileState


def _write_file_state(file_path: Path, state: FileState) -> None:
    file_path.parent.mkdir(parents=True, exist_ok=True)
    payload = state.model_dump(mode="json")
    file_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def load_file_state(file_path: str | Path, default_state: FileState | None = None) -> FileState:
    path = Path(file_path)
    default_state = default_state or FileState()
    if not path.exists():
        _write_file_state(path, default_state)
        return default_state
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
        return FileState.model_validate(payload)
    except (json.JSONDecodeError, ValidationError):
        _write_file_state(path, default_state)
        return default_state


def save_file_state(file_path: str | Path, state: FileState) -> None:
    _write_file_state(Path(file_path), state)
