// 뽀마키즈 베타 서버 함수 (ADR 39)
// 1) spousePush: 가족 문서에서 배우자 알림함에 새 알림이 생기면 그 사람 폰으로 FCM 푸시
// 2) coupangLink: 사용자 쿠팡 파트너스 키(Secret Manager)로 검색 딥링크 생성
// 3) eventReminders: 가족 일정 알림 (5분마다) — 아침 8시 '오늘의 일정' 요약 + 시간 있는 일정 30분 전 (ADR 52)
//    + 할 일 마감 초과 (ADR 59 → ADR 64): 이지 = 마감 때 두 사람에게 알림, 하루가 지나도 못 끝내면 다음 날 아침 8시 부여권
//                                          하드 = 마감 1분 뒤 바로 부여권 (시간 없는 할 일은 하루 지나면)
const { onDocumentUpdated } = require('firebase-functions/v2/firestore');
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { getFirestore } = require('firebase-admin/firestore');
const { defineSecret } = require('firebase-functions/params');
const { initializeApp } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const crypto = require('crypto');

initializeApp();
const REGION = 'asia-northeast3';

// v2(ADR 42): items.u_<role>_notifications.<id> = JSON 문자열 / v1: state.u_<role> 안의 notifications 배열
function parseNotifs(doc, role) {
  const m = ((doc || {}).items || {})['u_' + role + '_notifications'];
  if (m) return Object.values(m).map(j => { try { return JSON.parse(j); } catch (e) { return null; } }).filter(Boolean);
  try { return (JSON.parse(((doc || {}).state || {})['u_' + role] || 'null') || {}).notifications || []; } catch (e) { return []; }
}

exports.spousePush = onDocumentUpdated({ document: 'couples/{code}', region: REGION }, async (event) => {
  const before = event.data.before.data() || {};
  const after = event.data.after.data() || {};
  const tokens = after.tokens || {};
  const jobs = [];
  // 💞 부부 상태가 바뀌면 두 폰의 홈 화면 위젯에 조용한 데이터 푸시 (ADR 66)
  if (JSON.stringify(before.status || {}) !== JSON.stringify(after.status || {})) {
    const data = { type: 'status' };
    for (const r of ['mom', 'dad']) {
      const st = (after.status || {})[r] || {};
      data[r + 'Ic'] = String(st.ic || ''); data[r + 'T'] = String(st.t || ''); data[r + 'Since'] = String(st.since || 0);
      data[r + 'Name'] = displayName(after, r);
    }
    for (const r of ['mom', 'dad']) {
      if (tokens[r]) jobs.push(getMessaging().send({ token: tokens[r], data, android: { priority: 'high' } }).catch(err => console.warn('status push fail', r, err.code || err.message)));
    }
  }
  for (const role of ['mom', 'dad']) {
    const token = tokens[role];
    if (!token) continue;
    // 보낸 사람이 자기 자신에게 알림을 쓰는 경우는 제외 (updatedBy === role)
    if (after.updatedBy === role) continue;
    const oldIds = new Set(parseNotifs(before, role).map(n => n.id));
    const fresh = parseNotifs(after, role).filter(n => n && !oldIds.has(n.id) && !n.read).slice(0, 3);
    for (const n of fresh) {
      jobs.push(getMessaging().send({
        token,
        notification: { title: `${n.icon || '🔔'} ${n.title || '뽀마키즈'}`, body: String(n.message || '').slice(0, 180) },
        android: { priority: 'high', notification: { channelId: 'spouse', sound: 'default' } },
        data: { type: String(n.type || 'system'), notifId: String(n.id || '') }
      }).catch(err => console.warn('push fail', role, err.code || err.message)));
    }
  }
  await Promise.all(jobs);
});

const COUPANG_ACCESS_KEY = defineSecret('COUPANG_ACCESS_KEY');
const COUPANG_SECRET_KEY = defineSecret('COUPANG_SECRET_KEY');

