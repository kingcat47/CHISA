from pathlib import Path
from docx import Document
from pypdf import PdfReader


def _read_pdf(file_path: str | Path, max_pages: int = 5, max_chars: int = 4000) -> str:
	path = Path(file_path)
	if not path.exists():
		raise FileNotFoundError(f"PDF not found: {path}")

	reader = PdfReader(str(path))
	page_count = min(max_pages, len(reader.pages))
	chunks = []
	current_len = 0
	for page_index in range(page_count):
		text = reader.pages[page_index].extract_text() or ""
		if not text:
			continue
		remaining = max_chars - current_len
		if remaining <= 0:
			break
		chunk = text[:remaining]
		chunks.append(chunk)
		current_len += len(chunk)
	return "\n\n".join(chunks).strip()


def _read_docx(file_path: str | Path, max_chars: int = 4000) -> str:
	path = Path(file_path)
	if not path.exists():
		raise FileNotFoundError(f"DOCX not found: {path}")

	doc = Document(str(path))
	parts: list[str] = []
	current_len = 0
	for para in doc.paragraphs:
		text = (para.text or "").strip()
		if not text:
			continue
		remaining = max_chars - current_len
		if remaining <= 0:
			break
		chunk = text[:remaining]
		parts.append(chunk)
		current_len += len(chunk)
	return "\n\n".join(parts).strip()


def _read_txt(file_path: str | Path, max_chars: int = 4000) -> str:
	path = Path(file_path)
	if not path.exists():
		raise FileNotFoundError(f"TXT not found: {path}")
	with path.open("r", encoding="utf-8", errors="ignore") as handle:
		return handle.read(max_chars).strip()


def read_file(file_path: str | Path, *, max_pages: int = 5, max_chars: int = 4000) -> str:
	path = Path(file_path)
	if not path.exists():
		raise FileNotFoundError(f"File not found: {path}")

	suffix = path.suffix.lower()
	if suffix == ".pdf":
		return _read_pdf(path, max_pages=max_pages, max_chars=max_chars)
	if suffix == ".docx":
		return _read_docx(path, max_chars=max_chars)
	if suffix == ".txt":
		return _read_txt(path, max_chars=max_chars)
	raise ValueError(f"Unsupported file type: {suffix}")


if __name__ == "__main__":
	print("PDF Read Test")
	print(read_file("../../data/test_pdf.pdf"))
	print(f'\n\n\n\n\n {"="*100}')
	print("txt Read Test")
	print(read_file("../../data/test_txt.txt"))
