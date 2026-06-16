from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, Field


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
