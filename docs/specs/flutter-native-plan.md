# 📱 [네이티브 구현 설계서] Flutter + Android Native 연동 마스터 플랜

> **문서 상태**: Living Spec (네이티브 구현 가이드)  
> **대상 플랫폼**: Android 10+ (API Level 29~34) / Flutter 3.x  
> **최종 갱신일**: 2026-09-24

---

## 🏗️ 1. 시스템 아키텍처 개요

UI/UX 및 게이미피케이션 로직을 Android 네이티브 시스템 제어 기능과 완벽히 결합합니다.

```mermaid
graph TD
    subgraph Flutter UI Layer
        UI[Flutter Screen & Widgets]
        State[State Management - Riverpod]
        UI <--> State
    end

    subgraph Firebase Cloud Layer
        Firestore[(Cloud Firestore)]
        FCM[Firebase Cloud Messaging]
        State <--> Firestore
        FCM --> UI
    end

    subgraph Platform Channel
        MC[MethodChannel: com.weparent.app/native_focus]
        EC[EventChannel: com.weparent.app/app_usage]
        State <--> MC
        EC --> State
    end

    subgraph Android Native Services
        UsageService[UsageStatsManager / 앱 모니터링 루프]
        MediaSessionService[MediaSessionCompat + NotificationCompat.MediaStyle]
        ForegroundService[FocusForegroundService (포그라운드 서비스)]
        LockEngine[전체화면 차단 안내 뷰 & 홈 이동]
        MC --> ForegroundService
        ForegroundService --> MediaSessionService
        ForegroundService --> LockEngine
        UsageService --> EC
    end
```

---

## 🔌 2. Flutter Platform Channel 인터페이스 규격

### 2.1 MethodChannel (`com.weparent.app/native_focus`)

| 메서드명 (`method`) | 파라미터 (`arguments`) | 반환값 | 설명 |
|---|---|---|---|
| `startFocusMode` | `{ "secondsLeft": 1500, "totalSeconds": 1500, "userName": "지은", "userRole": "엄마", "blockedPackages": ["com.google.android.youtube", ...] }` | `bool` | 네이티브 포그라운드 서비스 가동 및 MediaSession 미디어 노티피케이션 활성화 |
| `stopFocusMode` | `{}` | `bool` | 집중 모드 해제 및 미디어 세션 종료 |
| `checkPermissions` | `{}` | `Map<String, bool>` | 필수 권한(`PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`) 허용 여부 점검 |
| `requestPermission` | `{ "permissionType": "USAGE_STATS" }` | `void` | 사용 정보 접근 설정 화면으로 Intent 이동 |
| `updateBlockedList` | `{ "blockedPackages": ["com.google.android.youtube", ...] }` | `bool` | 차단 대상 패키지 목록 실시간 갱신 |

### 2.2 EventChannel (`com.weparent.app/app_usage`)

* **역할**: 안드로이드 네이티브 서비스에서 감지된 딴짓 앱 실행 이벤트를 Flutter 레이어로 실시간 스트리밍.
* **이벤트 페이로드**:
  ```json
  {
    "eventType": "BLOCKED_APP_DETECTED",
    "packageName": "com.google.android.youtube",
    "appName": "YouTube",
    "timestamp": 1727182800000
  }
  ```

---

## 🛡️ 3. Android Native 핵심 컴포넌트 설계

### 3.1 딴짓 앱 감지 엔진 (`UsageStatsManager`)
* **방식**: `UsageStatsManager` (폴링 주기 500ms).
* **동작**: 현재 Foreground에 위치한 앱의 Package Name이 `blockedPackages`에 포함되어 있을 경우:
  1. 홈 화면(`Intent.ACTION_MAIN, Intent.CATEGORY_HOME`)으로 강제 이동.
  2. 전체화면 차단 안내 뷰 표출 및 *"지금은 아이에게 집중할 시간이에요! 👶"* 알림.

### 3.2 안드로이드 공식 시스템 미디어 세션 파이프라인 (ADR 21)
* **권한**: `android.permission.POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`
* **표준 API**: `androidx.media:media`의 `MediaSessionCompat` & `NotificationCompat.MediaStyle`
* **특징**:
  * 강제 오버레이 윈도우(`TYPE_APPLICATION_OVERLAY`)를 전면 배제하여 시스템 상태바 아이콘 충돌 원천 방지.
  * 안드로이드 SystemUI의 표준 슬롯에 맞춰 상태바에 `ic_stat_bboma` (`👶`) 아이콘 단독 상주.
  * 상단 알림 패널 및 잠금 화면(Lock Screen)에 3버튼 인터랙티브 미디어 컨트롤러 상시 제공:
    * `⏸` 일시정지 / `▶` 계속하기 (`PlaybackStateCompat`)
    * `⏩` +5분 즉시 연장
    * `✕` 육아 집중 세션 종료

---

## 📁 4. Flutter 프로젝트 구조 (Clean Architecture)

```
lib/
├── main.dart                          # 앱 엔트리포인트 & Firebase 초기화
├── core/
│   ├── constants/                     # 앱 테마, 색상, 애니메이션 토큰
│   ├── network/                       # Firebase & FCM 클라이언트
│   └── native/                        # MethodChannel 래퍼 클래스
├── data/
│   ├── models/                        # TaskModel, PenaltyModel, UserModel
│   ├── datasources/                   # Firestore Remote DataSource
│   └── repositories/                  # TaskRepository, PenaltyRepository
├── domain/
│   ├── entities/                      # 비즈니스 도메인 엔티티
│   └── usecases/                      # CompleteTask, TriggerPenalty, DefendPenalty
└── presentation/
    ├── providers/                     # Riverpod State Notifiers
    ├── screens/
    │   ├── splash_screen.dart         # 온보딩 & 룰북 시작 화면
    │   ├── couple_dashboard_screen.dart # 메인 2인 분할 대시보드
    │   ├── task_manage_screen.dart    # 할 일 CRUD & 6대 프리셋 모달
    │   ├── penalty_inventory_screen.dart # 패널티 CRUD & AI 맞춤 추천
    │   └── notification_center_screen.dart # 🔔 알림 센터 & 히스토리
    └── widgets/
        ├── fairy_bboma_widget.dart    # 뽀마 캐릭터 애니메이션
        └── focus_timer_dial.dart      # 집중 타이머 휠 다이얼
```

---

## 🚀 5. 단계별 개발 마일스톤 (Phase Roadmap)

1. **Step 1: Flutter 프로젝트 생성 & 디자인 시스템 구축**
   * 프로젝트 베이스라인 세팅, 폰트(Pretendard), 테마 컬러(`indigo`, `amber`, `rose`) 및 공통 위젯 이식.
2. **Step 2: Firestore & FCM 실시간 연동**
   * 부부 방 페어링, 실시간 할 일/패널티 CRUD 및 푸시 알림 리액티브 스트림 구축.
3. **Step 3: Android Native MethodChannel & 서비스 구현**
   * `UsageStatsManager` 앱 감지 엔진 완성 및 백그라운드 포그라운드 서비스 탑재.
   * `MediaSessionCompat` + `NotificationCompat.MediaStyle` 기반 3버튼 미디어 컨트롤러 탑재 (ADR 21).
4. **Step 4: 온보딩 영상 & 공식 룰북 탑재**
   * 사용자가 제작한 프롤로그 애니메이션 동영상 플레이어 연동 및 [부부 평화 육아 룰북](file:///E:/%EA%B0%9C%EB%B0%9C/%EB%BD%80%EB%A7%88%ED%82%A4%EC%A6%88/docs/specs/couple-rulebook.md) 메뉴 통합.
