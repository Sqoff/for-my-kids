# 📚 WeParent (부부 공동 육아 & 잔소리 방지 앱) 개발 문서 인덱스

본 프로젝트의 모든 기획, 기술 규격 및 의사결정 기록은 **Doc Consolidator 표준 3분할 체계(SSOT)**에 따라 관리됩니다.

---

## 🏛️ 1. 표준 기술 규격서 (Living Specs - SSOT)
현재 시스템의 구현 사양 및 동작 규칙을 정의한 단일 진실 공급원입니다.

- **[couple-rulebook.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/couple-rulebook.md)**: 📖 **공식 부부 평화 육아 룰북 (사용자 가이드 & 상벌 협약 규격)**
- **[core-system.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/core-system.md)**: 모드(이지/하드), 육아 집중 시간, **안드로이드 미디어 알림 & 미디어 컨트롤러(Media Controls)**, 앱 차단, 할 일 및 페널티/퀘스트 규칙
- **[flutter-native-plan.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/flutter-native-plan.md)**: 📱 **Flutter + Android Native MethodChannel & 서비스 연동 마스터 플랜**
- **[architecture.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/architecture.md)**: 전체 시스템 아키텍처, OS 권한 및 **MediaSession/MediaStyle 네이티브 파이프라인**, **웹 브라우저 기반 즉시 검증 환경(Tier 1 Web Prototype)**
- **[api-data.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/api-data.md)**: Cloud Firestore 데이터 모델, FCM 푸시 이벤트, 플랫폼 채널 인터페이스

---

## 🚀 2. 개발 단계 및 마일스톤 (Phases)
- **[prototype-gdd.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/phases/prototype-gdd.md)**: 1-Page MVP 게이미피케이션 프로토타입 사양서 (30초 코어 루프 & 검증 기준)
- **[roadmap.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/phases/roadmap.md)**: Phase 1 (MVP 부부 협력 & 웹 프로토타입 검증) ➔ Phase 2 (안드로이드 네이티브 집중 모드) ➔ Phase 3 (게이미피케이션)

---

## 📜 3. 의사결정 히스토리 (Decision Archive)
- **[decision-logs.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/history/decision-logs.md)**: 기술 스택 선정 이유, 이지/하드 모드 분리 배경, 방어 퀘스트 설계 의도, **웹 브라우저 즉시 프로토타이핑/테스트 전략(ADR 5)** 등 (Why)

---

## 💻 4. 인터랙티브 프로토타입 (Interactive Prototype)
- **[index.html](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/prototype/index.html)**: 부부 2인 동시 뷰 및 게이미피케이션 실시간 시뮬레이터
- **[server.py](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/prototype/server.py)**: 로컬 브라우저 자동 실행 및 모바일 동일 Wi-Fi 접속 서버 (`python prototype/server.py`)
