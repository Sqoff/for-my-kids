# 👶 WeParent (가칭: 우리아이 타임 / For My Kids)

<div align="center">

![Flutter](https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Android](https://img.shields.io/badge/Android_Native-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**"서로 잔소리하지 마세요, 앱이 대신 챙겨드립니다."**  
부부 간의 감정 소모를 줄이고 유쾌한 게임화(Gamification)로 완성하는 **부부 1:1 전용 공동 육아 & 습관 관리 플랫폼**

[📖 상세 기획/개발 규격서 (Living Specs)](docs/INDEX.md) • [🚀 개발 로드맵](docs/phases/roadmap.md)

</div>

---

## 💡 기획 배경 및 문제 정의

| AS-IS (기존의 어려움) | TO-BE (WeParent의 해결책) |
|---|---|
| "핸드폰 그만 보고 애 좀 봐!" (스마트폰 과몰입 갈등) | **육아 시간 딴짓 앱 자동 차단 + 플로팅 상기 뱃지** |
| "설거지 언제 할 거야? 양치 시켰어?" (지치는 잔소리) | **앱이 알아서 마감 알림 발송 & 할 일 분담** |
| 사소한 집안일 미루기가 큰 감정 싸움으로 번짐 | **유쾌한 '페널티권'과 합의된 '안전 룰셋'으로 재미있게 해결** |
| 일방적인 감시와 통제로 인한 피로감 | **공동 퀘스트 완수 시 '페널티 방어 면제권' & '데이트 쿠폰' 보상** |

---

## ✨ 핵심 기능 (Key Features)

### 1. 📵 육아 집중 시간 & 스마트폰 딴짓 차단
- 지정된 육아 시간(예: 평일 18:00~21:00) 동안 SNS(인스타/틱톡), 유튜브, 모바일 게임, 쇼핑 앱 등 **사전 지정된 딴짓 앱을 물리적으로 차단**합니다.
- 전화, 긴급 문자, 카카오톡, 카메라, 육아 기록 앱 등 필수 앱은 정상 이용 가능합니다.

### 2. 👶 상시 노출 플로팅 상기 뱃지 (`SYSTEM_ALERT_WINDOW`)
- 허용된 앱(카카오톡, 카메라 등)을 사용할 때도 화면 모서리에 `👶 육아 집중 01:23:45` 캡슐형 플로팅 위젯이 상시 표시되어 무의식적인 딴짓 전환을 방지합니다.
- 자유로운 드래그 이동 및 탭을 통한 빠른 상태 확인을 지원합니다.

### 3. ✅ 오늘의 할 일 & 난이도 모드 (이지 / 하드)
- 부부의 육아 성향에 맞춰 모드를 선택할 수 있습니다:
  - **이지 모드 (Easy Mode - 기본값)**: 마감 시간 초과 시 상대방에게 확인 알림 발송 (돌발 상황 시 30분 유예 또는 봐주기 가능).
  - **하드 모드 (Hard Mode)**: 마감 시간 경과 시 시스템이 칼같이 페널티권 즉시 발급.

### 4. 😈 유쾌한 페널티권 & 싸움 방지 안전 룰셋
- 할 일 미완료 시 상대방에게 페널티권 1장이 지급됩니다.
- 감정 싸움을 방지하기 위해 **앱에서 검증된 '안전 추천 룰셋'** 및 **양측 합의된 커스텀 룰** 안에서만 페널티를 행사할 수 있습니다.
  - *예시: 오늘 설거지 1회 넘기기, 1시간 스마트폰 추가 차단, 다음 목욕 전담, 15분 어깨 안마*

### 5. 🛡️ 협력 퀘스트 & 방어 아이템 (게이미피케이션)
- 부부가 한 팀이 되어 주간 공동 목표(예: 7일 연속 육아 완주, 노-스마트폰 육아 20시간 달성)를 달성하면 보상 아이템을 획득합니다.
- **페널티 면제권**: 상대방의 페널티권 사용 시 "🛡️ 방어 성공!"으로 1회 무효화.
- **데이트/외식 쿠폰**: 주말 외식권 또는 자유시간 2시간 공식 보장.

---

## 🏗️ 시스템 아키텍처 & 기술 스택

```
┌──────────────────────────────────────────────────────────┐
│                   Flutter Client (Dart)                  │
│       - Reactive UI & Gamification Animations            │
│       - State Management & Local Storage                 │
└────────────────────────────┬─────────────────────────────┘
                             │ Platform MethodChannel
┌────────────────────────────▼─────────────────────────────┐
│               Android Native Layer (Kotlin)              │
│       - Foreground Service (ParentingFocusService)       │
│       - UsageStatsManager (Realtime App Monitor)         │
│       - WindowManager Overlay (Floating Badge & Blocking)│
└────────────────────────────┬─────────────────────────────┘
                             │ Realtime Sync & Push
┌────────────────────────────▼─────────────────────────────┐
│                 Cloud Backend (Firebase)                 │
│       - Firebase Auth (Social Login & Couple Pairing)    │
│       - Cloud Firestore (Tasks, Penalty Cards, Quests)   │
│       - Firebase Cloud Messaging (Instant Push Alert)    │
└──────────────────────────────────────────────────────────┘
```

| 영역 | 기술 스택 | 설명 |
|---|---|---|
| **Client UI** | **Flutter (Dart)** | 크로스플랫폼 대응 및 애니메이션/게이미피케이션 UI |
| **Native Control** | **Android Kotlin** | 플로팅 뱃지(`SYSTEM_ALERT_WINDOW`), 포그라운드 서비스, 앱 차단(`UsageStats`) |
| **Backend & DB** | **Firebase Firestore** | 2인 부부 실시간 데이터 동기화 |
| **Push Notification** | **Firebase Cloud Messaging (FCM)** | 마감 알림, 페널티 알림, 방어 성공 푸시 |
| **Authentication** | **Firebase Auth** | 소셜 로그인 및 6자리 초대 코드 기반 1:1 결속 |

---

## 📚 표준 개발 문서 체계 (SSOT)

본 프로젝트는 `doc-consolidator` 표준 3분할 문서 체계로 관리됩니다.

- 🏛️ **[Living Specs (표준 기술 규격서)](docs/INDEX.md)**
  - [`core-system.md`](docs/specs/core-system.md) : 비즈니스 로직, 모드, 페널티/퀘스트 규칙
  - [`architecture.md`](docs/specs/architecture.md) : 시스템 아키텍처 및 안드로이드 네이티브 제어 명세
  - [`api-data.md`](docs/specs/api-data.md) : Firestore 데이터 스키마 및 FCM 이벤트 규격
- 🚀 **[Phases (개발 로드맵)](docs/phases/roadmap.md)**
  - Phase 1: MVP Core (부부 페어링, 할 일, 모드)
  - Phase 2: Android 네이티브 집중 모드 (플로팅 뱃지, 앱 차단)
  - Phase 3: 게이미피케이션 & 방어 퀘스트
- 📜 **[History (의사결정 아카이브)](docs/history/decision-logs.md)**
  - 기술 스택 및 주요 기획 의사결정 배경 (Why)

---

## 📄 License
This project is licensed under the MIT License.