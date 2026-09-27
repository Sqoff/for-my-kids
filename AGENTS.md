# 🤖 AGENTS.md — WeParent (뽀마키즈) AI 에이전트 가이드

이 파일은 이 레포지토리에서 작업하는 **모든 AI 에이전트 세션**이 반드시 숙지해야 할 컨텍스트 및 문서 참조 가이드입니다.

---

## 📌 프로젝트 개요

**WeParent (뽀마키즈)** — 부부 공동 육아 & 잔소리 방지 앱  
Flutter(클라이언트) + Kotlin Android Native(포그라운드 서비스) + Firebase(Auth/Firestore/FCM) 스택으로 구성된 **안드로이드 전용 부부 협력 육아 게이미피케이션 앱**입니다.

- **테스트 방침**: 모든 기능 검증은 USB 연결 실제 안드로이드 폰에서 `flutter run`으로 직접 실행. `Ctrl+S` 핫리로드로 즉각 반영.
- **문서 관리 체계**: [Doc Consolidator 표준 3분할 체계(SSOT)](docs/INDEX.md) — 스펙(Living Specs) / 단계(Phases) / 의사결정(Decision Archive)

---

## 📚 문서 참조 가이드 (docs/)

에이전트는 작업 전 관련 문서를 반드시 먼저 읽어야 합니다.

---

### 🏛️ 1. 표준 기술 규격서 (Living Specs — SSOT)

현재 구현 사양의 **단일 진실 공급원**입니다. 코드 작성 전 항상 먼저 확인하세요.

| 문서 | 용도 | 참조 시점 |
|---|---|---|
| [architecture.md](docs/specs/architecture.md) | 전체 시스템 아키텍처, Flutter↔Kotlin Platform Channel 인터페이스, MediaSession/MediaStyle 네이티브 파이프라인, Android 권한 매니페스트, 테스트 환경 규격 | 아키텍처 설계·수정 / Native 연동 작업 / 권한 관련 작업 |
| [core-system.md](docs/specs/core-system.md) | 모드(이지/하드), 육아 집중 타이머, 앱 차단, 할 일 CRUD/캘린더/루틴, 페널티권 라이프사이클, 퀘스트 자동 집계 엔진, 게이미피케이션 전체 로직, 알림 센터, 기프티콘 저금통 | 비즈니스 로직 / 상태 관리 / 게이미피케이션 기능 |
| [ui-layout-specification.md](docs/specs/ui-layout-specification.md) | 12개 핵심 화면 레이아웃 명세, 컴포넌트 디자인 규격, 색상·스타일 가이드 | UI 컴포넌트 구현 / 화면 레이아웃 작업 |
| [couple-rulebook.md](docs/specs/couple-rulebook.md) | 공식 부부 평화 육아 룰북 (사용자 가이드, 상벌 협약 규격, 용어집, 분쟁 조정 조항) | 페널티·방어권·퀘스트 규칙 해석 / 사용자 가이드 관련 작업 |
| [flutter-native-plan.md](docs/specs/flutter-native-plan.md) | Flutter + Android Native MethodChannel & 포그라운드 서비스 연동 마스터 플랜 (`native_focus` 채널 인터페이스 전체 명세) | Android Native 서비스 구현 / MethodChannel 연동 작업 |
| [api-data.md](docs/specs/api-data.md) | Cloud Firestore 데이터 모델 스키마, FCM 푸시 이벤트 명세, 플랫폼 채널 인터페이스 | Firebase 데이터 구조 설계·수정 / FCM 연동 |

---

### 🚀 2. 개발 단계 및 마일스톤 (Phases)

| 문서 | 용도 | 참조 시점 |
|---|---|---|
| [roadmap.md](docs/phases/roadmap.md) | Phase 1 (MVP 부부 협력) → Phase 2 (Android 네이티브 집중 모드) → Phase 3 (게이미피케이션) 개발 단계 체크리스트 | 어떤 기능을 구현해야 하는지 우선순위 파악 / 현재 진행 단계 확인 |

