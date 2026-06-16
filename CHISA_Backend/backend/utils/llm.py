import requests
from pathlib import Path
import json



def parse_key_value(text: str) -> dict:
	result: dict[str, str] = {}
	for raw_line in text.splitlines():
		line = raw_line.strip()
		if not line or ":" not in line:
			continue
		key, value = line.split(":", 1)
		key = key.strip().lower()
		value = value.strip()
		if key and value:
			result[key] = value
	return result


def _get_settings() -> dict:
	config_path = Path(__file__).resolve().parents[2] / "data" / "config.json"
	with open(config_path, "r", encoding="utf-8") as f:
		config = json.load(f)

	return config


def _prompt_generator(behavior_prompt: str, rule: str, content: str) -> str:
	prompt = behavior_prompt.strip()
	parts = [prompt]
	if rule:
		parts.append("Rule:\n" + rule)
	parts.append("Content:\n" + content)
	return "\n\n".join(parts) + "\n"


def _call_llm(base_prompt: str, rule: str, content: str) -> str:
	config = _get_settings()
	model = config.get("ollama_model")
	url = config.get("ollama_url")

	prompt = _prompt_generator(behavior_prompt=base_prompt, rule=rule or "", content=content)

	payload = {
		"model": model,
		"prompt": prompt,
		"stream": False,
	}

	response = requests.post(url, json=payload)
	response.raise_for_status()
	data = response.json()
	text = (data.get("response") or data.get("text") or "")
	if isinstance(text, list):
		text = "\n".join(text)
	return str(text).strip()



def generate_name(content: str) -> str:
	command = """
	당신은 문서의 제목을 생성합니다.
	문서의 핵심 주제를 기반으로 짧고 명확한 제목을 작성하세요.

	Key: Value 형식으로만 출력하세요.
	반드시 아래 키를 사용하세요:
	title: 문서를 대표하는 제목
	"""
	config = _get_settings()
	rule = config.get("name_prompt") or ""
	
	resp = _call_llm(command, rule, content)
	parsed = parse_key_value(resp)
	return parsed.get("title", "")


def generate_description(content: str) -> str:
	command = """
	당신은 문서의 핵심 내용을 1~2문장으로 요약합니다.

	Key: Value 형식으로만 출력하세요.
	반드시 아래 키를 사용하세요:
	description: 간결한 요약
	"""
	config = _get_settings()
	rule = config.get("description_prompt") or ""
	
	resp = _call_llm(command, rule, content)
	parsed = parse_key_value(resp)
	return parsed.get("description", "")


def guess_file_pos(tree: str, file_name: str, description: str) -> str:
	command = f"""
	파일을 어느 폴더에 넣어야 하는지 판단하세요.
	반드시 기존 폴더 중 하나를 선택하세요
	
	Key: Value 형식으로만 출력하세요
	반드시 아리 키를 사용하세요:
	folder: 선택한 폴더의 경로
	confidence: 파일의 폴더 배치 확신도 (0.0~1.0)
	"""

	resp = _call_llm(command, None, f"현재 폴더 구조:\n{tree}\n\n새 파일 정보:\n-파일 이름:{file_name}\n-파일 요약:{description}")
	parsed = parse_key_value(resp)
	confid = parsed.get("confidence","")
	print(f"confid:{confid}")

	path = parsed.get("folder","").strip()
	if path and not path.startswith("root"): # 많이 위험해 보인다ㅏ
		path = "root/" + path
	return path



if __name__ == "__main__":
	from .file_reader import read_file
	from .tree_builder import build_tree
	project_root = Path(__file__).resolve().parents[2]
	content_path = project_root / "data" / "test_txt.txt"

	content = read_file(content_path)

	print(content)

	print("="*100)
	name = generate_name(content)
	print(name)

	print("="*100)
	description = generate_description(content)	
	print(description)

	print("="*100)
	path = "/Users/sungho/dev/CHISA_Backend/file_example.json"
	with open(path, "r") as f:
		data = json.load(f)
	result = build_tree(data["nodes"])
	pos = guess_file_pos(result, content)
	print(f"pos: {pos}")