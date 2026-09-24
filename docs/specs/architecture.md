# [Living Spec] 시스템 아키텍처 & 네이티브 제어 규격서

> **문서 상태**: Active (SSOT)  
> **최종 갱신일**: 2026-09-24  
> **대상 시스템**: 클라이언트 아키텍처, Android OS 권한/서비스 제어, Firebase 백엔드 연동

---

## 1. 시스템 전체 아키텍처

```mermaid
flowchart TB
    subgraph FlutterApp["Flutter Application (UI & Client Logic)"]
        UI_Layer["UI Components (Riverpod / Bloc State)"]
        Repo_Layer["Repository & Service Layer"]
        NativeBridge["Platform Channel (MethodChannel)"]
    end

    subgraph AndroidNative["Android Native Layer (Kotlin)"]
        ForegroundService["ParentingFocusService (Foreground Service)"]
        UsageTracker["UsageStats Monitor (UsageStatsManager)"]
        OverlayManager["Overlay View Manager (WindowManager)"]
        FloatingBadgeView["Floating Badge View (SYSTEM_ALERT_WINDOW)"]
        BlockingView["Full Screen Blocking View"]
    end

    subgraph CloudServices["Firebase Backend (BaaS)"]
        FirebaseAuth["Firebase Auth (Google / Kakao OAuth)"]
        FirestoreDB["Cloud Firestore (Realtime Sync DB)"]
        CloudMessaging["Firebase Cloud Messaging (FCM)"]
    end

    UI_Layer --> Repo_Layer
    Repo_Layer <--> FirestoreDB
    Repo_Layer <--> FirebaseAuth
    Repo_Layer --> NativeBridge
    CloudMessaging -. Push Notification .-> FlutterApp

    NativeBridge <--> ForegroundService
    ForegroundService --> UsageTracker
    ForegroundService --> OverlayManager
    OverlayManager --> FloatingBadgeView
    OverlayManager --> BlockingView
```

---

## 2. 안드로이드 네이티브 제어 상세 규격

### 2.1 필수 권한 및 매니페스트 설정
```xml
<!-- AndroidManifest.xml 필수 권한 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

### 2.2 포그라운드 서비스 및 앱 감지 파이프라인
1. **서비스 시작**: 육아 집중 시간 시작 시 `ParentingFocusService.startService()` 호출.
   - 알림 표시줄(Notification Bar)에 "👶 육아 집중 시간 진행 중" 상주 알림 노출.
2. **앱 감지 루프 (`UsageStatsManager`)**:
   - `UsageStatsManager.queryEvents()`를 활용하여 최상단 액티비티 전환 이벤트(`UsageEvents.Event.ACTIVITY_RESUMED`) 감지 (500ms 주기).
3. **차단 및 오버레이 처리**:
   - 실행된 패키지가 차단 목록(`blockedPackages`)에 속한 경우:
     - `WindowManager.addView(blockingView, layoutParams)`로 화면 전체를 덮는 차단 뷰 표출.
     - `Intent.ACTION_MAIN + CATEGORY_HOME`을 발행하여 홈 화면으로 전환 유도.
   - 실행된 패키지가 허용 목록(`allowedPackages`)인 경우:
     - 차단 뷰를 제거하고, 화면 한구석에 `FloatingBadgeView` 유지.

### 2.3 플로팅 뱃지 오버레이 (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`)
- **터치 이벤트**: `OnTouchListener`로 드래그 시 `layoutParams.x`, `layoutParams.y`를 실시간 갱신하여 화면 가장자리에 자연스럽게 부착(Magnetic Snap).
- **타이머 브로드캐스트**: 포그라운드 서비스의 1초 주기 틱(Tick)을 플로팅 뷰의 `TextView`에 바인딩.

---

## 3. Flutter - Native Platform Channel 인터페이스 규격

### MethodChannel: `com.weparent.app/native_focus`

| 메서드명 (`method`) | 매개변수 (`arguments`) | 반환값 (`result`) | 설명 |
|---|---|---|---|
| `checkPermissions` | - | `Map<String, Boolean>` | 오버레이 및 사용량 접근 권한 허용 여부 반환 |
| `requestOverlayPermission` | - | `Boolean` | '다른 앱 위에 표시' 설정 화면으로 이동 |
| `requestUsageStatsPermission` | - | `Boolean` | '사용 정보 접근' 설정 화면으로 이동 |
| `startFocusMode` | `{"durationSec": Int, "blockedList": List<String>}` | `Boolean` | 포그라운드 서비스 및 플로팅 뱃지 시작 |
| `stopFocusMode` | - | `Boolean` | 포그라운드 서비스 및 오버레이 해제 |
| `updateBlockedList` | `{"blockedList": List<String>}` | `Boolean` | 차단 앱 목록 실시간 동기화 |

---

## 4. 개발 검증 및 테스트 환경 규격 (Testing & Verification Strategy)

본 프로젝트는 불필요한 APK 수동 빌드/설치 오버헤드를 제거하고 이터레이션 속도를 극대화하기 위해 다계층 테스트 환경을 표준으로 규정한다.

```mermaid
flowchart LR
    DevCode[개발 코드 / 상태 머신] --> WebTest["[Tier 1] 웹 브라우저 즉시 구동<br/>(Web Prototype / Flutter Web)"]
    DevCode --> AVDTest["[Tier 2] PC 가상 에뮬레이터<br/>(Android Studio AVD)"]
    DevCode --> DeviceTest["[Tier 3] 실기기 USB 디버깅<br/>(Hot Reload)"]

    WebTest -->|초고속 즉시 검증| V1[2인 동시 상호작용 / 룰셋 / UI 애니메이션]
    AVDTest -->|OS 레벨 검증| V2[SYSTEM_ALERT_WINDOW / 사용량 감지]
    DeviceTest -->|최종 실기기 검증| V3[실제 디바이스 사용성 / 백그라운드 배터리]
```

### 4.1 Tier 1: 웹 브라우저 즉시 구동 (Primary Instant Verification) - 기본 표준
- **실행 방식**: `prototype/index.html` 단독 실행 또는 `python prototype/server.py` / `flutter run -d chrome` 구동.
- **주요 검증 영역**:
  - **부부 2인 동시 상호작용**: PC 한 화면에서 엄마(지은) 👩 와 아빠(민수) 👨 화면을 나란히 띄워 실시간 푸시, 할 일 완료 칭찬, 페널티 청구/방어 배틀 검증.
  - **게이미피케이션 상태 전이**: 이지 모드(30분 봐주기/유예) vs 하드 모드(즉시 페널티 자동 발급) 로직 실시간 검증.
  - **가상 OS 오버레이 시뮬레이션**: 딴짓 앱 실행 감지 및 차단 오버레이, 드래그 가능한 플로팅 뱃지 UI 시뮬레이션.
- **목적**: 설치 대기 시간 0초로 기획·UI·비즈니스 로직을 즉각 수정 및 검증.

### 4.2 Tier 2: PC 가상 안드로이드 에뮬레이터 (Native OS Verification)
- **실행 방식**: Android Studio AVD (Pixel / Galaxy 가상 기기) 실행 후 `flutter run`.
- **주요 검증 영역**: Kotlin MethodChannel 연동, `UsageStatsManager` 실제 폴링 주기, `WindowManager` 플로팅 오버레이 네이티브 렌더링.

### 4.3 Tier 3: 실기기 USB/무선 디버깅 (Device Hot Reload)
- **실행 방식**: 개발자 모드 활성화 스마트폰 연결 후 `flutter run` 실행 (APK 수동 복사/설치 불필요, `Ctrl+S` 시 0.5초 핫리로드).

