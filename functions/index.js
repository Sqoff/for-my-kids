// 뽀마키즈 베타 서버 함수 (ADR 39)
// 1) spousePush: 가족 문서에서 배우자 알림함에 새 알림이 생기면 그 사람 폰으로 FCM 푸시
// 2) coupangLink: 사용자 쿠팡 파트너스 키(Secret Manager)로 검색 딥링크 생성
const { onDocumentUpdated } = require('firebase-functions/v2/firestore');
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const { initializeApp } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const crypto = require('crypto');

initializeApp();
const REGION = 'asia-northeast3';

function parseNotifs(state, role) {
  try { return (JSON.parse((state || {})['u_' + role] || 'null') || {}).notifications || []; } catch (e) { return []; }
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
    const oldIds = new Set(parseNotifs(before.state, role).map(n => n.id));
    const fresh = parseNotifs(after.state, role).filter(n => n && !oldIds.has(n.id) && !n.read).slice(0, 3);
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
