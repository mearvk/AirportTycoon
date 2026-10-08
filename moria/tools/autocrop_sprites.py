#!/usr/bin/env python3
"""
Moria — sprite autocropper (pure Python standard library, no dependencies).

Why pure stdlib: the build sandbox has no Pillow / ImageMagick and no network,
so this tool carries its own minimal **baseline JPEG decoder** (SOF0, Huffman,
8x8 IDCT, YCbCr->RGB) and a PNG writer (zlib is stdlib). That is enough to:

  1. decode a baseline .jpeg sprite sheet to RGB,
  2. AUTOCROP it to the figure's tight bounding box (trim uniform background
     margins, measured from the sheet's corner colour),
  3. slice the cropped figure strip into the animation sample frames
     START / MID / STOP (the three poses the game animates a walk from),
  4. write each as a .png under  images/<character>/  named
     <character>-<start|mid|stop>.png,
  5. and write a tiny sprite.manifest the SLeeLa UI can read for frame sizes.

Usage:
  python3 tools/autocrop_sprites.py SHEET.jpeg CHARACTER [--frames N] [--out images]

If a real image library ever becomes available it is preferred automatically
(Pillow), but the stdlib path is always present so the tool runs offline.

This decoder targets BASELINE jpegs (the sheets in moria/images are SOF0); it
is intentionally small and readable, not a general-purpose codec.
"""
import sys, os, struct, zlib, argparse

# --------------------------------------------------------------------------
# Optional fast path: use Pillow if it is importable (never required).
# --------------------------------------------------------------------------
def _try_pillow_load(path):
    try:
        from PIL import Image
    except Exception:
        return None
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = list(im.getdata())
    flat = bytearray(w * h * 3)
    i = 0
    for (r, g, b) in px:
        flat[i] = r; flat[i+1] = g; flat[i+2] = b; i += 3
    return (w, h, flat)


# ==========================================================================
# Minimal baseline-JPEG decoder (SOF0). Returns (width, height, rgb-bytes).
# ==========================================================================
ZIGZAG = [
    0, 1, 8,16, 9, 2, 3,10,
   17,24,32,25,18,11, 4, 5,
   12,19,26,33,40,48,41,34,
   27,20,13, 6, 7,14,21,28,
   35,42,49,56,57,50,43,36,
   29,22,15,23,30,37,44,51,
   58,59,52,45,38,31,39,46,
   53,60,61,54,47,55,62,63,
]

class _BitReader:
    def __init__(self, data):
        self.data = data; self.pos = 0; self.bits = 0; self.nbits = 0
    def _next_byte(self):
        if self.pos >= len(self.data):
            return 0
        b = self.data[self.pos]; self.pos += 1
        if b == 0xFF:
            # skip stuffed zero byte; markers inside scan are handled by caller
            if self.pos < len(self.data) and self.data[self.pos] == 0x00:
                self.pos += 1
        return b
    def bit(self):
        if self.nbits == 0:
            self.bits = self._next_byte(); self.nbits = 8
        self.nbits -= 1
        return (self.bits >> self.nbits) & 1
    def receive(self, n):
        v = 0
        for _ in range(n):
            v = (v << 1) | self.bit()
        return v
    def extend(self, v, n):
        if n == 0: return 0
        if v < (1 << (n - 1)):
            v += (-1 << n) + 1
        return v
    def reset(self):
        self.bits = 0; self.nbits = 0


def _build_huff(counts, symbols):
    # counts: 16 entries (lengths 1..16). Return {(length,code): symbol}.
    table = {}
    code = 0; k = 0
    for length in range(1, 17):
        for _ in range(counts[length - 1]):
            table[(length, code)] = symbols[k]; k += 1; code += 1
        code <<= 1
    return table


def _huff_decode(br, table):
    length = 0; code = 0
    while length < 16:
        code = (code << 1) | br.bit(); length += 1
        s = table.get((length, code))
        if s is not None:
            return s
    raise ValueError("bad huffman code")


