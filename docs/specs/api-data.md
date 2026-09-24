# [Living Spec] 데이터 모델 & API 인터페이스 규격서

> **문서 상태**: Active (SSOT)  
> **최종 갱신일**: 2026-09-24  
> **대상 시스템**: Cloud Firestore 스키마, FCM 알림 이벤트, 데이터 모델 정의

---

## 1. Cloud Firestore 데이터베이스 스키마

### 1.1 `users` 컬렉션
사용자 기본 프로필 정보.
```json
// Collection: users / Document: {userId}
{
  "uid": "USER_ABC_123",
  "name": "홍길동",
  "role": "HUSBAND", // "HUSBAND" | "WIFE" | "PARTNER"
  "coupleId": "COUPLE_XYZ_789", // 연결된 부부 방 ID
  "fcmToken": "eK...token_string",
  "createdAt": "2026-09-24T18:00:00Z"
}
```

### 1.2 `couples` 컬렉션
부부 결속 방 및 설정 정보.
```json
// Collection: couples / Document: {coupleId}
{
  "coupleId": "COUPLE_XYZ_789",
  "inviteCode": "CARE-7892",
  "partnerUids": ["USER_ABC_123", "USER_DEF_456"],
  "mode": "EASY", // "EASY" | "HARD"
  "focusSchedule": {
    "enabled": true,
    "startTime": "18:00",
    "endTime": "21:00",
    "days": [1, 2, 3, 4, 5] // 월~금
  },
  "blockedPackages": [
    "com.google.android.youtube",
    "com.instagram.android",
    "com.zhiliaoapp.musically"
  ],
  "stats": {
    "weeklyFocusMinutes": 480,
    "currentStreakDays": 5
  },
  "createdAt": "2026-09-24T18:00:00Z"
}
```

### 1.3 `tasks` 서브컬렉션
부부의 당일 할 일 및 세부 체크리스트 목록.
```json
// Collection: couples/{coupleId}/tasks / Document: {taskId}
{
  "taskId": "TASK_001",
  "title": "아이 목욕 및 로션 바르기",
  "assigneeUid": "USER_ABC_123", // 담당자 UID
  "deadline": "20:00",           // 수행 목표 시각
  "isRoutine": true,             // true: 고정 루틴 | false: 1회성 할 일
  "routineDays": [0, 1, 2, 3, 4, 5, 6], // 반복 요일 (0:일, 1:월 ... 6:토)
  "targetDate": null,            // 1회성일 경우 대상 날짜 "2026-09-25" (루틴이면 null)
  "subtasks": [                  // 세부 체크리스트 템플릿
    { "id": "st_01", "text": "욕실 온도 체크 & 목욕물 받기" },
    { "id": "st_02", "text": "머리 구석구석 말리기" },
    { "id": "st_03", "text": "보습제 바르고 잠옷 입히기" },
    { "id": "st_04", "text": "욕실 바닥 물기 닦기" }
  ],
  "dateRecords": {               // 일자별 독립 수행 기록 맵 (Key: "YYYY-MM-DD")
    "2026-09-24": {
      "done": true,
      "overdue": false,
      "helperUid": null,
      "subtaskDone": { "st_01": true, "st_02": true, "st_03": true, "st_04": true },
      "incompleteNotice": null,
      "completedAt": "2026-09-24T20:10:00Z"
    }
  },
  "createdAt": "2026-09-24T09:00:00Z"
}
```

### 1.4 `penalty_cards` 서브컬렉션
발급된 페널티권 및 사용 이력.
```json
// Collection: couples/{coupleId}/penalty_cards / Document: {cardId}
{
  "cardId": "PENALTY_001",
  "ownerUid": "USER_DEF_456", // 획득자 (아내)
  "targetUid": "USER_ABC_123", // 대상자 (남편)
  "sourceTaskId": "TASK_001",
  "status": "AVAILABLE", // "AVAILABLE" | "USED" | "DEFENDED" | "EXPIRED"
  "selectedPenalty": {
    "type": "PASS_TASK", // "PASS_TASK" | "BLOCK_PHONE_1H" | "SOLO_NEXT_ROUTINE" | "MASSAGE_15M"
    "description": "오늘 설거지 1회 넘기기"
  },
  "usedAt": null,
  "createdAt": "2026-09-24T20:05:00Z"
}
```

