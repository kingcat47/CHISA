from __future__ import annotations

import uvicorn
from fastapi import FastAPI

from .router import file_router, llm_router
from .router.common import initialize_state

app = FastAPI()
app.include_router(file_router)
app.include_router(llm_router)


@app.on_event("startup")
def startup() -> None:
	initialize_state()


if __name__ == "__main__":
	uvicorn.run(app, host="0.0.0.0", port=8080)
