#!/usr/bin/env python3
"""
Moria — dungeon terrain tiles (pure Python standard library, no deps).

The sprite GUI paints the board as a grid of image tiles. The MOVING pieces
(hero + Fellowship + monsters) are the autocropped/rendered character sprites;
the STATIC terrain under them — floor, wall, door, the stairs, treasure, the
dark beyond the torch — needs its own small set of picture tiles so the board
reads as a torch-lit stone hall rather than empty cells.

This tool writes those terrain tiles as clean 64x64 RGBA PNGs under
images/tiles/ in the slick-black / torch-amber theme of the UI:

    floor.png     lit dwarf-stone flagstone (warm, faintly mortared)
    wall.png      rough-hewn rock face (darker, chiselled)
    door.png      a timbered doorway in a stone frame
    stair-down.png a descending stair (amber chevrons pointing down)
    stair-up.png   an ascending stair (amber chevrons pointing up)
    treasure.png  a glint of gold on the floor
    dark.png      the dark beyond the torch (near-black)
    surface.png   open daylight ground (level 1, the surface)

Pure stdlib: a tiny RGBA PNG writer (zlib is stdlib). Deterministic output.

Usage:
  python3 tools/make_tiles.py [--out images] [--size 64]
"""
import os, struct, zlib, argparse, math

# Theme (matches MoriaUI: torch-amber on dwarf-stone).
TORCH = (240, 170, 60)
STONE_LIT = (70, 60, 52)
STONE_DARK = (34, 30, 28)
MORTAR = (52, 46, 42)
GOLD = (240, 200, 80)
WOOD = (120, 80, 46)
DAYLIGHT = (120, 128, 110)
NEARBLACK = (14, 14, 16)


def blank(w, h, rgba):
    buf = bytearray(w * h * 4)
    for i in range(w * h):
        o = i * 4
        buf[o], buf[o+1], buf[o+2], buf[o+3] = rgba
    return buf


def pset(buf, w, h, x, y, rgba):
    if 0 <= x < w and 0 <= y < h:
        o = (y*w + x) * 4
        a = rgba[3]
        if a >= 255:
            buf[o], buf[o+1], buf[o+2], buf[o+3] = rgba
        else:                                   # alpha blend over existing
            ia = 255 - a
            buf[o]   = (rgba[0]*a + buf[o]*ia)   // 255
            buf[o+1] = (rgba[1]*a + buf[o+1]*ia) // 255
            buf[o+2] = (rgba[2]*a + buf[o+2]*ia) // 255
            buf[o+3] = max(buf[o+3], a)


def rect(buf, w, h, x0, y0, x1, y1, rgba, fill=True):
    for y in range(max(0, y0), min(h, y1)):
        for x in range(max(0, x0), min(w, x1)):
            if fill or x == x0 or x == x1-1 or y == y0 or y == y1-1:
                pset(buf, w, h, x, y, rgba)


def disc(buf, w, h, cx, cy, r, rgba):
    for y in range(cy-r, cy+r+1):
        for x in range(cx-r, cx+r+1):
            if (x-cx)**2 + (y-cy)**2 <= r*r:
                pset(buf, w, h, x, y, rgba)


def vignette(buf, w, h, strength=0.35):
    """Darken toward the edges a touch so adjacent tiles read as a grid."""
    cx, cy = (w-1)/2.0, (h-1)/2.0
    maxd = math.hypot(cx, cy)
    for y in range(h):
        for x in range(w):
            d = math.hypot(x-cx, y-cy) / maxd
            f = 1.0 - strength * (d*d)
            o = (y*w + x) * 4
            buf[o]   = int(buf[o]   * f)
            buf[o+1] = int(buf[o+1] * f)
            buf[o+2] = int(buf[o+2] * f)


def write_png(path, w, h, buf):
    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)
        raw += buf[y*stride:(y+1)*stride]
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


