# 날씨별 뽀마 이미지 생성 프롬프트 (ADR 54 후속)

현재 앱은 원본 `prototype/assets/bboma_hero.png` 위에 SVG로 옷/효과를 덧입힌다(v0.0.28).
같은 3D 질감을 원하면 아래 프롬프트로 날씨별 이미지를 새로 만들어 교체한다.

## 사용 방법
1. 이미지 생성 AI(ChatGPT 이미지 생성 권장 — 참고 이미지 유지·투명 배경 지원)에 **원본 `bboma_hero.png`를 첨부**한다.
2. **공통 프롬프트 + 날씨별 프롬프트**를 이어 붙여 한 장씩 만든다. 같은 대화창에서 계속 만들면 캐릭터가 덜 바뀐다.
3. 얼굴이 달라지면: `Keep the face, eyes, blush and fur exactly like the reference image.` 를 덧붙여 다시 요청.
4. 배경이 투명하지 않으면 흰 배경으로 받아도 된다 (앱에 넣을 때 잘라냄).
5. 파일 이름: `bboma_wx_sunny.png`, `bboma_wx_hot.png`, `bboma_wx_night.png`, `bboma_wx_cloudy.png`, `bboma_wx_rain.png`, `bboma_wx_snow.png`, `bboma_wx_storm.png`

## 공통 프롬프트 (매번 맨 앞에)
```
Use the attached image as the exact character reference and keep the character identical:
a round, fluffy white cloud mascot with soft realistic fur, big glossy blue eyes, pink blush cheeks,
a small open smiling mouth, tiny stubby arms and feet, and a small gold crown on the top right of its head.
Same soft 3D render style, same soft light from the upper left, same front camera angle, same body proportions and pose.
Full body, centered, square 1024x1024 canvas, the character takes about 75% of the height with some empty space above the head.
Transparent background (PNG with alpha). No ground shadow, no background scenery, no text, no frame.
Outfit and props must look like real materials in the same 3D style (fabric, plastic, knit), fitted naturally to the round body.
```

## 날씨별 프롬프트
| 파일 | 날씨 | 프롬프트 |
|---|---|---|
| `bboma_wx_sunny.png` | 맑음 | `Weather: bright sunny day. Warm golden sunlight from the upper left gives a soft warm rim light on the fur. A small glowing 3D sun peeks from behind the upper-left side of the head. A few tiny sparkles. Happy, cheerful expression.` |
| `bboma_wx_hot.png` | 더움 (28°↑) | `Weather: very hot summer day. The mascot wears small round black sunglasses resting on its eyes, holds a tiny pastel ice cream cone in one hand, and has one small sweat drop on the side of its head. Warm, bright light.` |
| `bboma_wx_night.png` | 맑은 밤 | `Weather: clear night. Soft blue moonlight tint on the fur. The mascot wears a soft light-blue striped nightcap with a small pom-pom (the gold crown is still visible, peeking out), holds a small star-shaped plush, sleepy gentle smile. A small glowing crescent moon behind the upper-left side.` |
| `bboma_wx_cloudy.png` | 흐림 | `Weather: cloudy, overcast day. The fur is slightly grayer and darker like a rain cloud (light gray instead of pure white), with a calm, slightly pouty expression. One or two small soft gray cloud puffs float near its head.` |
| `bboma_wx_rain.png` | 비 | `Weather: rainy day. The mascot holds a small clear transparent vinyl umbrella above its head with one tiny hand (the crown is visible through the clear umbrella), raindrops on the umbrella, a few tiny water droplets on the fur, and small glossy yellow rain boots. A few thin rain streaks around.` |
| `bboma_wx_snow.png` | 눈 | `Weather: snowy winter day. The mascot wears a chunky red knit scarf wrapped once around its body with one end hanging down, and fluffy red knit earmuffs. A few snowflakes resting on the fur and falling around. Rosy cheeks from the cold.` |
| `bboma_wx_storm.png` | 태풍·비바람 | `Weather: typhoon / strong wind and rain. The mascot wears a glossy yellow raincoat fitted to its round body and a matching yellow sou'wester rain hat (covering the crown), plus yellow rain boots. Its fur is slightly windswept to one side, brave determined expression, a few diagonal rain streaks.` |

## 앱에 넣는 방식 (이미지가 준비되면)
- 파일을 `prototype/assets/`에 넣고, 해당 날씨일 때 `<img class="fh-illust">`의 `src`만 바꾼다.
- 새 이미지에는 옷이 그려져 있으므로 SVG 옷 레이어(`body`/`front`의 옷)는 끄고, 비·눈 입자와 칩(`☀️ 22°`)만 유지.
- 없는 날씨는 지금처럼 SVG 스킨으로 대체.
