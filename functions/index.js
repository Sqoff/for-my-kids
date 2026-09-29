// 뽀마키즈 베타 서버 함수 (ADR 39)
// 1) spousePush: 가족 문서에서 배우자 알림함에 새 알림이 생기면 그 사람 폰으로 FCM 푸시
// 2) coupangLink: 사용자 쿠팡 파트너스 키(Secret Manager)로 검색 딥링크 생성
// 3) eventReminders: 가족 일정 알림 (5분마다) — 아침 8시 '오늘의 일정' 요약 + 시간 있는 일정 30분 전 (ADR 52)
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