# ----- the individual tiles -------------------------------------------------
def tile_floor(s):
    buf = blank(s, s, (*STONE_LIT, 255))
    # faint flagstone mortar lines (a cross through the tile)
    rect(buf, s, s, 0, s//2-1, s, s//2+1, (*MORTAR, 255))
    rect(buf, s, s, s//2-1, 0, s//2+1, s, (*MORTAR, 255))
    # a warm torch sheen in the middle
    disc(buf, s, s, s//2, s//2, s//6, (TORCH[0], TORCH[1], TORCH[2], 40))
    vignette(buf, s, s, 0.30)
    return buf


def tile_surface(s):
    buf = blank(s, s, (*DAYLIGHT, 255))
    # specks of ground
    for i in range(18):
        x = (i*37) % s; y = (i*53) % s
        pset(buf, s, s, x, y, (90, 100, 86, 255))
    vignette(buf, s, s, 0.18)
    return buf


def tile_wall(s):
    buf = blank(s, s, (*STONE_DARK, 255))
    # chiselled brick courses
    rect(buf, s, s, 0, s//3, s, s//3+2, (20, 18, 16, 255))
    rect(buf, s, s, 0, 2*s//3, s, 2*s//3+2, (20, 18, 16, 255))
    rect(buf, s, s, s//2-1, 0, s//2+1, s//3, (20, 18, 16, 255))
    rect(buf, s, s, s//4-1, s//3, s//4+1, 2*s//3, (20, 18, 16, 255))
    rect(buf, s, s, 3*s//4-1, 2*s//3, 3*s//4+1, s, (20, 18, 16, 255))
    rect(buf, s, s, 0, 0, s, s, (58, 52, 48, 255), fill=False)
    return buf


def tile_door(s):
    buf = tile_wall(s)
    rect(buf, s, s, s//4, s//6, 3*s//4, s, (*WOOD, 255))
    rect(buf, s, s, s//4, s//6, 3*s//4, s, (70, 46, 26, 255), fill=False)
    rect(buf, s, s, s//2-1, s//6, s//2+1, s, (70, 46, 26, 255))      # plank seam
    disc(buf, s, s, 3*s//4-5, s//2, 2, (*GOLD, 255))                 # handle
    return buf


def _chevrons(buf, s, down):
    rng = range(1, 4) if down else range(1, 4)
    for i in rng:
        y = (i * s // 4) if down else (s - i * s // 4)
        for dx in range(-s//4, s//4+1):
            yy = y + (abs(dx) if down else -abs(dx))
            rect(buf, s, s, s//2+dx, yy, s//2+dx+1, yy+2, (*TORCH, 255))


def tile_stair_down(s):
    buf = tile_floor(s)
    disc(buf, s, s, s//2, s//2, s//3, (12, 12, 14, 255))     # the dark mouth
    _chevrons(buf, s, True)
    return buf


def tile_stair_up(s):
    buf = tile_floor(s)
    rect(buf, s, s, s//4, s//4, 3*s//4, 3*s//4, (90, 80, 70, 255))
    _chevrons(buf, s, False)
    return buf


def tile_treasure(s):
    buf = tile_floor(s)
    disc(buf, s, s, s//2, 3*s//5, s//4, (*GOLD, 255))
    disc(buf, s, s, s//2-3, 3*s//5-3, s//10, (255, 240, 180, 255))   # glint
    return buf


def tile_dark(s):
    return blank(s, s, (*NEARBLACK, 255))


TILES = {
    "floor": tile_floor,
    "surface": tile_surface,
    "wall": tile_wall,
    "door": tile_door,
    "stair-down": tile_stair_down,
    "stair-up": tile_stair_up,
    "treasure": tile_treasure,
    "dark": tile_dark,
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="images")
    ap.add_argument("--size", type=int, default=64)
    args = ap.parse_args()
    outdir = os.path.join(args.out, "tiles")
    os.makedirs(outdir, exist_ok=True)
    s = args.size
    manifest = [f"# terrain tiles for the Moria sprite board", f"size\t{s}x{s}"]
    for name, fn in TILES.items():
        buf = fn(s)
        path = os.path.join(outdir, name + ".png")
        write_png(path, s, s, buf)
        manifest.append(f"tile\t{name}\t{name}.png\t{s}x{s}")
        print(f"wrote {path} ({s}x{s})")
    with open(os.path.join(outdir, "tiles.manifest"), "w") as f:
        f.write("\n".join(manifest) + "\n")
    print(f"wrote {outdir}/tiles.manifest")


if __name__ == "__main__":
    main()
