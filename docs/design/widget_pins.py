# 위젯 캐릭터 핀 (ADR 80): 사용자 캐릭터(엄마=토끼, 아빠=곰) → 동그라미 + 아래 꼭지, 귀는 동그라미 위로 살짝 나오게
import os
from PIL import Image, ImageDraw, ImageFilter, ImageChops

SRC = os.path.dirname(os.path.abspath(__file__))  # widget-character-*.png
RES = r'G:\개발\for-my-kids\android\app\src\main\res'
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_out')

def cutout(path):
    im = Image.open(path).convert('RGB')
    w, h = im.size
    # 흰 배경: 거의 순백(무채색)인 픽셀만, 가장자리에서 이어진 부분만 지움
    px = im.load()
    bgmask = Image.new('L', (w, h), 0)
    cand = Image.new('L', (w, h), 0)
    cp = cand.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if min(r, g, b) >= 248 and max(r, g, b) - min(r, g, b) <= 4:
                cp[x, y] = 255
    # 가장자리에서 flood fill
    work = cand.copy()
    for seed in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (w // 2, 0), (0, h // 2), (w - 1, h // 2), (w // 2, h - 1)]:
        if work.getpixel(seed) == 255:
            ImageDraw.floodfill(work, seed, 128)
    bgmask = work.point(lambda v: 255 if v == 128 else 0)
    alpha = ImageChops.invert(bgmask).filter(ImageFilter.GaussianBlur(1.2))
    rgba = im.convert('RGBA'); rgba.putalpha(alpha)
    return rgba

def pin(src, name, ring, fill, face_xy, head_w, scale_k=1.0):
    S = 4                       # 64x74 기준의 4배
    W, H = 64 * S, 74 * S
    cx, cy, R = 32 * S, 33 * S, 27 * S
    ch = cutout(os.path.join(SRC, src))
    # 캐릭터 크기: 머리 폭이 동그라미 지름의 약 0.95
    k = (2 * R * 0.95 / head_w) * scale_k
    ch = ch.resize((int(ch.width * k), int(ch.height * k)), Image.LANCZOS)
    fx, fy = int(face_xy[0] * k), int(face_xy[1] * k)
    ox, oy = cx - fx, cy + int(4 * S) - fy           # 얼굴을 원 중심보다 살짝 아래
    layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    layer.paste(ch, (ox, oy), ch)
    canvas = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)
    # 꼭지 + 테두리 원
    d.polygon([(cx - 9 * S, cy + R - 2 * S), (cx + 9 * S, cy + R - 2 * S), (cx, cy + R + 13 * S)], fill=ring)
    d.ellipse([cx - R - 3 * S, cy - R - 3 * S, cx + R + 3 * S, cy + R + 3 * S], fill=ring)
    d.ellipse([cx - R, cy - R, cx + R, cy + R], fill=fill)
    # 캐릭터: 원 안 + 원 윗부분(중심선 위)으로는 밖으로 나와도 됨(귀)
    clip = Image.new('L', (W, H), 0)
    cd = ImageDraw.Draw(clip)
    cd.ellipse([cx - R, cy - R, cx + R, cy + R], fill=255)
    cd.rectangle([0, 0, W, cy - R // 3], fill=255)
    a = ImageChops.multiply(layer.getchannel('A'), clip)
    layer.putalpha(a)
    canvas.alpha_composite(layer)
    canvas = canvas.resize((W // 2, H // 2), Image.LANCZOS)  # 128x148 로 저장 (위젯에 충분)
    os.makedirs(os.path.join(RES, 'drawable-nodpi'), exist_ok=True)
    canvas.save(os.path.join(RES, 'drawable-nodpi', name + '.png'), optimize=True)
    os.makedirs(OUT, exist_ok=True); canvas.save(os.path.join(OUT, name + '_preview.png'))

# 얼굴 중심(눈 사이·코 근처)과 머리 폭(원본 픽셀)
pin('widget-character-mom-rabbit.png', 'widget_pin_mom', (245, 154, 174, 255), (255, 233, 238, 255), face_xy=(627, 640), head_w=700, scale_k=0.88)
pin('widget-character-dad-bear.png', 'widget_pin_dad', (127, 176, 245, 255), (227, 238, 254, 255), face_xy=(627, 600), head_w=760)
print('ok')
