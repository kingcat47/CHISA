from __future__ import annotations

from pathlib import Path
from uuid import UUID

from fastapi import APIRouter, Body, HTTPException, Query
from pydantic import BaseModel

import json

from ..utils.llm import (
	generate_description as llm_generate_description,
	# generate_folder_name as llm_generate_folder_name,
	generate_name as llm_generate_name,
	# generate_structure as llm_generate_structure,
	guess_file_pos as llm_generate_path
)
from .common import (
	aggregate_text,
	FILES_ROOT,
	STATE_PATH,
	get_node,
	http_error_from_exception,
	load_config,
	load_state,
	relative_path_for_node,
	root_id,
	save_config,
	save_state,
	read_content_from_path,
)
from ..utils.tree_builder import build_tree

router = APIRouter(prefix="/llm", tags=["llm"])


class ContentPayload(BaseModel):
	content: str | None = None


class UserRulePayload(BaseModel):
	user_prompt: str | None = None


def _tree_nodes_for_building(state) -> dict[str, dict]:
	return {
		node_id: node.model_dump(mode="json") if hasattr(node, "model_dump") else dict(node)
		for node_id, node in state.nodes.items()
	}


@router.post("/generate/name")
def generate_name(id: UUID = Query(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")
	if not node.content:
		file_path = FILES_ROOT / relative_path_for_node(state, node_id)
		try:
			node.content = read_content_from_path(file_path)
		except Exception as exc:
			raise http_error_from_exception(exc)
		save_state(state)
	name = llm_generate_name(node.content).strip()
	original_suffix = Path(node.name).suffix
	if original_suffix and not name.endswith(original_suffix):
		name = f"{name}{original_suffix}"
	return {"id": node_id, "name": name}


@router.post("/generate/description")
def generate_description(id: UUID = Query(...)):
	state = load_state()
	node_id = str(id)
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")
	if not node.content:
		file_path = FILES_ROOT / relative_path_for_node(state, node_id)
		try:
			node.content = read_content_from_path(file_path)
		except Exception as exc:
			raise http_error_from_exception(exc)
	node.summary = llm_generate_description(node.content)
	save_state(state)
	return {"id": node_id, "description": node.summary}

@router.get("/generate/path")
def generate_path(id: UUID = Query(...)):
	node_id = str(id)
	state = load_state()
	node = get_node(state, node_id)
	if node.type != "file":
		raise HTTPException(status_code=400, detail="Node is not a file")

	tree = "\n".join(build_tree(_tree_nodes_for_building(state), root_id(state)))
	name = node.name
	description = node.summary
	print(description)

	new_path = llm_generate_path(tree, name, description)

	return {"id": node_id, "path": new_path}


# @router.post("/generate/folder-structure")
# def generate_structure(payload: ContentPayload | None = Body(None)):
# 	state = load_state()
# 	content = payload.content if payload else None
# 	if not content:
# 		content = aggregate_text(state, root_id(state))
# 	if not content:
# 		raise HTTPException(status_code=400, detail="No content available for structure generation")
# 	structure = llm_generate_structure(content)
# 	return {"structure": structure}


# @router.post("/generate/folder-name")
# def generate_folder_name(id: UUID = Query(...)):
# 	state = load_state()
# 	node_id = str(id)
# 	node = get_node(state, node_id)
# 	if node.type != "folder":
# 		raise HTTPException(status_code=400, detail="Node is not a folder")
# 	content = aggregate_text(state, node_id)
# 	if not content:
# 		raise HTTPException(status_code=400, detail="No content available for folder name generation")
# 	name = llm_generate_folder_name(content)
# 	return {"id": node_id, "name": name}


@router.get("/config")
def get_rules():
	config = load_config()
	return {
		"user_prompt": config.get("user_prompt"),
		"description_prompt": config.get("description_prompt"),
		"name_prompt": config.get("name_prompt"),
		"name_pattern": config.get("name_pattern"),
		"folder_pattern": config.get("folder_pattern"),
		"structure_pattern": config.get("structure_pattern"),
		"ollama_url": config.get("ollama_url"),
		"ollama_model": config.get("ollama_model"),
	}


@router.get("/rules/user")
def get_user_rule():
	config = load_config()
	return {"user_prompt": config.get("user_prompt")}


@router.patch("/rules/user")
def update_user_rule(payload: UserRulePayload = Body(...)):
	config = load_config()
	config["user_prompt"] = payload.user_prompt
	save_config(config)
	return {"user_prompt": config.get("user_prompt")}


@router.get("/rules/learned")
def get_learned_rules():
	config = load_config()
	return {
		"name_pattern": config.get("name_pattern"),
		"structure_pattern": config.get("structure_pattern"),
	}
