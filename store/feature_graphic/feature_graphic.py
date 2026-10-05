# -*- coding: utf-8 -*-
"""
MzDKPlayer 商店置顶大图（Feature Graphic）生成脚本
输出: 1024x500 PNG，用于 Google Play / APKPure 的应用详情页置顶大图

设计要点
- 深色科技感背景 + 青色光晕，与应用内的深色主题一致
- 左侧品牌区（反白后的应用图标 + 名称 + 中文卖点 + 标签）
- 右侧使用真实截图（非虚构界面），圆角卡片 + 投影 + 青色辉光
- 全程 2 倍超采样后缩放，保证圆角与描边平滑
- 关键信息集中在中央安全区（左右各留 60px 以上），避免被商店裁切时丢失

用法: <venv>/Scripts/python.exe feature_graphic.py
依赖: Pillow
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ---------------------------------------------------------------- 基础常量

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT_DIR = os.path.join(ROOT, "store", "feature_graphic")
SHOT_DIR = os.path.join(ROOT, "screenshots")
LOGO = os.path.join(ROOT, "app", "src", "main", "res", "mipmap-xxxhdpi", "ic_launcher.png")

TARGET_W, TARGET_H = 1024, 500
K = 2                      # 超采样倍率
W, H = TARGET_W * K, TARGET_H * K


def p(v):
    """目标坐标 -> 绘制坐标"""
    return int(round(v * K))


FONT_ZH_B = r"C:\Windows\Fonts\msyhbd.ttc"
FONT_ZH = r"C:\Windows\Fonts\msyh.ttc"
FONT_EN_B = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_EN = r"C:\Windows\Fonts\segoeui.ttf"

ACCENT_CYAN = (34, 211, 238)
ACCENT_VIOLET = (124, 108, 246)


def font(path, size, index=0):
    return ImageFont.truetype(path, p(size), index=index)


# ---------------------------------------------------------------- 绘制组件

def make_background():
    """对角渐变底 + 克制的光晕 + 同心圆环 + 点阵网格 + 极轻噪点"""
    sw, sh = 128, 64
    small = Image.new("RGB", (sw, sh))
    load = small.load()
    c1, c2 = (7, 10, 17), (15, 27, 43)
    for y in range(sh):
        for x in range(sw):
            t = (x / (sw - 1)) * 0.62 + (y / (sh - 1)) * 0.38
            load[x, y] = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
    img = small.resize((W, H), Image.LANCZOS).convert("RGBA")

    def glow(center, radius, color, strength):
        layer = Image.new("L", (W, H), 0)
        d = ImageDraw.Draw(layer)
        cx, cy = p(center[0]), p(center[1])
        r = p(radius)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=strength)
        layer = layer.filter(ImageFilter.GaussianBlur(r * 0.62))
        img.paste(Image.new("RGB", (W, H), color), (0, 0), layer)

    glow((905, 20), 300, ACCENT_CYAN, 44)
    glow((90, 495), 250, ACCENT_VIOLET, 30)
    glow((470, 250), 400, (22, 58, 92), 26)

    # 同心圆环（呼应应用图标的环形轮廓）
    ring = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(ring)
    for r in (300, 372):
        cx, cy, rr = p(880), p(250), p(r)
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=26, width=p(1))
    img.paste(Image.new("RGB", (W, H), (140, 200, 235)), (0, 0), ring)

    # 点阵网格
    grid = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(grid)
    r = p(1)
    for x in range(0, W, p(34)):
        for y in range(0, H, p(34)):
            d.ellipse([x - r, y - r, x + r, y + r], fill=11)
    img.paste(Image.new("RGB", (W, H), (150, 205, 235)), (0, 0), grid)

    # 噪点（抑制渐变色带）
    noise = Image.effect_noise((W, H), 26).convert("L").point(lambda v: int(v * 0.05))
    img.paste(Image.new("RGB", (W, H), (255, 255, 255)), (0, 0), noise)
    return img


def white_logo(size):
    """把白底黑线的应用图标转成白色线稿（透明底），用于深色大图"""
    gray = Image.open(LOGO).convert("L").resize((p(size), p(size)), Image.LANCZOS)
    alpha = gray.point(lambda v: min(255, int((255 - v) * 1.35)))
    out = Image.new("RGBA", (p(size), p(size)), (255, 255, 255, 0))
    out.putalpha(alpha)
    return out


def rounded_paste(img, shot, box, radius=14, shadow=True):
    """截图以圆角卡片形式贴入，带投影与青色辉光"""
    x0, y0, x1, y1 = [p(v) for v in box]
    cw, ch = x1 - x0, y1 - y0

    # 等比裁剪填满卡片（cover）
    sh, sw = shot.height, shot.width
    scale = max(cw / sw, ch / sh)
    shot = shot.resize((int(sw * scale + 0.5), int(sh * scale + 0.5)), Image.LANCZOS)
    left = (shot.width - cw) // 2
    top = (shot.height - ch) // 2
    shot = shot.crop((left, top, left + cw, top + ch))

    mask = Image.new("L", (cw, ch), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, cw - 1, ch - 1], radius=p(radius), fill=255)

    if shadow:
        sh_a = Image.new("L", (W, H), 0)
        sh_a.paste(mask, (x0, y0 + p(16)))
        sh_a = sh_a.filter(ImageFilter.GaussianBlur(p(20)))
        shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        shadow.putalpha(sh_a.point(lambda v: int(v * 0.62)))
        img.paste(shadow, (0, 0), shadow)

        gl_a = Image.new("L", (W, H), 0)
        gl_a.paste(mask, (x0, y0))
        gl_a = gl_a.filter(ImageFilter.GaussianBlur(p(46)))
        gl = Image.new("RGBA", (W, H), (*ACCENT_CYAN, 0))
        gl.putalpha(gl_a.point(lambda v: int(v * 0.30)))
        img.paste(gl, (0, 0), gl)

    card = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    card.paste(shot, (0, 0))
    card.putalpha(mask)
    img.paste(card, (x0, y0), card)

    ImageDraw.Draw(img).rounded_rectangle(
        [x0, y0, x1 - 1, y1 - 1], radius=p(radius),
        outline=(255, 255, 255, 48), width=p(1),
    )


def pill(d, x, y, text, f, height=34, pad=15):
    """圆角标签胶囊，返回下一个 x"""
    w = d.textlength(text, font=f)
    total = w + 2 * p(pad)
    d.rounded_rectangle(
        [x, y, x + total, y + p(height)], radius=p(height // 2),
        fill=(255, 255, 255, 10), outline=(120, 205, 235, 96), width=p(1),
    )
    d.text((x + total / 2, y + p(height) / 2 - p(1)), text, font=f,
           fill=(196, 228, 244, 255), anchor="mm")
    return x + total + p(10)


# ---------------------------------------------------------------- 版式

def build(brand_title, tagline, pills, footnote, shot_name, out_name, cn=True):
    """cn=False 时副标题/胶囊/说明文字改用 Segoe UI 渲染（英文版）"""
    img = make_background()

    # 右侧截图卡片
    shot = Image.open(os.path.join(SHOT_DIR, shot_name)).convert("RGB")
    rounded_paste(img, shot, (500, 118, 956, 374), radius=14)

    d = ImageDraw.Draw(img)
    x0 = 88
    f_sub = font(FONT_ZH if cn else FONT_EN, 21)
    f_pill = font(FONT_ZH_B if cn else FONT_EN_B, 16)
    f_note = font(FONT_ZH if cn else FONT_EN, 15)

    # 顶部小字
    d.text((p(x0), p(152)), "ANDROID TV · MEDIA PLAYER", font=font(FONT_EN_B, 13),
           fill=(34, 211, 238, 220), anchor="la")

    # 品牌行：图标 + 名称
    logo = white_logo(56)
    img.paste(logo, (p(x0), p(168)), logo)
    d.text((p(x0 + 70), p(196)), brand_title, font=font(FONT_EN_B, 46),
           fill=(255, 255, 255, 255), anchor="lm")

    # 副标题
    d.text((p(x0), p(240)), tagline, font=f_sub,
           fill=(146, 170, 192, 255), anchor="la")

    # 标签胶囊
    x = p(x0)
    for label in pills:
        x = pill(d, x, p(286), label, f_pill)

    # 底部说明文字
    d.text((p(x0), p(346)), footnote, font=f_note,
           fill=(110, 132, 152, 255), anchor="la")

    out = os.path.join(OUT_DIR, out_name)
    img.convert("RGB").resize((TARGET_W, TARGET_H), Image.LANCZOS).save(out, "PNG")
    print("saved:", out)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    NOTE_CN = "支持 SMB · NFS · FTP · WebDAV · HTTP 网络存储"
    NOTE_EN = "SMB · NFS · FTP · WebDAV · HTTP network shares"
    # A 版：突出弹幕与播放控制
    build(
        brand_title="MzDKPlayer",
        tagline="安卓电视本地影音播放器",
        pills=["弹幕播放", "4K HDR", "NAS 直连"],
        footnote=NOTE_CN,
        shot_name="Screenshot_20251104_190350.webp",
        out_name="feature_graphic_zh_A_1024x500.png",
    )
    # B 版：突出媒体库与刮削
    build(
        brand_title="MzDKPlayer",
        tagline="电影 · 剧集 · 音乐 一站管理",
        pills=["TMDB 刮削", "双播放内核", "字幕自定义"],
        footnote=NOTE_CN,
        shot_name="Screenshot_20260708_105320.webp",
        out_name="feature_graphic_zh_B_1024x500.png",
    )
    # 英文版（与 A 版同构图，供海外市场使用）
    build(
        brand_title="MzDKPlayer",
        tagline="Your media library, on the big screen",
        pills=["Danmaku", "4K HDR", "NAS Ready"],
        footnote=NOTE_EN,
        shot_name="Screenshot_20251104_190350.webp",
        out_name="feature_graphic_en_1024x500.png",
        cn=False,
    )
