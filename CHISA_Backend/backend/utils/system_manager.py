import json
from pathlib import Path
from typing import Literal
from uuid import NAMESPACE_URL, uuid5

from pydantic import BaseModel, Field, ValidationError


class Node(BaseModel):
    name: str
    type: Literal["folder", "file"]
    parent: str | None = None
    children: list[str] = Field(default_factory=list)
    summary: str | None = None
    content: str | None = None


class FileState(BaseModel):
    nodes: dict[str, Node] = Field(default_factory=dict)
    file_version: int = 1


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


def _uuid_for_file(path: Path) -> str:
    content = path.read_bytes()
    return str(uuid5(NAMESPACE_URL, content.hex()))


def _uuid_from_entry(entry: dict[str, object]) -> str | None:
    value = entry.get("uuid")
    return str(value) if value else None


def _build_snapshot(root_dir: str | Path, *, exclude_paths: list[str | Path] | None = None) -> dict[str, dict[str, object]]:
    root = Path(root_dir)
    excludes = {Path(path).resolve() for path in (exclude_paths or [])}
    snapshot: dict[str, dict[str, object]] = {}
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if path.resolve() in excludes:
            continue
        relative_path = str(path.relative_to(root))
        stat = path.stat()
        snapshot[relative_path] = {
            "uuid": _uuid_for_file(path),
            "size": stat.st_size,
            "mtime": stat.st_mtime,
        }
    return snapshot


def _detect_moves(prev_files: dict[str, dict[str, object]], curr_files: dict[str, dict[str, object]]):
    added = {path for path in curr_files if path not in prev_files}
    removed = {path for path in prev_files if path not in curr_files}

    removed_by_uuid: dict[str, list[str]] = {}
    for path in removed:
        entry_uuid = _uuid_from_entry(prev_files[path] or {})
        if not entry_uuid:
            continue
        removed_by_uuid.setdefault(entry_uuid, []).append(path)

    added_by_uuid: dict[str, list[str]] = {}
    for path in added:
        entry_uuid = _uuid_from_entry(curr_files[path] or {})
        if not entry_uuid:
            continue
        added_by_uuid.setdefault(entry_uuid, []).append(path)

    moved = []
    for file_uuid, old_paths in removed_by_uuid.items():
        new_paths = added_by_uuid.get(file_uuid, [])
        while old_paths and new_paths:
            old_path = old_paths.pop()
            new_path = new_paths.pop()
            moved.append({"from": old_path, "to": new_path})
            removed.discard(old_path)
            added.discard(new_path)

    return added, removed, moved


def get_system_changes(
    root_dir: str | Path,
    config_path: str | Path,
    previous_state: dict[str, object] | None = None,
) -> tuple[dict[str, object], dict[str, object]]:
    config_file = Path(config_path)
    current_state = {
        "files": _build_snapshot(root_dir, exclude_paths=[config_file]),
        "config_uuid": str(uuid5(NAMESPACE_URL, config_file.read_bytes().hex())) if config_file.exists() else None,
    }

    changes = {
        "added": [],
        "removed": [],
        "modified": [],
        "moved": [],
        "config_changed": False,
    }

    if not previous_state:
        return current_state, changes

    prev_files = previous_state.get("files", {})
    curr_files = current_state["files"]
    added, removed, moved = _detect_moves(prev_files, curr_files)
    modified = {
        path
        for path in curr_files
        if path in prev_files and _uuid_from_entry(curr_files[path]) != _uuid_from_entry(prev_files[path])
    }

    changes["added"] = sorted(added)
    changes["removed"] = sorted(removed)
    changes["modified"] = sorted(modified)
    changes["moved"] = sorted(moved, key=lambda item: (item["from"], item["to"]))
    changes["config_changed"] = previous_state.get("config_uuid") != current_state.get("config_uuid")

    return current_state, changes