exports.coupangLink = onCall({ region: REGION, secrets: [COUPANG_ACCESS_KEY, COUPANG_SECRET_KEY] }, async (req) => {
  if (!req.auth) throw new HttpsError('unauthenticated', 'login required');
  const keyword = String((req.data && req.data.keyword) || '').trim().slice(0, 50) || '생필품';
  const plain = `https://www.coupang.com/np/search?q=${encodeURIComponent(keyword)}`;
  const ak = COUPANG_ACCESS_KEY.value(), sk = COUPANG_SECRET_KEY.value();
  if (!ak || !sk || ak === 'NONE' || sk === 'NONE') return { url: plain, partner: false };
  const method = 'POST';
  const path = '/v2/providers/affiliate_open_api/apis/openapi/v1/deeplink';
  const d = new Date();
  const p2 = (n) => String(n).padStart(2, '0');
  const datetime = `${String(d.getUTCFullYear()).slice(2)}${p2(d.getUTCMonth() + 1)}${p2(d.getUTCDate())}T${p2(d.getUTCHours())}${p2(d.getUTCMinutes())}${p2(d.getUTCSeconds())}Z`;
  const signature = crypto.createHmac('sha256', sk).update(datetime + method + path).digest('hex');
  try {
    const r = await fetch('https://api-gateway.coupang.com' + path, {
      method,
      headers: { 'Content-Type': 'application/json;charset=UTF-8', Authorization: `CEA algorithm=HmacSHA256, access-key=${ak}, signed-date=${datetime}, signature=${signature}` },
      body: JSON.stringify({ coupangUrls: [plain], subId: 'bbomakids' })
    });
    const j = await r.json();
    const item = j && j.data && j.data[0];
    return { url: (item && (item.shortenUrl || item.landingUrl)) || plain, partner: !!item };
  } catch (e) {
    console.warn('coupang fail', e.message);
    return { url: plain, partner: false };
  }
});

// ---------- 📅 가족 일정 알림 ----------
// v2: items.events.<id> = JSON 문자열 / v1: state.events = 배열 JSON
function parseList(doc, key) {
  const m = ((doc || {}).items || {})[key];
  if (m) return Object.values(m).map(j => { try { return JSON.parse(j); } catch (e) { return null; } }).filter(Boolean);
  try { return JSON.parse(((doc || {}).state || {})[key] || '[]') || []; } catch (e) { return []; }
}
function kstNow() {
  const d = new Date(Date.now() + 9 * 3600e3);
  return { date: d.toISOString().slice(0, 10), min: d.getUTCHours() * 60 + d.getUTCMinutes() };
}
const DAILY_FROM = 8 * 60, DAILY_UNTIL = 10 * 60; // 아침 요약: 8시~10시 사이 첫 실행 1회
const BEFORE_MIN = 30; // 시간 있는 일정: 30분 전부터 1회

exports.eventReminders = onSchedule({ schedule: 'every 5 minutes', timeZone: 'Asia/Seoul', region: REGION }, async () => {
  const db = getFirestore();
  const { date, min } = kstNow();
  const snap = await db.collection('couples').get();
  const jobs = [];
  const send = (tokens, roles, title, body, extra) => roles.filter(r => tokens[r]).map(r => getMessaging().send({
    token: tokens[r],
    notification: { title, body: String(body).slice(0, 180) },
    android: { priority: 'high', notification: { channelId: 'events', sound: 'default' } },
    data: Object.assign({ type: 'event', date }, extra || {})
  }).catch(err => console.warn('event push fail', r, err.code || err.message)));
  // 같은 알림을 두 번 보내지 않도록 reminderLog/{가족_키} 문서를 먼저 만들어 봄 (이미 있으면 건너뜀)
  const once = async (code, key, fn) => {
    try { await db.collection('reminderLog').doc(`${code}_${key}`).create({ at: Date.now() }); } catch (e) { return; }
    await Promise.all(fn());
  };
  for (const doc of snap.docs) {
    const data = doc.data() || {};
    const tokens = data.tokens || {};
    // 할 일 마감 초과 → 페널티 부여권 (새 버전 앱이 켠 가족만)
    if (data.features && data.features.overdueGrant) jobs.push(checkOverdue(db, doc, data, date, min, once));
    if (!tokens.mom && !tokens.dad) continue;
    const today = parseList(data, 'events').filter(e => e && e.date === date)
      .sort((a, b) => (a.time || '99').localeCompare(b.time || '99'));
    if (!today.length) continue;
    if (min >= DAILY_FROM && min < DAILY_UNTIL) {
      const list = today.slice(0, 3).map(e => `${e.time || '종일'} ${e.title}`).join(' · ') + (today.length > 3 ? ` 외 ${today.length - 3}개` : '');
      jobs.push(once(doc.id, `${date}_daily`, () => send(tokens, ['mom', 'dad'], `📅 오늘의 일정 ${today.length}개`, list)));
    }
    for (const e of today) {
      const m = /^(\d{1,2}):(\d{2})/.exec(e.time || '');
      if (!m) continue;
      const left = (+m[1]) * 60 + (+m[2]) - min;
      if (left < 0 || left > BEFORE_MIN) continue;
      const roles = (e.who === 'mom' || e.who === 'dad') ? [e.who] : ['mom', 'dad'];
      const when = left <= 1 ? '곧' : `${left}분 후`;
      jobs.push(once(doc.id, `${date}_${e.id}_${e.time}`, () => send(tokens, roles, `${e.icon || '⏰'} ${when} 일정: ${e.title}`, `${e.time}${e.memo ? ` · ${e.memo}` : ''}`, { eventId: String(e.id || '') })));
    }
  }
  await Promise.all(jobs);
});