### 1.5 `inventory` 서브컬렉션
게이미피케이션 보상 아이템 인벤토리.
```json
// Collection: couples/{coupleId}/inventory / Document: {itemId}
{
  "itemId": "ITEM_DEFENSE_01",
  "itemType": "DEFENSE_SHIELD", // "DEFENSE_SHIELD" (페널티 면제권) | "DATE_COUPON" (외식 쿠폰) | "FREE_TIME_2H"
  "title": "페널티 1회 방어 면제권",
  "ownerUid": "USER_ABC_123",
  "status": "UNLOCKED", // "UNLOCKED" | "USED"
  "acquiredFromQuestId": "Q_STREAK_7",
  "createdAt": "2026-09-24T12:00:00Z"
}
```

### 1.6 `notifications` 서브컬렉션
부부 상호 작용 및 변경 내역 알림 히스토리.
```json
// Collection: couples/{coupleId}/notifications / Document: {notificationId}
{
  "notificationId": "NOTIF_001",
  "recipientUid": "USER_ABC_123", // 수신자 UID
  "senderUid": "USER_DEF_456",    // 발신자 UID
  "senderName": "지은 (엄마)",
  "type": "TASK",                 // "TASK" | "PENALTY" | "HELP" | "SYSTEM" | "DEFENSE" | "NUDGE"
  "icon": "📋",
  "title": "새 할 일 등록",
  "message": "지은님이 '아이 목욕 및 로션 바르기'를 민수 담당으로 추가했습니다.",
  "time": "17:35",
  "read": false,
  "createdAt": "2026-09-24T17:35:00Z"
}
```

---

## 2. Firebase Cloud Messaging (FCM) 푸시 알림 이벤트 규격

| 이벤트 트리거 (`event_type`) | 수신 대상 | 페이로드 내용 |
|---|---|---|
| `TASK_CREATED` | 배우자 | "📋 [새 할 일 등록] 상대방이 [제목]을(를) 등록했습니다." |
| `TASK_UPDATED` | 배우자 | "✏️ [할 일 수정] 상대방이 [제목] 정보를 수정했습니다." |
| `TASK_DELETED` | 배우자 | "🗑️ [할 일 삭제] 상대방이 [제목]을(를) 삭제했습니다." |
| `TASK_DEADLINE_APPROACHING` | 담당자 | "⏰ [아이 목욕] 마감 15분 전입니다!" |
| `TASK_COMPLETED_PERFECT` | 배우자 | "🎉 상대방이 [아이 목욕]을 모든 체크리스트와 함께 완벽 완료했습니다! (+20 EXP)" |
| `TASK_INCOMPLETE_PASS` | 배우자 | "⚠️ [미흡 완료 알림] 상대방이 [아이 목욕]을 완료했으나, [머리 말리기]를 빼먹었습니다!" |
| `NUDGE_GESTURE` | 배우자 | "👉 [주의/재촉/칭찬] 상대방이 [제스처 메시지]를 보냈습니다." |
| `TASK_HELPED` | 피도움자 | "👼 [천사 배우자] 상대방이 내 [제목]을(를) 대신 완료했습니다! (+35 EXP)" |
| `TASK_OVERDUE_EASY` | 배우자 | "👀 상대방의 할 일 시간이 지났습니다. (확인 / 30분 연장)" |
| `PENALTY_CUSTOMIZED` | 배우자 | "⚡ [패널티 변경] 상대방이 [패널티 제목]을(를) 추가/수정했습니다." |
| `PENALTY_AWARDED` | 부부 모두 | "😈 [설거지 미완료]로 아내님께 페널티권이 1장 지급되었습니다." |
| `PENALTY_USED` | 대상자 | "🚨 상대방이 [1시간 폰 추가 차단권]을 사용했습니다!" |
| `DEFENSE_TRIGGERED` | 부부 모두 | "🛡️ 상대방이 [페널티 면제권]으로 방어에 성공했습니다!" |
| `QUEST_COMPLETED` | 부부 모두 | "🏆 [퀘스트 달성!] 페널티 면제권을 획득했습니다." |