**현재 Phase 진행 상태 요약 (2026-09-27):**
- **Phase 1~3** — Capacitor 프로토타입으로 부부 협력·네이티브 집중 알림·게이미피케이션 구현 완료 ✅ (단, 부부 연동은 한 기기 시뮬레이션)
- **Phase 3.5 (디자인 폴리싱)** — 진행 중. 최신 배포 v0.0.11 (GitHub Releases APK)
- **🔒 최종 연동 단계** — 디자인 완료 후 일괄 진행: Firebase 로그인 + 초대 코드 부부 연결 + Firestore 동기화 + FCM 푸시, 실제 앱 차단(UsageStats + 접근성 홈 이동) (ADR 32)
- **Phase 4** — Flutter 프로덕션 전환 (Next)

---

### 📜 3. 의사결정 히스토리 (Decision Archive)

| 문서 | 용도 | 참조 시점 |
|---|---|---|
| [decision-logs.md](docs/history/decision-logs.md) | 기술 스택 선정 이유, 이지/하드 모드 분리 배경, MediaSession 전환 결정(ADR 21), 퀘스트 수동 조작 제거 이유 등 모든 설계 결정의 **Why** | 기존 구현에 의문이 생겼을 때 / 설계 변경 전 배경 파악 / 새 ADR 추가 시 |

---

## ⚠️ 에이전트 필수 준수 규칙

1. **SSOT 우선**: 코드와 문서가 충돌할 경우 `docs/specs/` 문서를 진실로 간주하고 코드를 수정하세요.
2. **오버레이 사용 금지**: `SYSTEM_ALERT_WINDOW` / `TYPE_APPLICATION_OVERLAY` 권한 및 관련 구현은 **ADR 21에 의거 폐기**됨. 절대 재도입하지 마세요. 대신 `MediaSessionCompat` + `NotificationCompat.MediaStyle`을 사용하세요.
3. **수동 퀘스트 조작 금지**: 퀘스트 진행도는 앱 내 이벤트 트리거 기반 100% 자동 집계 방식만 허용합니다. (ADR 16)
4. **테스트 방침 준수**: 에뮬레이터를 기본 환경으로 사용하지 마세요. USB 연결 실기기가 기본입니다.
5. **ADR 추가**: 새로운 중요 설계 결정을 내릴 때는 [`decision-logs.md`](docs/history/decision-logs.md)에 기록을 추가하세요.
6. **실기능 연동 보류 (ADR 32)**: 디자인 작업 중에는 Firebase 연동·실제 앱 차단을 끼워 넣지 마세요. 두 폰 간 전달이 필요한 기능은 "현재 한 기기 시뮬레이션"임을 명시하세요.
7. **UI 원칙**: 선택 상태는 배경 변경 없이 테두리+글자만 강조, 어두운 반투명 색 배경 금지, 한글 `keep-all` 줄바꿈 (ADR 27, 30).

---

## 🗂️ 주요 문서 빠른 색인

```
docs/
├── INDEX.md                        # 전체 문서 인덱스
├── specs/
│   ├── architecture.md             # 시스템 아키텍처 & 네이티브 제어 규격
│   ├── core-system.md              # 핵심 비즈니스 & 게이미피케이션 시스템 규격
│   ├── ui-layout-specification.md  # UI/UX 레이아웃 설계 규격
│   ├── couple-rulebook.md          # 공식 부부 육아 룰북
│   ├── flutter-native-plan.md      # Flutter + Android Native 연동 플랜
│   └── api-data.md                 # Firestore 데이터 모델 & FCM 이벤트 명세
├── phases/
│   └── roadmap.md                  # 개발 단계 & 마일스톤 체크리스트
└── history/
    └── decision-logs.md            # 아키텍처 & 기획 의사결정 기록 (Why)
```
