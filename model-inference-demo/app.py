from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel, Field

from inference_demo.parser import IntentSlotParser


app = FastAPI(title="Hospital NLU Inference Demo", version="0.1.0")
parser = IntentSlotParser()


class InferRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=500)
    sessionId: Optional[str] = None


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/infer")
def infer(request: InferRequest):
    return parser.parse(request.text)

