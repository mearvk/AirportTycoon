#!/usr/bin/env python3
"""
Moria — sprite-set generator (pure Python standard library, no dependencies).

Creates a COMPLETE directional sprite set for a named character, in the exact
on-disk layout the game's animation engine (game/MoriaAnimation.sleela) loads,
plus the full 4-direction x 3-frame matrix the designer asked for:

    images/<character>/
        <character>-<frame>.png                 (base, facing-neutral)
        <character>-<frame>-<dir>.png           (directional)
        sprite.manifest

  directions : top, down, left, right     (the four facings)
  frames     : start, mid, end            (end == the engine's "stop" pose)

Backward-compatibility: the engine's MoriaAnimation.framePath() asks for
  images/<char>/<char>-<start|mid|stop>-<left|right>.png
so this generator ALSO writes the "stop" aliases (same pixels as "end") and the
left/right facings, so a gandalf set drops straight into the existing walk code
and shows up with no further wiring.

When a real sprite SHEET exists, prefer tools/autocrop_sprites.py to slice it;
this generator makes clean, labelled PLACEHOLDER poses (a directional figure
with the character name, pose, and facing drawn on) so the slots are filled,
valid, and visibly distinct until the art is dropped in.

Usage:
  python3 tools/make_sprite_set.py CHARACTER [--out images] [--w 320 --h 480]
"""
import os, struct, zlib, argparse

FRAMES = ["start", "mid", "end"]          # 'end' is the designer's name for stop
FRAME_ALIASES = {"end": "stop"}           # engine compatibility: end -> stop
DIRECTIONS = ["top", "down", "left", "right"]

# ---- A tiny 5x7 bitmap font so the placeholders are legibly labelled --------
FONT = {
 "A":["01110","10001","10001","11111","10001","10001","10001"],
 "B":["11110","10001","11110","10001","10001","10001","11110"],
 "C":["01111","10000","10000","10000","10000","10000","01111"],
 "D":["11110","10001","10001","10001","10001","10001","11110"],
 "E":["11111","10000","11110","10000","10000","10000","11111"],
 "F":["11111","10000","11110","10000","10000","10000","10000"],
 "G":["01111","10000","10000","10111","10001","10001","01110"],
 "H":["10001","10001","11111","10001","10001","10001","10001"],
 "I":["11111","00100","00100","00100","00100","00100","11111"],
 "L":["10000","10000","10000","10000","10000","10000","11111"],
 "M":["10001","11011","10101","10101","10001","10001","10001"],
 "N":["10001","11001","10101","10011","10001","10001","10001"],
 "O":["01110","10001","10001","10001","10001","10001","01110"],
 "P":["11110","10001","10001","11110","10000","10000","10000"],
 "R":["11110","10001","10001","11110","10100","10010","10001"],
 "S":["01111","10000","10000","01110","00001","00001","11110"],
 "T":["11111","00100","00100","00100","00100","00100","00100"],
 "U":["10001","10001","10001","10001","10001","10001","01110"],
 "W":["10001","10001","10001","10101","10101","11011","10001"],
 "D2":["11110","10001","10001","10001","10001","10001","11110"],
 "-":["00000","00000","00000","11111","00000","00000","00000"],
 " ":["00000","00000","00000","00000","00000","00000","00000"],
}

def _blank(w, h, rgb):
    buf = bytearray(w*h*3)
    for i in range(w*h):
        buf[i*3] = rgb[0]; buf[i*3+1] = rgb[1]; buf[i*3+2] = rgb[2]
    return buf

def _set(buf, w, x, y, rgb):
    if 0 <= x < w and 0 <= y*w*3 < len(buf):
        o = (y*w + x)*3
        if 0 <= o <= len(buf)-3:
            buf[o] = rgb[0]; buf[o+1] = rgb[1]; buf[o+2] = rgb[2]

def _rect(buf, w, h, x0, y0, x1, y1, rgb, fill=True):
    for y in range(max(0,y0), min(h,y1)):
        for x in range(max(0,x0), min(w,x1)):
            if fill or x==x0 or x==x1-1 or y==y0 or y==y1-1:
                _set(buf, w, x, y, rgb)

def _text(buf, w, h, x, y, s, rgb, scale=2):
    cx = x
    for ch in s.upper():
        glyph = FONT.get(ch, FONT[" "])
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            _set(buf, w, cx+gx*scale+sx, y+gy*scale+sy, rgb)
        cx += (5*scale + scale)

def _triangle(buf, w, h, cx, cy, size, direction, rgb):
    """A filled direction arrow so the facing reads at a glance."""
    for dy in range(-size, size+1):
        for dx in range(-size, size+1):
            inside = False
            if direction == "top":    inside = (dy <= 0) and (abs(dx) <= (size + dy))
            elif direction == "down": inside = (dy >= 0) and (abs(dx) <= (size - dy))
            elif direction == "left": inside = (dx <= 0) and (abs(dy) <= (size + dx))
            elif direction == "right":inside = (dx >= 0) and (abs(dy) <= (size - dx))
            if inside:
                _set(buf, w, cx+dx, cy+dy, rgb)

