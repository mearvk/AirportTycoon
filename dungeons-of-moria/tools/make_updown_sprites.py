#!/usr/bin/env python3
"""
Moria — up/down sprite completer (pure Python standard library, no deps).

The two autocropped heroes (images/adventurer/ and images/warden/) were sliced
by tools/autocrop_sprites.py into only the LEFT / RIGHT / base facings — enough
to walk east and west, but not the TOP (north) and DOWN (south) poses the sprite
GUI needs so the figure moves in ALL FOUR directions on screen.

Rather than drop in a flat placeholder (make_sprite_set.py would overwrite the
real art), this tool DERIVES the missing `-top` and `-down` directional frames
from the character's own REAL cropped frames, so the up/down poses look like the
actual character:

  * `-down`  (facing the viewer, walking south): the real base/right frame,
             with a small downward facing chip composited in a corner so the
             heading reads at a glance.
  * `-top`   (back to the viewer, walking north): the real base/left frame,
             mildly darkened (the figure is seen from behind, away from the
             torch) with an upward facing chip.

For each of start / mid / end(=stop) it writes:
      images/<char>/<char>-<frame>-top.png
      images/<char>/<char>-<frame>-down.png
and the engine "stop" aliases where the designer frame is "end". It also fills
the nested images/<char>/<dir>/<action>/ tree for top & down (byte-identical
copies) so BOTH layouts resolve, matching the cast characters.

It NEVER touches the existing real left/right/base PNGs. Pure stdlib: it reads
the real PNGs with its own minimal PNG reader and writes new PNGs with zlib.

Usage:
  python3 tools/make_updown_sprites.py CHARACTER [--out images]
  python3 tools/make_updown_sprites.py adventurer warden   # several at once
"""
import os, sys, struct, zlib, argparse

# The autocropped heroes (adventurer, warden) label the planted pose "stop";
# the designer/cast layout calls the same pose "end". We read whichever the
# real crop provides and write BOTH names so the 2-facing walk code (framePath,
# which asks for "stop") and the 4-way code (framePath4, which asks for "end")
# both resolve to the derived up/down pose.
FRAMES = ["start", "mid", "stop"]         # the real cropped frame labels
FRAME_ALIASES = {"stop": "end"}           # also write the 'end' designer alias
NEW_DIRS = ["top", "down"]


# --------------------------------------------------------------------------
# Minimal PNG reader (8-bit truecolour, with or without alpha). Returns
# (width, height, channels, bytearray) where channels is 3 (RGB) or 4 (RGBA).
# Supports the two colour types the project's PNG writers emit (2 and 6) with
# no interlacing — which is exactly what autocrop_sprites.py / make_sprite_set
# produce.
# --------------------------------------------------------------------------
def read_png(path):
    with open(path, "rb") as f:
        data = f.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path}: not a PNG")
    pos = 8
    w = h = bit_depth = color_type = None
    idat = bytearray()
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos+4])
        tag = data[pos+4:pos+8]
        body = data[pos+8:pos+8+length]
        pos += 12 + length
        if tag == b"IHDR":
            w, h, bit_depth, color_type, comp, filt, interlace = struct.unpack(">IIBBBBB", body)
            if bit_depth != 8 or interlace != 0 or color_type not in (2, 6):
                raise ValueError(f"{path}: unsupported PNG (depth={bit_depth}, "
                                 f"color={color_type}, interlace={interlace})")
        elif tag == b"IDAT":
            idat += body
        elif tag == b"IEND":
            break
    channels = 4 if color_type == 6 else 3
    raw = zlib.decompress(bytes(idat))
    stride = w * channels
    out = bytearray(w * h * channels)
    prev = bytearray(stride)
    rp = 0
    for y in range(h):
        ft = raw[rp]; rp += 1
        line = bytearray(raw[rp:rp+stride]); rp += stride
        # Undo the PNG row filter.
        if ft == 1:      # Sub
            for i in range(channels, stride):
                line[i] = (line[i] + line[i-channels]) & 255
        elif ft == 2:    # Up
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif ft == 3:    # Average
            for i in range(stride):
                a = line[i-channels] if i >= channels else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif ft == 4:    # Paeth
            for i in range(stride):
                a = line[i-channels] if i >= channels else 0
                b = prev[i]
                c = prev[i-channels] if i >= channels else 0
                p = a + b - c
                pa = abs(p-a); pb = abs(p-b); pc = abs(p-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y*stride:(y+1)*stride] = line
        prev = line
    return (w, h, channels, out)


def write_png(path, w, h, channels, buf):
    color_type = 6 if channels == 4 else 2
    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)
    stride = w * channels
    raw = bytearray()
    for y in range(h):
        raw.append(0)                       # filter type 0 (None)
        raw += buf[y*stride:(y+1)*stride]
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, color_type, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


# --------------------------------------------------------------------------
# Pixel helpers over a (w,h,channels,buf) image.
# --------------------------------------------------------------------------
def _px_set(buf, w, channels, x, y, rgb):
    o = (y*w + x)*channels
    buf[o] = rgb[0]; buf[o+1] = rgb[1]; buf[o+2] = rgb[2]
    if channels == 4:
        buf[o+3] = 255