// ---------- ⚖️ 할 일 마감 초과 → 🎫 페널티 부여권 (ADR 59) ----------
const HARD_GRACE = 1;       // 하드 모드: 마감 1분 뒤 바로
const ISSUE_WINDOW = 120;   // 마감 알림·하드 발급은 이 시간 안에서만 (배포 직후 지난 일까지 몰아서 보내지 않도록)
const DAYEND_FROM = 8 * 60, DAYEND_UNTIL = 10 * 60; // 하루가 지나도 못 끝낸 할 일 → 다음 날 아침 8~10시 첫 실행에 부여권
function parseWhole(data, key, def) { try { const v = JSON.parse(((data || {}).state || {})[key] || 'null'); return v == null ? def : v; } catch (e) { return def; } }
function displayName(data, role) {
  const u = parseWhole(data, 'u_' + role, {}) || {};
  return u.nameSet && u.name ? u.name : (role === 'mom' ? '엄마' : '아빠');
}
function kstDateOfMs(ms) { return new Date(ms + 9 * 3600e3).toISOString().slice(0, 10); }
// 앱의 isTaskActiveOnDate와 같은 규칙 (루틴: 요일·시작일·종료일 / 1회성: 대상 날짜)
function taskActiveOn(t, date) {
  if (t.isRoutine !== false) {
    const dow = new Date(date + 'T00:00:00Z').getUTCDay();
    const days = t.routineDays || [1, 2, 3, 4, 5, 6, 0];
    if (!days.includes(dow)) return false;
    let start = t.startDate;
    if (!start) { const m = /^t_(\d{12,})/.exec(t.id || ''); if (m) start = kstDateOfMs(+m[1]); }
    if (start && date < start) return false;
    if (t.endDate && date > t.endDate) return false;
    return true;
  }
  return t.targetDate === date;
}
function makeNotif(type, icon, title, message) {
  const now = Date.now();
  const d = new Date(now + 9 * 3600e3);
  return { id: `notif_${now}_${Math.random().toString(36).slice(2, 6)}`, type, icon, title, message, time: `${String(d.getUTCHours()).padStart(2, '0')}:${String(d.getUTCMinutes()).padStart(2, '0')}`, read: false, timestamp: now, _o: now };
}
// [제목] 뒤 을/를: 마지막 글자 받침에 맞춤
function eul(w) { const c = String(w || '').trim().slice(-1).charCodeAt(0); return c >= 0xAC00 && c <= 0xD7A3 ? ((c - 0xAC00) % 28 ? '을' : '를') : '을(를)'; }
function prevDate(date) { return new Date(Date.parse(date + 'T00:00:00Z') - 86400e3).toISOString().slice(0, 10); }
function issueGrant(doc, data, t, day, mode, reason, hhmm) {
  const who = t.assignee, spouse = who === 'mom' ? 'dad' : 'mom';
  const whoName = displayName(data, who), spName = displayName(data, spouse);
  const now = Date.now();
  const what = reason === 'dayend' ? `어제 [${t.title}]${eul(t.title)} 끝내지 못했어요` : `[${t.title}] 마감(${hhmm})을 넘겼어요`;
  const g = { id: `pg_${now}_${Math.random().toString(36).slice(2, 6)}`, kind: 'grant', title: '페널티 부여권', icon: '🎫', desc: `${whoName}님이 ${what}`, taskId: t.id, taskTitle: t.title, date: day, deadline: hhmm || '', lateName: whoName, mode, reason, createdAt: now, _o: now };
  const toSpouse = makeNotif('penalty', '🎫', '페널티 부여권이 생겼어요', `${whoName}님이 ${what}. 벌칙을 골라 보내거나 이번엔 넘어갈 수 있어요.`);
  const toLate = makeNotif('penalty', '⌛', reason === 'dayend' ? '어제 못 한 할 일이 있어요' : '마감을 넘겼어요', `${reason === 'dayend' ? `어제 [${t.title}]${eul(t.title)} 못 끝내서` : `[${t.title}] 마감(${hhmm})이 지나`} ${spName}님에게 🎫 페널티 부여권이 생겼어요.`);
  return [doc.ref.update({
    [`items.u_${spouse}_penalties.${g.id}`]: JSON.stringify(g),
    [`items.u_${spouse}_notifications.${toSpouse.id}`]: JSON.stringify(toSpouse),
    [`items.u_${who}_notifications.${toLate.id}`]: JSON.stringify(toLate),
    updatedBy: 'server'
  })];
}
async function checkOverdue(db, doc, data, date, min, once) {
  const mode = parseWhole(data, 'mode', 'easy') === 'hard' ? 'hard' : 'easy';
  const tasks = parseList(data, 'tasks');
  const writes = [];
  const yday = prevDate(date);
  for (const t of tasks) {
    if (!t || t.isPenaltyTask || (t.assignee !== 'mom' && t.assignee !== 'dad')) continue;
    const m = /^(\d{1,2}):(\d{2})/.exec(t.deadline || '');
    const hhmm = m ? `${m[1].padStart(2, '0')}:${m[2]}` : '';
    const who = t.assignee, spouse = who === 'mom' ? 'dad' : 'mom';

    // ① 오늘 마감이 지남
    if (m && taskActiveOn(t, date)) {
      const rec = (t.dateRecords || {})[date];
      const late = min - ((+m[1]) * 60 + (+m[2]));
      if (!(rec && rec.done)) {
        if (mode === 'easy' && late >= 0 && late <= ISSUE_WINDOW) {
          // 이지: 두 사람에게 한 번씩만 알림 (부여권 없음)
          writes.push(once(doc.id, `${date}_${t.id}_due`, () => {
            const whoName = displayName(data, who);
            const nMe = makeNotif('todo', '⏰', '마감이 지났어요', `[${t.title}] 마감(${hhmm})이 지났어요. 오늘 안에만 끝내면 괜찮아요`);
            const nSp = makeNotif('todo', '⏰', '마감이 지났어요', `${whoName}님의 [${t.title}] 마감(${hhmm})이 지났어요`);
            return [doc.ref.update({ [`items.u_${who}_notifications.${nMe.id}`]: JSON.stringify(nMe), [`items.u_${spouse}_notifications.${nSp.id}`]: JSON.stringify(nSp), updatedBy: 'server' })];
          }));
        }
        if (mode === 'hard' && late >= HARD_GRACE && late <= HARD_GRACE + ISSUE_WINDOW) {
          writes.push(once(doc.id, `${date}_${t.id}_grant`, () => issueGrant(doc, data, t, date, mode, 'deadline', hhmm)));
        }
      }
    }

    // ② 어제 하루 동안 못 끝낸 할 일 → 아침 8~10시에 부여권 (하드 모드의 시간 있는 할 일은 ①에서 이미 처리)
    if (min >= DAYEND_FROM && min < DAYEND_UNTIL && taskActiveOn(t, yday) && !(mode === 'hard' && m)) {
      const rec = (t.dateRecords || {})[yday];
      if (!(rec && rec.done)) {
        writes.push((async () => {
          // 예전 규칙(30분)으로 어제 이미 부여권이 나간 할 일은 한 번 더 주지 않음
          try { if ((await db.collection('reminderLog').doc(`${doc.id}_${yday}_${t.id}_grant`).get()).exists) return; } catch (e) {}
          await once(doc.id, `${yday}_${t.id}_dayend`, () => issueGrant(doc, data, t, yday, mode, 'dayend', hhmm));
        })());
      }
    }
  }
  await Promise.all(writes);
}
