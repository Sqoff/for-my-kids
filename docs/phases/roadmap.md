# [Phase Roadmap] 개발 단계 및 마일스톤 계획

---

## Phase 1: MVP Core & 부부 협력 시스템
- [ ] Flutter 프로젝트 초기 세팅 & 아키텍처 구조화 (Riverpod + Clean Architecture)
- [ ] Firebase Auth (소셜 로그인) & 6자리 초대 코드를 통한 1:1 부부 결속
- [ ] 난이도 모드(이지/하드) 설정 화면 및 온보딩
- [ ] 오늘의 할 일 등록, 마감 시간 지정, 완료 체크, Firebase 실시간 동기화
- [ ] 기본 페널티권 발급 및 사용 UI

## Phase 2: Android 네이티브 육아 집중 & 플로팅 오버레이
- [ ] Kotlin Platform Channel (`native_focus`) 구현
- [ ] 안드로이드 필수 권한 핸들링 (`SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`)
- [ ] `ParentingFocusService` 포그라운드 서비스 구현
- [ ] 상시 노출 드래그 가능 `FloatingBadgeView` (실시간 타이머 갱신)
- [ ] 엔터테인먼트 딴짓 앱 감지 시 전체화면 차단 오버레이 표출 및 홈 화면 전환

## Phase 3: 게이미피케이션 & 방어 퀘스트 고도화
- [ ] 싸움 방지 추천 안전 룰셋 프리셋 및 부부 상호 합의 커스텀 룰
- [ ] 7일 연속 스트릭 및 주간 집중 시간 누적 계산기
- [ ] 퀘스트 완료 시 페널티 면제권 / 데이트 쿠폰 보상 인벤토리
- [ ] 페널티 사용 시 면제권 방어 인터랙션 ("🛡️ 방어 성공!")
- [ ] FCM 푸시 알림 트리거 및 백그라운드 핸들러
