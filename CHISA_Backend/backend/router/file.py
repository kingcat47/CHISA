from __future__ import annotations

import shutil
from pathlib import Path
from typing import Any
from uuid import UUID, uuid4

from fastapi import APIRouter, Body, File, HTTPException, Path as PathParam, Query, UploadFile
from pydantic import BaseModel, Field

from ..utils.file_manager import FileSystemManager
from ..utils.system_manager import Node
from .common import (
	FILES_ROOT,
	check_sibling_duplicate,
	ensure_files_root,
	ensure_folder_path,
	http_error_from_exception,
	get_node,
	load_state,
	node_by_path,
	read_content_from_path,
	relative_path_for_node,
	remove_node_recursive,
	root_id,
	save_state,
	unique_filename,
)

router = APIRouter(prefix="/files", tags=["files"])


class RenamePayload(BaseModel):
	new_name: str = Field(..., min_length=1)


class FolderCreatePayload(BaseModel):
	path: str = Field(..., min_length=1)


class MoveFolderPayload(BaseModel):
	target_parent_id: UUID | None = None
	target_parent_path: str | None = None


class MoveFilePayload(BaseModel):
	target_parent_id: UUID | None = None
	target_parent_path: str | None = None


@router.post("/files/upload")
def upload_file(file: UploadFile = File(...)):
	ensure_files_root()
	state = load_state()
	root_node_id = root_id(state)

	try:
		safe_name = unique_filename(FILES_ROOT, file.filename or "upload")
		target_path = FILES_ROOT / safe_name
		with target_path.open("wb") as handle:
			shutil.copyfileobj(file.file, handle)
		content = read_content_from_path(target_path)
	except Exception as exc:
		raise http_error_from_exception(exc)

	node_id = str(uuid4())
	state.nodes[node_id] = Node(
		name=safe_name,
		type="file",
		parent=root_node_id,
		children=[],
		content=content,
	)
	state.nodes[root_node_id].children.append(node_id)
	save_state(state)
	return {"id": node_id, "name": safe_name}