def _idct_8x8(block):
    # Straightforward separable IDCT (float). Readable over fast.
    import math
    tmp = [0.0] * 64
    c = [1.0] * 8; c[0] = 1.0 / math.sqrt(2)
    cos = [[math.cos((2*x+1)*u*math.pi/16) for u in range(8)] for x in range(8)]
    for y in range(8):
        for x in range(8):
            s = 0.0
            for v in range(8):
                for u in range(8):
                    s += c[u]*c[v]*block[v*8+u]*cos[x][u]*cos[y][v]
            tmp[y*8+x] = s/4.0
    return tmp


def decode_jpeg(path):
    fast = _try_pillow_load(path)
    if fast is not None:
        return fast
    with open(path, "rb") as f:
        data = f.read()
    if data[:2] != b"\xff\xd8":
        raise ValueError("not a JPEG")
    qt = {}; huff_dc = {}; huff_ac = {}
    comps = []; width = height = 0; restart = 0
    i = 2
    while i < len(data):
        if data[i] != 0xFF:
            i += 1; continue
        marker = data[i+1]; i += 2
        if marker == 0xD9: break
        if marker in (0x01,) or 0xD0 <= marker <= 0xD7:
            continue
        seglen = struct.unpack(">H", data[i:i+2])[0]
        seg = data[i+2:i+seglen]; nexti = i + seglen
        if marker == 0xDB:  # DQT
            p = 0
            while p < len(seg):
                pq_tq = seg[p]; p += 1; tq = pq_tq & 15; prec = pq_tq >> 4
                tbl = []
                for _ in range(64):
                    if prec:
                        tbl.append(struct.unpack(">H", seg[p:p+2])[0]); p += 2
                    else:
                        tbl.append(seg[p]); p += 1
                qt[tq] = tbl
        elif marker in (0xC0, 0xC1):  # SOF0/1 baseline
            height = struct.unpack(">H", seg[1:3])[0]
            width = struct.unpack(">H", seg[3:5])[0]
            n = seg[5]; p = 6
            for _ in range(n):
                cid = seg[p]; hv = seg[p+1]; tq = seg[p+2]; p += 3
                comps.append({"id":cid,"h":hv>>4,"v":hv&15,"tq":tq})
        elif marker == 0xC2:
            raise ValueError("progressive JPEG not supported by the stdlib path")
        elif marker == 0xC4:  # DHT
            p = 0
            while p < len(seg):
                tc_th = seg[p]; p += 1; tc = tc_th >> 4; th = tc_th & 15
                counts = list(seg[p:p+16]); p += 16
                total = sum(counts)
                symbols = list(seg[p:p+total]); p += total
                tbl = _build_huff(counts, symbols)
                (huff_dc if tc == 0 else huff_ac)[th] = tbl
        elif marker == 0xDD:  # DRI
            restart = struct.unpack(">H", seg[0:2])[0]
        elif marker == 0xDA:  # SOS -> entropy-coded scan to end (minus trailer)
            ns = seg[0]; p = 1; scan = []
            for _ in range(ns):
                cs = seg[p]; td_ta = seg[p+1]; p += 2
                for c in comps:
                    if c["id"] == cs:
                        c["td"] = td_ta >> 4; c["ta"] = td_ta & 15
                        scan.append(c)
            ecs = data[i+seglen:]
            # strip final EOI
            end = ecs.find(b"\xff\xd9")
            if end >= 0: ecs = ecs[:end]
            return _decode_scan(width, height, comps, scan, qt,
                                huff_dc, huff_ac, restart, ecs)
        i = nexti
    raise ValueError("no scan found")


