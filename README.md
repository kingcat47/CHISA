# CHISA - 온디바이스 AI 파일 관리 앱

> **C**lassify, **H**andle, **I**ntelligently **S**tore, **A**rrange

Android용 AI 기반 파일/폴더 관리 앱입니다.
LLM이 파일 내용을 읽고 이름을 제안하거나, 적절한 폴더 위치를 추천해줍니다.
모든 AI 처리는 기기 내부에서 실행되며 외부 서버로 데이터가 전송되지 않습니다.

---

## 왜 CHISA인가?

기존의 AI 파일 정리 도구들은 대부분 **데스크탑 전용**이며, 파일 내용을 외부 서버로 전송해 처리합니다.
이 방식은 개인 문서, 사진, 업무 파일 등 민감한 데이터가 서버를 거쳐야 한다는 **개인정보 위험**을 동반합니다.

CHISA는 이 문제를 두 가지 방법으로 해결합니다.

| 문제 | CHISA의 접근 |
|------|-------------|
| 데스크탑 전용 | Android 네이티브 앱 |
| 서버 전송 → 개인정보 위험 | 온디바이스 LLM → 데이터 외부 유출 없음 |

---

## 주요 기능

- **그리드 파일 탐색기**: 파일과 폴더를 그리드로 시각화, 폴더 탐색 지원
- **파일/폴더 관리**: 생성, 이름변경, 이동, 삭제, 외부 파일 가져오기
- **AI 이름 생성**: 파일 내용을 분석해 적절한 이름 자동 제안
- **AI 요약**: 파일 핵심 내용을 1~2문장으로 요약
- **AI 위치 추천**: 현재 폴더 구조를 파악해 파일을 넣을 폴더를 추천
- **파일 뷰어**: PDF, 이미지, 오디오 내장 뷰어

---

## 온디바이스 LLM: Gemma 4 E2B

AI 기능은 기기에서 직접 실행되는 **Gemma 4 E2B (2B 파라미터)** 모델을 사용합니다.

### 이 모델을 선택한 이유

- **경량 모델**: 2B 파라미터로 모바일 기기 메모리 내에서 실행 가능
- **Google LiteRT 최적화**: `.litertlm` 포맷으로 Android 추론 엔진과 완벽 호환
- **GPU/CPU 자동 전환**: GPU 가속을 우선 시도하고, 불가능하면 CPU로 자동 폴백
- **Instruction-tuned**: 구조화된 출력(Key: Value) 형식을 잘 따름
- **한국어 지원**: 한국어 프롬프트 및 결과 생성 지원

모델은 앱 최초 실행 시 HuggingFace에서 기기로 다운로드되며, 이후에는 오프라인으로 동작합니다.

---

## 아키텍처

```
Repository → UseCase → ViewModel → UI (Jetpack Compose)
```

```
app/
├── model/          # GridItem (Folder / File sealed class)
├── repository/     # 파일시스템 I/O
├── usecase/        # 기능 단위 (Delete, Rename, Move, Import, GenerateName, ...)
├── viewmodel/      # UI 상태 관리 (MainViewModel)
├── components/     # Composable UI 컴포넌트
│   ├── viewer/     # PDF / Image / Audio 뷰어
│   └── Popup/      # 다이얼로그 모음
├── backend/
│   ├── model/      # ChisaConfig (설정)
│   └── service/    # LlmService, ModelDownloader, FileReaderService
└── pages/          # 화면 (ModelLoadingScreen, SettingsScreen)
```

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| UI | Jetpack Compose |
| 아키텍처 | Clean Architecture (Repository / UseCase / ViewModel) |
| LLM 런타임 | Google LiteRT LLM (litertlm) |
| AI 모델 | Gemma 4 E2B Instruct |
| PDF 처리 | PDFBox (Android 포트) |
| 언어 | Kotlin |

---

## 설정 (ChisaConfig)

`chisa_config.json` 파일로 LLM 동작을 커스터마이징할 수 있습니다.

| 키 | 기본값 | 설명 |
|----|--------|------|
| `name_prompt` | - | 이름 생성 시 추가 규칙 |
| `description_prompt` | - | 요약 생성 시 추가 규칙 |
| `folder_max_depth` | 5 | 폴더 트리 탐색 최대 깊이 |
| `max_pages` | 5 | PDF에서 읽을 최대 페이지 수 |
| `max_chars` | 4000 | LLM에 전달할 최대 글자 수 |
