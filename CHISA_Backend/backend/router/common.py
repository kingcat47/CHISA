from __future__ import annotations

import json
from pathlib import Path
from typing import Any
from uuid import uuid4

from fastapi import HTTPException

from ..utils.file_reader import read_file
from ..schema.state import FileState, Node
from ..utils.system_manager import load_file_state, save_file_state

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = PROJECT_ROOT / "data"
FILES_ROOT = DATA_DIR / "files"
CONFIG_PATH = DATA_DIR / "config.json"
STATE_PATH = DATA_DIR / "file.json"


def ensure_files_root() -> None:
	FILES_ROOT.mkdir(parents=True, exist_ok=True)


def load_state() -> FileState:
	return load_file_state(STATE_PATH)


def save_state(state: FileState) -> None:
	save_file_state(STATE_PATH, state)


def fresh_state() -> FileState:
	state = FileState()
	state.nodes["root"] = Node(name="root", type="folder", parent=None, children=[])
	return state


def initialize_state() -> None:
	ensure_files_root()
	save_state(fresh_state())


def load_config() -> dict[str, Any]:
	if not CONFIG_PATH.exists():
		return {}
	return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))


def save_config(config: dict[str, Any]) -> None:
	CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
	CONFIG_PATH.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")


def root_id(state: FileState) -> str:
	for node_id, node in state.nodes.items():
		if node.parent is None and node.type == "folder":
			return node_id
	state.nodes["root"] = Node(name="files", type="folder", parent=None, children=[])
	return "root"


def get_node(state: FileState, node_id: str) -> Node:
	node = state.nodes.get(node_id)
	if not node:
		raise HTTPException(status_code=404, detail="Node not found")
	return node


def relative_path_for_node(state: FileState, node_id: str) -> Path:
	node = get_node(state, node_id)
	parts: list[str] = []
	current = node
	while current.parent:
		parts.append(current.name)
		current = get_node(state, current.parent)
	return Path(*reversed(parts))


def safe_files_path(relative_path: str | Path) -> Path:
	root = FILES_ROOT.resolve()
	target = (FILES_ROOT / relative_path).resolve()
	if not str(target).startswith(str(root)):
		raise ValueError("Invalid path access")
	return target


def node_by_path(state: FileState, relative_path: str) -> str | None:
	current_id = root_id(state)
	if not relative_path:
		return current_id
	for part in Path(relative_path).parts:
		current_node = state.nodes.get(current_id)
		if not current_node:
			return None
		next_id = None
		for child_id in current_node.children:
			child = state.nodes.get(child_id)
			if child and child.name == part:
				next_id = child_id
				break
		if not next_id:
			return None
		current_id = next_id
	return current_id


def ensure_folder_path(state: FileState, relative_path: str) -> str:
	current_id = root_id(state)
	for part in Path(relative_path).parts:
		current_node = state.nodes[current_id]
		match_id = None
		for child_id in current_node.children:
			child = state.nodes.get(child_id)
			if child and child.type == "folder" and child.name == part:
				match_id = child_id
				break
		if not match_id:
			match_id = str(uuid4())
			state.nodes[match_id] = Node(name=part, type="folder", parent=current_id, children=[])
			current_node.children.append(match_id)
		current_id = match_id
	return current_id


def remove_node_recursive(state: FileState, node_id: str) -> None:
	node = state.nodes.get(node_id)
	if not node:
		return
	for child_id in list(node.children):
		remove_node_recursive(state, child_id)
	if node.parent and node.parent in state.nodes:
		parent = state.nodes[node.parent]
		if node_id in parent.children:
			parent.children.remove(node_id)
	state.nodes.pop(node_id, None)


def unique_filename(root: Path, filename: str) -> str:
	candidate = Path(filename).name
	path = root / candidate
	if not path.exists():
		return candidate
	stem = path.stem
	suffix = path.suffix
	index = 1
	while True:
		new_name = f"{stem}_{index}{suffix}"
		if not (root / new_name).exists():
			return new_name
		index += 1


def read_content_from_path(file_path: Path) -> str:
	config = load_config()
	max_pages = int(config.get("max_pages", 5))
	max_chars = int(config.get("max_chars", 4000))
	return read_file(file_path, max_pages=max_pages, max_chars=max_chars)


def aggregate_text(state: FileState, node_id: str) -> str:
	node = get_node(state, node_id)
	if node.type == "file":
		return node.summary or node.content or ""
	parts: list[str] = []
	for child_id in node.children:
		text = aggregate_text(state, child_id)
		if text:
			parts.append(text)
	return "\n\n".join(parts)


def check_sibling_duplicate(state: FileState, parent_id: str, name: str) -> None:
	parent = get_node(state, parent_id)
	for child_id in parent.children:
		child = state.nodes.get(child_id)
		if child and child.name == name:
			raise HTTPException(status_code=409, detail="Duplicate name in same folder")


def http_error_from_exception(exc: Exception) -> HTTPException:
	if isinstance(exc, FileNotFoundError):
		return HTTPException(status_code=404, detail=str(exc))
	if isinstance(exc, FileExistsError):
		return HTTPException(status_code=409, detail=str(exc))
	if isinstance(exc, ValueError):
		return HTTPException(status_code=400, detail=str(exc))
	return HTTPException(status_code=500, detail="Internal server error")