def _decode_scan(width, height, comps, scan, qt, huff_dc, huff_ac, restart, ecs):
    hmax = max(c["h"] for c in comps); vmax = max(c["v"] for c in comps)
    mcux = (width + 8*hmax - 1) // (8*hmax)
    mcuy = (height + 8*vmax - 1) // (8*vmax)
    planes = {}
    for c in comps:
        pw = mcux * c["h"] * 8; ph = mcuy * c["v"] * 8
        planes[c["id"]] = {"w":pw,"h":ph,"px":[0]*(pw*ph)}
    br = _BitReader(ecs)
    pred = {c["id"]: 0 for c in comps}
    cnt = 0
    for my in range(mcuy):
        for mx in range(mcux):
            for c in scan:
                for by in range(c["v"]):
                    for bx in range(c["h"]):
                        block = _decode_block(br, c, pred, qt, huff_dc, huff_ac)
                        _place(planes[c["id"]], block, mx*c["h"]+bx, my*c["v"]+by)
            cnt += 1
            if restart and cnt % restart == 0 and (my*mcux+mx) != mcux*mcuy-1:
                br.reset()
                for k in pred: pred[k] = 0
    return _to_rgb(width, height, comps, planes, hmax, vmax)


def _decode_block(br, c, pred, qt, huff_dc, huff_ac):
    q = qt[c["tq"]]
    coef = [0]*64
    t = _huff_decode(br, huff_dc[c["td"]])
    diff = br.extend(br.receive(t), t) if t else 0
    pred[c["id"]] += diff
    coef[0] = pred[c["id"]] * q[0]
    k = 1
    ac = huff_ac[c["ta"]]
    while k < 64:
        rs = _huff_decode(br, ac); r = rs >> 4; s = rs & 15
        if s == 0:
            if r == 15: k += 16; continue
            break
        k += r
        if k >= 64: break
        val = br.extend(br.receive(s), s)
        coef[ZIGZAG[k]] = val * q[k]
        k += 1
    px = _idct_8x8(coef)
    return [max(0, min(255, int(round(v)) + 128)) for v in px]


def _place(plane, block, bx, by):
    pw = plane["w"]; px = plane["px"]
    for yy in range(8):
        row = (by*8+yy)*pw + bx*8
        for xx in range(8):
            px[row+xx] = block[yy*8+xx]


def _to_rgb(width, height, comps, planes, hmax, vmax):
    out = bytearray(width*height*3)
    cmap = {c["id"]: c for c in comps}
    yc = comps[0]["id"]
    cbc = comps[1]["id"] if len(comps) > 1 else None
    crc = comps[2]["id"] if len(comps) > 2 else None
    for y in range(height):
        for x in range(width):
            def samp(cid):
                c = cmap[cid]; pl = planes[cid]
                sx = x * c["h"] // hmax; sy = y * c["v"] // vmax
                return pl["px"][sy*pl["w"]+sx]
            Y = samp(yc)
            if cbc is None:
                r = g = b = Y
            else:
                Cb = samp(cbc) - 128; Cr = samp(crc) - 128
                r = Y + 1.402*Cr; g = Y - 0.344136*Cb - 0.714136*Cr; b = Y + 1.772*Cb
            o = (y*width+x)*3
            out[o]   = max(0,min(255,int(r)))
            out[o+1] = max(0,min(255,int(g)))
            out[o+2] = max(0,min(255,int(b)))
    return (width, height, out)


# ==========================================================================
# PNG writer (stdlib zlib).
# ==========================================================================
def write_png(path, w, h, rgb):
    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter: none
        raw += rgb[y*w*3:(y+1)*w*3]
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


# ==========================================================================
# Autocrop + frame slicing.
# ==========================================================================
def autocrop(w, h, rgb, tol=18):
    # Background colour = the sheet's top-left corner.
    br, bg, bb = rgb[0], rgb[1], rgb[2]
    def is_bg(x, y):
        o = (y*w+x)*3
        return (abs(rgb[o]-br) <= tol and abs(rgb[o+1]-bg) <= tol and abs(rgb[o+2]-bb) <= tol)
    x0, y0, x1, y1 = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            if not is_bg(x, y):
                if x < x0: x0 = x
                if y < y0: y0 = y
                if x > x1: x1 = x
                if y > y1: y1 = y
    if x1 < x0 or y1 < y0:
        return (0, 0, w, h)   # all background; keep whole
    return (x0, y0, x1+1, y1+1)