# A distinct hue per direction so the four facings are visually separable.
DIR_RGB = {
    "top":   (70, 130, 220),
    "down":  (220, 150, 60),
    "left":  (90, 190, 110),
    "right": (200, 90, 120),
}
# A per-frame leg/stride offset so start/mid/end read as a gait.
FRAME_DX = {"start": -6, "mid": 0, "end": 6}

def make_pose(character, frame, direction, w, h):
    bg = (18, 18, 22)
    accent = DIR_RGB[direction]
    buf = _blank(w, h, bg)
    # border
    _rect(buf, w, h, 0, 0, w, h, (60,60,70), fill=False)
    _rect(buf, w, h, 1, 1, w-1, h-1, (40,40,48), fill=False)

    # A simple stick/figure body, nudged by the frame so the stride shows.
    cx = w//2 + FRAME_DX.get(frame, 0)
    headR = max(8, w//12)
    headY = h//4
    # head
    for yy in range(-headR, headR+1):
        for xx in range(-headR, headR+1):
            if xx*xx + yy*yy <= headR*headR:
                _set(buf, w, cx+xx, headY+yy, accent)
    # body
    _rect(buf, w, h, cx-3, headY+headR, cx+3, h*3//4, accent)
    # legs — splayed on 'mid', together on start/end, to read as a stride
    spread = (w//8) if frame == "mid" else (w//16)
    for t in range(0, h//4):
        _set(buf, w, cx - spread*t//(h//4), h*3//4 + t, accent)
        _set(buf, w, cx + spread*t//(h//4), h*3//4 + t, accent)
    # arms
    arm = (w//7) if frame != "mid" else (w//5)
    for t in range(0, arm):
        _set(buf, w, cx - t, headY+headR+6 + t//3, accent)
        _set(buf, w, cx + t, headY+headR+6 + t//3, accent)

    # facing arrow
    _triangle(buf, w, h, cx, headY, max(10, w//14), direction, (240,240,250))

    # labels: CHARACTER, DIRECTION, FRAME
    _text(buf, w, h, 8, 8, character, (235,235,245), scale=2)
    _text(buf, w, h, 8, h-56, direction, accent, scale=2)
    _text(buf, w, h, 8, h-28, frame, (200,200,210), scale=2)
    return (w, h, buf)

def write_png(path, w, h, rgb):
    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw += rgb[y*w*3:(y+1)*w*3]
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("character")
    ap.add_argument("--out", default="images")
    ap.add_argument("--w", type=int, default=320)
    ap.add_argument("--h", type=int, default=480)
    args = ap.parse_args()

    outdir = os.path.join(args.out, args.character)
    os.makedirs(outdir, exist_ok=True)
    manifest = [f"# sprite manifest for {args.character}",
                f"# generated placeholder set: 4 directions x 3 frames",
                f"# directions: {', '.join(DIRECTIONS)}",
                f"# frames: {', '.join(FRAMES)} (end == engine 'stop')",
                f"size\t{args.w}x{args.h}"]

    def emit(name, frame, direction, tag):
        w, h, buf = make_pose(args.character, frame, direction or "down", args.w, args.h)
        write_png(os.path.join(outdir, name), w, h, buf)
        manifest.append(tag)
        print(f"wrote {outdir}/{name} ({w}x{h})")

    for frame in FRAMES:
        # base, facing-neutral (uses a 'down' figure, no arrow emphasis)
        base = f"{args.character}-{frame}.png"
        emit(base, frame, "down", f"frame\t{frame}\t{base}\t{args.w}x{args.h}")
        # the engine also wants the 'stop' alias of 'end' at base + L/R
        alias = FRAME_ALIASES.get(frame)
        if alias:
            an = f"{args.character}-{alias}.png"
            emit(an, frame, "down", f"frame\t{alias}\t{an}\t{args.w}x{args.h}\t(alias of {frame})")

        for d in DIRECTIONS:
            name = f"{args.character}-{frame}-{d}.png"
            emit(name, frame, d, f"frame\t{frame}\t{d}\t{name}\t{args.w}x{args.h}")
            # engine-compat alias for stop (left/right already in DIRECTIONS)
            if alias and d in ("left", "right"):
                an = f"{args.character}-{alias}-{d}.png"
                emit(an, frame, d, f"frame\t{alias}\t{d}\t{an}\t{args.w}x{args.h}\t(alias of {frame})")

    with open(os.path.join(outdir, "sprite.manifest"), "w") as f:
        f.write("\n".join(manifest) + "\n")
    print(f"wrote {outdir}/sprite.manifest")

if __name__ == "__main__":
    main()
