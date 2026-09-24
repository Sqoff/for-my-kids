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
부부의 당일 할 일 목록.
```json
// Collection: couples/{coupleId}/tasks / Document: {taskId}
{
  "taskId": "TASK_001",
  "title": "아이 목욕 및 로션 바르기",
  "assigneeUid": "USER_ABC_123", // 담당자 UID
  "deadline": "2026-09-24T20:00:00Z",
  "status": "PENDING", // "PENDING" | "COMPLETED" | "OVERDUE" | "EXCUSED"
  "completedAt": null,
  "isRoutine": true,
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

---

## 2. Firebase Cloud Messaging (FCM) 푸시 알림 이벤트 규격

| 이벤트 트리거 (`event_type`) | 수신 대상 | 페이로드 내용 |
|---|---|---|
| `TASK_DEADLINE_APPROACHING` | 담당자 | "⏰ [아이 목욕] 마감 15분 전입니다!" |
| `TASK_COMPLETED` | 배우자 | "🎉 상대방이 [아이 목욕]을 완료했습니다!" |
| `TASK_OVERDUE_EASY` | 배우자 | "👀 상대방의 할 일 시간이 지났습니다. (확인 / 30분 연장)" |
| `PENALTY_AWARDED` | 부부 모두 | "😈 [설거지 미완료]로 아내님께 페널티권이 1장 지급되었습니다." |
| `PENALTY_USED` | 대상자 | "🚨 상대방이 [1시간 폰 추가 차단권]을 사용했습니다!" |
| `DEFENSE_TRIGGERED` | 부부 모두 | "🛡️ 상대방이 [페널티 면제권]으로 방어에 성공했습니다!" |
| `QUEST_COMPLETED` | 부부 모두 | "🏆 [7일 연속 완주!] 페널티 면제권을 획득했습니다." |