def _darken(buf, channels, factor):
    """Scale RGB toward black by `factor` (0..1), leaving alpha intact."""
    n = len(buf)
    i = 0
    while i < n:
        buf[i]   = int(buf[i]   * factor)
        buf[i+1] = int(buf[i+1] * factor)
        buf[i+2] = int(buf[i+2] * factor)
        i += channels

def _chip(buf, w, h, channels, direction):
    """Composite a small facing arrow (top=blue up, down=amber down) into the
    top-left corner so the derived heading reads at a glance over the real art."""
    pad = max(4, w // 40)
    size = max(6, w // 18)
    cx = pad + size
    cy = pad + size
    col = (90, 150, 235) if direction == "top" else (235, 165, 70)
    for dy in range(-size, size+1):
        for dx in range(-size, size+1):
            if direction == "top":
                inside = (dy <= 0) and (abs(dx) <= (size + dy))
            else:
                inside = (dy >= 0) and (abs(dx) <= (size - dy))
            if inside:
                x = cx + dx; y = cy + dy
                if 0 <= x < w and 0 <= y < h:
                    _px_set(buf, w, channels, x, y, col)


def _load_source(indir, character, frame, facing):
    """Load a real cropped source frame, trying facing then base."""
    for name in (f"{character}-{frame}-{facing}.png", f"{character}-{frame}.png"):
        p = os.path.join(indir, name)
        if os.path.exists(p):
            return p, read_png(p)
    raise FileNotFoundError(
        f"no real source frame for {character} {frame} (need "
        f"{character}-{frame}-{facing}.png or {character}-{frame}.png in {indir})")


def _alias_end_of_stop(indir, character, written):
    """The 4-way engine accessor framePath4() names the planted pose 'end'
    (start/mid/END), but the autocropped heroes name it 'stop'. Mirror the
    existing LEFT / RIGHT / base 'stop' crops to 'end' names (byte-identical)
    so framePath4() resolves the planted left/right/base poses too — completing
    the hero's four-direction matrix under BOTH labels."""
    for facing in ("left", "right", None):
        suffix = f"-{facing}" if facing else ""
        src = os.path.join(indir, f"{character}-stop{suffix}.png")
        if not os.path.exists(src):
            continue
        w, h, ch, buf = read_png(src)
        dst_name = f"{character}-end{suffix}.png"
        write_png(os.path.join(indir, dst_name), w, h, ch, buf)
        written.append(dst_name)


def derive(character, outroot):
    indir = os.path.join(outroot, character)
    if not os.path.isdir(indir):
        raise FileNotFoundError(f"no sprite folder: {indir}")

    written = []
    # First, make the planted-pose 'end' aliases for the existing L/R/base crops.
    _alias_end_of_stop(indir, character, written)
    for frame in FRAMES:
        alias = FRAME_ALIASES.get(frame)        # stop -> end (designer name)
        for direction in NEW_DIRS:
            # down derives from the right/base pose; top from the left/base
            # pose (seen from behind), lightly darkened.
            facing = "right" if direction == "down" else "left"
            _src, (w, h, ch, buf) = _load_source(indir, character, frame, facing)
            img = bytearray(buf)                 # copy so we never alter source
            if direction == "top":
                _darken(img, ch, 0.78)           # the back, away from the torch
            _chip(img, w, h, ch, direction)

            out_name = f"{character}-{frame}-{direction}.png"
            write_png(os.path.join(indir, out_name), w, h, ch, img)
            written.append(out_name)

            # designer 'end' alias of the 'stop' pose (4-way framePath4 asks
            # for <char>-end-<dir>.png)
            if alias:
                alias_name = f"{character}-{alias}-{direction}.png"
                write_png(os.path.join(indir, alias_name), w, h, ch, img)
                written.append(alias_name)

            # nested <direction>/<action>/ tree (byte-identical copies), under
            # BOTH the real label and the designer alias so either resolves.
            for action in ([frame] + ([alias] if alias else [])):
                action_dir = os.path.join(indir, direction, action)
                os.makedirs(action_dir, exist_ok=True)
                write_png(os.path.join(action_dir, f"{character}-{direction}-{action}.png"),
                          w, h, ch, img)

    # Record what we added in a side manifest (do not clobber sprite.manifest).
    man = os.path.join(indir, "updown.manifest")
    with open(man, "w") as f:
        f.write(f"# derived up/down directional frames for {character}\n")
        f.write(f"# source: real {character}-<frame>-<left|right>.png crops\n")
        f.write(f"# top   = left pose, darkened (seen from behind) + up chip\n")
        f.write(f"# down  = right pose + down chip\n")
        for name in written:
            f.write(f"derived\t{name}\n")
    written.append("updown.manifest")
    return written


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("characters", nargs="+")
    ap.add_argument("--out", default="images")
    args = ap.parse_args()
    for character in args.characters:
        print(f"== {character} ==")
        for name in derive(character, args.out):
            print(f"  wrote images/{character}/{name}")


if __name__ == "__main__":
    main()
