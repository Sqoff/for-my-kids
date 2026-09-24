# 📚 WeParent (부부 공동 육아 & 잔소리 방지 앱) 개발 문서 인덱스

본 프로젝트의 모든 기획, 기술 규격 및 의사결정 기록은 **Doc Consolidator 표준 3분할 체계(SSOT)**에 따라 관리됩니다.

---

## 🏛️ 1. 표준 기술 규격서 (Living Specs - SSOT)
현재 시스템의 구현 사양 및 동작 규칙을 정의한 단일 진실 공급원입니다.

- **[core-system.md](file:///E:/%EA%B0%9C%EB%B0%9C/-/docs/specs/core-system.md)**: 모드(이지/하드), 육아 집중 시간, 딴짓 앱 차단 및 플로팅 뱃지, 할 일 및 페널티/퀘스트 규칙
- **[architecture.md](file:///E:/%EA%B0%9C%EB%B0%9C/-/docs/specs/architecture.md)**: Flutter + Android Native MethodChannel 아키텍처, OS 권한 및 포그라운드 서비스/오버레이 메커니즘
- **[api-data.md](file:///E:/%EA%B0%9C%EB%B0%9C/-/docs/specs/api-data.md)**: Cloud Firestore 데이터 모델, FCM 푸시 이벤트, 플랫폼 채널 인터페이스

---

## 🚀 2. 개발 단계 및 마일스톤 (Phases)
- **[roadmap.md](file:///E:/%EA%B0%9C%EB%B0%9C/-/docs/phases/roadmap.md)**: Phase 1 (MVP 부부 협력) ➔ Phase 2 (안드로이드 네이티브 집중 모드) ➔ Phase 3 (게이미피케이션)

---

## 📜 3. 의사결정 히스토리 (Decision Archive)
- **[decision-logs.md](file:///E:/%EA%B0%9C%EB%B0%9C/-/docs/history/decision-logs.md)**: 기술 스택 선정 이유, 이지/하드 모드 분리 배경, 방어 퀘스트 설계 의도 등 (Why)
