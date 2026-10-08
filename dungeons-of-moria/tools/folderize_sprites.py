#!/usr/bin/env python3
"""
Moria — sprite folderizer (pure Python standard library, no dependencies).

Builds the NESTED directional/action folder tree for a character's sprites,
alongside the flat files the animation engine already loads:

    images/<character>/
        <direction>/                  top, left, down, right
            <action>/                 start, mid, end
                <character>-<direction>-<action>.png

It does NOT re-render anything: it copies the exact pixels of the flat source
sprites (<character>-<action>-<direction>.png, with 'end' sourced from either
'end' or the engine alias 'stop') into their nested home, so the two layouts
are byte-for-byte the same image. A per-direction manifest records each slot.

Usage:
  python3 tools/folderize_sprites.py CHARACTER [--out images]
"""
import os, argparse, shutil

DIRECTIONS = ["top", "left", "down", "right"]   # the four facing folders
ACTIONS    = ["start", "mid", "end"]            # the action folders in each

def flat_source(outdir, character, action, direction):
    """The flat source file for (action, direction), honouring the 'end'/'stop'
    alias. Returns an existing path or None."""
    candidates = []
    candidates.append(f"{character}-{action}-{direction}.png")
    if action == "end":
        candidates.append(f"{character}-stop-{direction}.png")
    if action == "stop":
        candidates.append(f"{character}-end-{direction}.png")
    for name in candidates:
        p = os.path.join(outdir, name)
        if os.path.isfile(p):
            return p
    return None

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("character")
    ap.add_argument("--out", default="images")
    args = ap.parse_args()

    base = os.path.join(args.out, args.character)
    if not os.path.isdir(base):
        raise SystemExit(f"no sprite folder at {base} — run make_sprite_set.py first")

    index = [f"# nested sprite tree for {args.character}",
             f"# layout: <direction>/<action>/{args.character}-<direction>-<action>.png",
             f"# directions: {', '.join(DIRECTIONS)}",
             f"# actions: {', '.join(ACTIONS)}"]

    made = 0
    for d in DIRECTIONS:
        ddir = os.path.join(base, d)
        os.makedirs(ddir, exist_ok=True)
        dir_manifest = [f"# {args.character} — facing {d}"]
        for a in ACTIONS:
            adir = os.path.join(ddir, a)
            os.makedirs(adir, exist_ok=True)
            src = flat_source(base, args.character, a, d)
            dest_name = f"{args.character}-{d}-{a}.png"
            dest = os.path.join(adir, dest_name)
            if src is None:
                raise SystemExit(f"missing flat source for {d}/{a} "
                                 f"(looked for {args.character}-{a}-{d}.png)")
            shutil.copyfile(src, dest)
            rel = os.path.relpath(dest, base)
            dir_manifest.append(f"action\t{a}\t{rel}")
            index.append(f"sprite\t{d}\t{a}\t{rel}")
            made += 1
            print(f"placed {dest}  (from {os.path.basename(src)})")
        with open(os.path.join(ddir, "sprite.manifest"), "w") as f:
            f.write("\n".join(dir_manifest) + "\n")
        print(f"wrote {ddir}/sprite.manifest")

    with open(os.path.join(base, "folders.manifest"), "w") as f:
        f.write("\n".join(index) + "\n")
    print(f"wrote {base}/folders.manifest  ({made} sprites placed)")

if __name__ == "__main__":
    main()
