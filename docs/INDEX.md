# 📚 WeParent (부부 공동 육아 & 잔소리 방지 앱) 개발 문서 인덱스

본 프로젝트의 모든 기획, 기술 규격 및 의사결정 기록은 **Doc Consolidator 표준 3분할 체계(SSOT)**에 따라 관리됩니다.

---

## 🏛️ 1. 표준 기술 규격서 (Living Specs - SSOT)
현재 시스템의 구현 사양 및 동작 규칙을 정의한 단일 진실 공급원입니다.

- **[ui-layout-specification.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/ui-layout-specification.md)**: 🎨 **공식 UI/UX 레이아웃 설계 규격서 (토스&애플 화이트 테마 기본화, 좌측 밀어서 완료 제스처, 12개 핵심 화면 및 컴포넌트 명세)**
- **[couple-rulebook.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/couple-rulebook.md)**: 📖 **공식 부부 평화 육아 룰북 (사용자 가이드 & 상벌 협약 규격)**
- **[core-system.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/core-system.md)**: 모드(이지/하드), 육아 집중 시간(프리셋/시간대지정), **안드로이드 공식 미디어 알림 & 미디어 컨트롤러(Media Controls)**, 앱 차단, 할 일(밀어서 완료/대신 돕기), 100% 자동집계 퀘스트, 토스 치킨 저금통, 쿠팡 2% 캐시백, 10섹터 룰렛, 스마트 넛지
- **[flutter-native-plan.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/flutter-native-plan.md)**: 📱 **Flutter + Android Native MethodChannel & 서비스 연동 마스터 플랜**
- **[architecture.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/architecture.md)**: 전체 시스템 아키텍처, OS 권한 및 **MediaSession/MediaStyle 네이티브 포그라운드 파이프라인**, Capacitor 브릿지 매핑
- **[api-data.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/api-data.md)**: Cloud Firestore 데이터 모델, FCM 푸시 이벤트, 플랫폼 채널 인터페이스

---

## 🚀 2. 개발 단계 및 마일스톤 (Phases)
- **[roadmap.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/phases/roadmap.md)**: Phase 1 (MVP 부부 협력) ✅ ➔ Phase 2 (안드로이드 네이티브 집중 모드) ✅ ➔ Phase 3 (게이미피케이션 & 실물 리워드) ✅ ➔ Phase 4 (Flutter 프로덕션 전환 & Firebase 백엔드)

---

## 📜 3. 의사결정 히스토리 (Decision Archive)
- **[decision-logs.md](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/history/decision-logs.md)**: 기술 스택 선정 이유, 이지/하드 모드 분리 배경, 방어 퀘스트 설계 의도, **MediaSession 미디어 컨트롤러 도입(ADR 21)**, **토스&키즈노트 UX 개편(ADR 22)**, **듀얼 테마 & 스마트 넛지(ADR 23)**, **밀어서 완료 제스처 & 튜토리얼 소멸 규칙(ADR 24)**, **화이트 테마 공식 기본화 & 전면 가독성 개편(ADR 25)** 등 (Why)

---

