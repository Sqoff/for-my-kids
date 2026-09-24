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

### 1. 🎬 오프닝 프롤로그 스토리 튜토리얼 (웹툰/애니메이션 연출)
- 부부의 피로와 사소한 갈등("니가 해!" vs "니가 해!") ➔ 아기의 눈물("으앙~") ➔ 당황한 부부가 땀을 삐질 흘리며 달래기 ➔ 수호 요정 '뽀마(Bboma)'의 탄생으로 이어지는 감성 몰입형 프롤로그 애니메이션.
- 하단 **[⏩ 스킵하기]** 버튼으로 언제든 즉시 시작 가능.

### 2. 📵 육아 집중 시간 & 스마트폰 딴짓 차단 (상태바 👶 아이콘 상주)
- 앱을 계속 켜둘 필요 없이, **스마트폰 상단 시계줄(상태바)에 `👶` 아이콘만 조용히 상주**하여 화면 방해 없이 백그라운드에서 동작합니다.
- 지정된 집중 시간 동안 유튜브, SNS, 게임, 쇼핑 등 **사전 지정된 딴짓 앱을 물리적으로 차단**합니다. (전화, 긴급 112/119, 카톡, 카메라, 베이비타임 등은 상시/선택 허용).

### 3. ⏰ 할 일 수행 시간 강제 팝업 (Mandatory Task Pop-up)
- 목욕, 설거지, 소독 등 약속된 할 일 시간이 되면 화면에 확인 팝업이 강제로 표출되어 **[✅ 완료했어요]** 또는 **[⏳ 15분 뒤 알림]**, **[📋 오늘 할 일 리스트 확인]**을 직접 터치해야만 닫힙니다.

### 4. 🐣 아기 수호 요정 '뽀마' 육성 & 부부 협력 RPG
- 부부가 서로 가사를 돕고 집중 시간을 채울 때마다 요정 뽀마가 성장하며 레벨업(EXP)합니다.
- **👼 천사 배우자 퀘스트**: 상대방의 가사를 N회 대신 도와주면 **`🛡️ 페널티 면제권 1장`**을 지급 (3회 / 5회 / 10회 등 부부가 시작 시 자유롭게 합의 결정).

### 5. 😈 유쾌한 페널티권 & 🛡️ 방어 아이템 배틀
- 할 일 미완료 시 상대방에게 페널티권이 지급되며, 사전 합의된 안전 룰셋(설거지 넘기기, 15분 어깨 마사지, 1시간 폰 추가 차단 등)으로 유쾌하게 해결합니다.
- 천사 퀘스트로 모은 **🛡️ 방어권**으로 상대방의 페널티 공격을 무효화할 수 있습니다.


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
  - [`architecture.md`](docs/specs/architecture.md) : 시스템 아키텍처, 네이티브 제어, **웹 브라우저 즉시 검증 표준**
  - [`api-data.md`](docs/specs/api-data.md) : Firestore 데이터 스키마 및 FCM 이벤트 규격
- 🚀 **[Phases (개발 로드맵 & 프로토타입 GDD)](docs/phases/roadmap.md)**
  - [`prototype-gdd.md`](docs/phases/prototype-gdd.md) : 1-Page MVP 게이미피케이션 프로토타입 사양서
  - Phase 1: MVP Core (웹 프로토타입 즉시 검증 & 부부 협력 시스템)
  - Phase 2: Android 네이티브 집중 모드 (플로팅 뱃지, 앱 차단)
  - Phase 3: 게이미피케이션 & 방어 퀘스트
- 📜 **[History (의사결정 아카이브)](docs/history/decision-logs.md)**
  - 기술 스택 및 주요 기획 의사결정 배경 (Why), **웹 브라우저 즉시 검증 채택 이유(ADR 5)**

---

## 🎮 빠른 프로토타입 실행 (Web Prototype Instant Test)

APK 빌드/설치 없이 웹 브라우저에서 2인(엄마/아빠) 동시 화면과 코어 루프를 즉시 테스트할 수 있습니다:

```bash
# 로컬 웹 서버 실행 (브라우저 자동 실행 & 모바일 동일 Wi-Fi 접속 지원)
python prototype/server.py
```
*(또는 [`prototype/index.html`](prototype/index.html)을 크롬/엣지 브라우저로 직접 더블 클릭하여 실행)*

---

## 📄 License
This project is licensed under the MIT License.