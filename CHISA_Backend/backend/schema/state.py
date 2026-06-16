from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class Node(BaseModel):
    name: str
    type: Literal["folder", "file"]
    parent: str | None = None
    children: list[str] = Field(default_factory=list)
    summary: str | None = None
    content: str | None = None


class FileState(BaseModel):
    nodes: dict[str, Node] = Field(default_factory=dict)