@router.patch("/files/{id}/move")
def move_file(id: UUID = PathParam(...), payload: MoveFilePayload = Body(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")
	target_parent_path = payload.target_parent_path
	if payload.target_parent_id:
		target_parent_path = str(relative_path_for_node(state, str(payload.target_parent_id)))
	if not target_parent_path:
		raise HTTPException(status_code=400, detail="Target parent not provided")
	source_rel = relative_path_for_node(state, node_id)
	source_path = FILES_ROOT / source_rel
	target_parent = FILES_ROOT / target_parent_path
	if not target_parent.exists() or not target_parent.is_dir():
		raise HTTPException(status_code=404, detail="Target folder not found")
	destination = target_parent / node.name
	if destination.exists():
		raise HTTPException(status_code=409, detail="Destination already exists")
	try:
		shutil.move(str(source_path), str(destination))
	except Exception as exc:
		raise http_error_from_exception(exc)
	if node.parent and node.parent in state.nodes:
		parent = state.nodes[node.parent]
		if node_id in parent.children:
			parent.children.remove(node_id)
	target_parent_id = ensure_folder_path(state, target_parent_path)
	node.parent = target_parent_id
	state.nodes[target_parent_id].children.append(node_id)
	save_state(state)
	return {
		"status": "success",
		"action": "move_file",
		"source": str(source_rel),
		"destination": str(Path(target_parent_path) / node.name),
	}


@router.delete("/files/{id}")
def delete_file(id: UUID = PathParam(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")
	relative_path = relative_path_for_node(state, node_id)
	manager = FileSystemManager(str(FILES_ROOT))
	try:
		result = manager.delete_file(str(relative_path))
	except Exception as exc:
		raise http_error_from_exception(exc)
	remove_node_recursive(state, node_id)
	save_state(state)
	return result


@router.patch("/files/{id}/rename")
def rename_file(id: UUID = PathParam(...), payload: RenamePayload = Body(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")
	relative_path = relative_path_for_node(state, node_id)
	manager = FileSystemManager(str(FILES_ROOT))
	try:
		result = manager.rename_file(str(relative_path), payload.new_name)
	except Exception as exc:
		raise http_error_from_exception(exc)
	node.name = payload.new_name
	save_state(state)
	return result


@router.get("/files/{id}/describe")
def get_describe(id: UUID = PathParam(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")
	return {"id": node_id, "description": node.summary}


@router.post("/folders")
def make_folder(payload: FolderCreatePayload = Body(...)):
	state = load_state()
	path = Path(payload.path)
	parent_path = str(path.parent) if str(path.parent) != "." else ""
	parent_id = node_by_path(state, parent_path) or root_id(state)
	check_sibling_duplicate(state, parent_id, path.name)
	manager = FileSystemManager(str(FILES_ROOT))
	try:
		result = manager.create_folder(payload.path)
	except Exception as exc:
		raise http_error_from_exception(exc)
	ensure_folder_path(state, payload.path)
	save_state(state)
	return result


@router.patch("/folders/{id}/rename")
def rename_folder(id: UUID = PathParam(...), payload: RenamePayload = Body(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "folder":
		raise HTTPException(status_code=400, detail="Node is not a folder")
	relative_path = relative_path_for_node(state, node_id)
	manager = FileSystemManager(str(FILES_ROOT))
	try:
		result = manager.rename_folder(str(relative_path), payload.new_name)
	except Exception as exc:
		raise http_error_from_exception(exc)
	node.name = payload.new_name
	save_state(state)
	return result


@router.patch("/folder/{id}/move")
def move_folder(id: UUID = PathParam(...), payload: MoveFolderPayload = Body(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "folder":
		raise HTTPException(status_code=400, detail="Node is not a folder")
	target_parent_path = payload.target_parent_path
	if payload.target_parent_id:
		target_parent_path = str(relative_path_for_node(state, str(payload.target_parent_id)))
	if not target_parent_path:
		raise HTTPException(status_code=400, detail="Target parent not provided")
	source_path = str(relative_path_for_node(state, node_id))
	manager = FileSystemManager(str(FILES_ROOT))
	try:
		result = manager.move_folder(source_path, target_parent_path)
	except Exception as exc:
		raise http_error_from_exception(exc)
	if node.parent and node.parent in state.nodes:
		parent = state.nodes[node.parent]
		if node_id in parent.children:
			parent.children.remove(node_id)
	target_parent_id = ensure_folder_path(state, target_parent_path)
	node.parent = target_parent_id
	state.nodes[target_parent_id].children.append(node_id)
	save_state(state)
	return result


@router.delete("/folders/{id}")
def delete_folder(id: UUID = PathParam(...), recursive: bool = Query(True)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "folder":
		raise HTTPException(status_code=400, detail="Node is not a folder")
	relative_path = relative_path_for_node(state, node_id)
	manager = FileSystemManager(str(FILES_ROOT))
	try:
		result = manager.delete_folder(str(relative_path), recursive=recursive)
	except Exception as exc:
		raise http_error_from_exception(exc)
	remove_node_recursive(state, node_id)
	save_state(state)
	return result


@router.post("/restruct/apply")
def apply_restruct(command: dict[str, Any] = Body(...)):
	manager = FileSystemManager(str(FILES_ROOT))
	state = load_state()
	operations = command.get("operations") or command.get("actions") or command
	if not isinstance(operations, list):
		raise HTTPException(status_code=400, detail="Invalid restructure payload")
	results = []
	for op in operations:
		if not isinstance(op, dict):
			raise HTTPException(status_code=400, detail="Invalid operation format")
		action = op.get("action")
		try:
			if action == "create_folder":
				result = manager.create_folder(op["path"])
				ensure_folder_path(state, op["path"])
			elif action == "delete_folder":
				result = manager.delete_folder(op["path"], recursive=op.get("recursive", True))
				node_id = node_by_path(state, op["path"])
				if node_id:
					remove_node_recursive(state, node_id)
			elif action == "rename_folder":
				result = manager.rename_folder(op["path"], op["new_name"])
				node_id = node_by_path(state, op["path"])
				if node_id and node_id in state.nodes:
					state.nodes[node_id].name = op["new_name"]
			elif action == "move_folder":
				result = manager.move_folder(op["source_path"], op["target_parent"])
				node_id = node_by_path(state, op["source_path"])
				if node_id:
					target_id = ensure_folder_path(state, op["target_parent"])
					current_parent = state.nodes[node_id].parent
					if current_parent and current_parent in state.nodes:
						parent = state.nodes[current_parent]
						if node_id in parent.children:
							parent.children.remove(node_id)
					state.nodes[node_id].parent = target_id
					state.nodes[target_id].children.append(node_id)
			elif action == "delete_file":
				result = manager.delete_file(op["path"])
				node_id = node_by_path(state, op["path"])
				if node_id:
					remove_node_recursive(state, node_id)
			elif action == "rename_file":
				result = manager.rename_file(op["path"], op["new_name"])
				node_id = node_by_path(state, op["path"])
				if node_id and node_id in state.nodes:
					state.nodes[node_id].name = op["new_name"]
			else:
				raise HTTPException(status_code=400, detail=f"Unknown action: {action}")
		except HTTPException:
			raise
		except Exception as exc:
			raise http_error_from_exception(exc)
		results.append(result)
	save_state(state)
	return {"results": results}