def crop(w, h, rgb, box):
    x0, y0, x1, y1 = box; cw, ch = x1-x0, y1-y0
    out = bytearray(cw*ch*3)
    for y in range(ch):
        src = ((y0+y)*w + x0)*3
        out[y*cw*3:(y+1)*cw*3] = rgb[src:src+cw*3]
    return (cw, ch, out)


def slice_frames(w, h, rgb, n):
    # Split a horizontal strip into n equal frames (left->right).
    fw = w // n
    frames = []
    for i in range(n):
        x0 = i*fw; x1 = w if i == n-1 else (i+1)*fw
        frames.append(crop(w, h, rgb, (x0, 0, x1, h)))
    return frames


def flip_h(w, h, rgb):
    """Horizontal mirror of an RGB frame — turns a right-facing pose into the
    left-facing one (and vice versa)."""
    out = bytearray(w*h*3)
    for y in range(h):
        row = y*w*3
        for x in range(w):
            s = row + x*3
            d = row + (w-1-x)*3
            out[d] = rgb[s]; out[d+1] = rgb[s+1]; out[d+2] = rgb[s+2]
    return (w, h, out)


SAMPLE = ["start", "mid", "stop"]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("sheet")
    ap.add_argument("character")
    ap.add_argument("--frames", type=int, default=3)
    ap.add_argument("--out", default="images")
    ap.add_argument("--directions", action="store_true",
                    help="also emit left/right facings (right = sheet orientation, "
                         "left = horizontal mirror), named <char>-<pose>-<dir>.png")
    args = ap.parse_args()

    w, h, rgb = decode_jpeg(args.sheet)
    box = autocrop(w, h, rgb)
    cw, ch, crgb = crop(w, h, rgb, box)
    frames = slice_frames(cw, ch, crgb, args.frames)

    outdir = os.path.join(args.out, args.character)
    os.makedirs(outdir, exist_ok=True)
    manifest = [f"# sprite manifest for {args.character}",
                f"sheet\t{os.path.basename(args.sheet)}\t{w}x{h}",
                f"autocrop\t{box[0]},{box[1]},{box[2]},{box[3]}\t{cw}x{ch}"]
    labels = SAMPLE if args.frames == 3 else [f"frame{i:02d}" for i in range(args.frames)]
    for i, (fw2, fh2, fb) in enumerate(frames):
        label = labels[i] if i < len(labels) else f"frame{i:02d}"
        # Base (facing-neutral) frame, kept for backward compatibility.
        name = f"{args.character}-{label}.png"
        write_png(os.path.join(outdir, name), fw2, fh2, fb)
        manifest.append(f"frame\t{label}\t{name}\t{fw2}x{fh2}")
        print(f"wrote {outdir}/{name}  ({fw2}x{fh2})")
        if args.directions:
            # right = the sheet's own orientation; left = its horizontal mirror.
            rname = f"{args.character}-{label}-right.png"
            write_png(os.path.join(outdir, rname), fw2, fh2, fb)
            manifest.append(f"frame\t{label}\tright\t{rname}\t{fw2}x{fh2}")
            print(f"wrote {outdir}/{rname}  ({fw2}x{fh2})  [right]")
            lw, lh, lb = flip_h(fw2, fh2, fb)
            lname = f"{args.character}-{label}-left.png"
            write_png(os.path.join(outdir, lname), lw, lh, lb)
            manifest.append(f"frame\t{label}\tleft\t{lname}\t{lw}x{lh}")
            print(f"wrote {outdir}/{lname}  ({lw}x{lh})  [left, mirrored]")
    with open(os.path.join(outdir, "sprite.manifest"), "w") as f:
        f.write("\n".join(manifest) + "\n")
    print(f"wrote {outdir}/sprite.manifest")

if __name__ == "__main__":
    main()
