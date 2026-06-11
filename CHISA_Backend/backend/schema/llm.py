from __future__ import annotations

from pydantic import BaseModel


class ContentPayload(BaseModel):
    content: str | None = None


class UserRulePayload(BaseModel):
    user_prompt: str | None = None
