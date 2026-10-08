package main

import (
	"fmt"
	"image"
	"image/color"
	_ "image/jpeg"
	"image/png"
	"os"
	"path/filepath"
)

const thresh = 24 // luminance threshold for "content" vs black background

type box struct{ x0, y0, x1, y1 int }

func main() {
	if len(os.Args) < 3 {
		fmt.Fprintln(os.Stderr, "usage: crop <sheet.jpeg> <outDir> [namePrefix]")
		os.Exit(2)
	}
	src := os.Args[1]
	outDir := os.Args[2]
	// Name prefix for the emitted PNGs (<prefix>-NN.png). Defaults to the
	// output directory's parent name so e.g. .../moria-goblin/sprites -> "moria-goblin".
	prefix := ""
	if len(os.Args) > 3 {
		prefix = os.Args[3]
	} else {
		prefix = filepath.Base(filepath.Dir(outDir))
	}
	f, err := os.Open(src)
	must(err)
	defer f.Close()
	img, _, err := image.Decode(f)
	must(err)
	b := img.Bounds()
	w, h := b.Dx(), b.Dy()

	lum := func(x, y int) uint32 {
		r, g, bl, _ := img.At(b.Min.X+x, b.Min.Y+y).RGBA()
		r8, g8, b8 := r>>8, g>>8, bl>>8
		return (299*r8 + 587*g8 + 114*b8) / 1000
	}

	// column content counts
	colCount := make([]int, w)
	for x := 0; x < w; x++ {
		for y := 0; y < h; y++ {
			if lum(x, y) > thresh {
				colCount[x]++
			}
		}
	}

	// find column runs (sprite columns), ignoring tiny speckles (width>=3, count>=4 somewhere)
	var runs [][2]int
	inRun := false
	start := 0
	for x := 0; x <= w; x++ {
		content := x < w && colCount[x] > 0
		if content && !inRun {
			inRun, start = true, x
		} else if !content && inRun {
			inRun = false
			if x-start >= 3 {
				runs = append(runs, [2]int{start, x - 1})
			}
		}
	}

	// Separate touching sprites: when two figures abut with no empty column
	// between them, the gap-based scan above merges them into one oversized
	// run. Detect such runs (much wider than the median) and split each at its
	// lowest-content interior column (the valley between the two figures).
	// Repeated until no run is anomalously wide, so a 3-way merge also splits.
	medianRunWidth := func(rs [][2]int) int {
		ws := make([]int, 0, len(rs))
		for _, r := range rs {
			ws = append(ws, r[1]-r[0]+1)
		}
		for a := 0; a < len(ws); a++ {
			for b2 := a + 1; b2 < len(ws); b2++ {
				if ws[b2] < ws[a] {
					ws[a], ws[b2] = ws[b2], ws[a]
				}
			}
		}
		if len(ws) == 0 {
			return 0
		}
		return ws[len(ws)/2]
	}
	for {
		med := medianRunWidth(runs)
		if med == 0 {
			break
		}
		var next [][2]int
		changed := false
		for _, r := range runs {
			rw := r[1] - r[0] + 1
			// A run ~1.6x wider than the typical sprite is almost certainly
			// two figures touching.
			if rw >= med*8/5 {
				margin := rw / 4 // don't split near the run's own edges
				minx, min := -1, 1<<30
				for x := r[0] + margin; x <= r[1]-margin; x++ {
					if colCount[x] < min {
						min, minx = colCount[x], x
					}
				}
				if minx > r[0] && minx < r[1] {
					next = append(next, [2]int{r[0], minx - 1}, [2]int{minx, r[1]})
					changed = true
					continue
				}
			}
			next = append(next, r)
		}
		runs = next
		if !changed {
			break
		}
	}

	// For each column run, compute a tight bounding box (trim empty rows within the run).
	var boxes []box
	for _, r := range runs {
		x0, x1 := r[0], r[1]
		y0, y1 := -1, -1
		// also trim leading/trailing empty columns inside the run (there shouldn't be, but be safe)
		for y := 0; y < h; y++ {
			rowHas := false
			for x := x0; x <= x1; x++ {
				if lum(x, y) > thresh {
					rowHas = true
					break
				}
			}
			if rowHas {
				if y0 < 0 {
					y0 = y
				}
				y1 = y
			}
		}
		boxes = append(boxes, box{x0, y0, x1, y1})
	}

	// Determine uniform crop size = max width and max height across all boxes.
	maxW, maxH := 0, 0
	for _, bx := range boxes {
		if dw := bx.x1 - bx.x0 + 1; dw > maxW {
			maxW = dw
		}
		if dh := bx.y1 - bx.y0 + 1; dh > maxH {
			maxH = dh
		}
	}

	fmt.Printf("detected %d sprites; uniform crop = %dx%d\n", len(boxes), maxW, maxH)

	must(os.MkdirAll(outDir, 0o755))

	// Emit each sprite centered within a uniform maxW x maxH transparent canvas.
	for i, bx := range boxes {
		dst := image.NewRGBA(image.Rect(0, 0, maxW, maxH))
		// transparent by default
		sw := bx.x1 - bx.x0 + 1
		sh := bx.y1 - bx.y0 + 1
		// center horizontally; anchor to bottom vertically (characters stand on ground)
		offX := (maxW - sw) / 2
		offY := maxH - sh
		for y := 0; y < sh; y++ {
			for x := 0; x < sw; x++ {
				sx, sy := bx.x0+x, bx.y0+y
				r, g, bl, _ := img.At(b.Min.X+sx, b.Min.Y+sy).RGBA()
				r8, g8, b8 := uint8(r>>8), uint8(g>>8), uint8(bl>>8)
				l := (299*uint32(r8) + 587*uint32(g8) + 114*uint32(b8)) / 1000
				var a uint8 = 255
				if l <= thresh {
					a = 0 // make background black transparent
				}
				dst.Set(offX+x, offY+y, color.RGBA{r8, g8, b8, a})
			}
		}
		name := filepath.Join(outDir, fmt.Sprintf("%s-%02d.png", prefix, i+1))
		of, err := os.Create(name)
		must(err)
		must(png.Encode(of, dst))
		of.Close()
		fmt.Printf("sprite %2d: src box x=[%d..%d] y=[%d..%d] (%dx%d) -> %s\n",
			i+1, bx.x0, bx.x1, bx.y0, bx.y1, sw, sh, filepath.Base(name))
	}
}

func must(err error) {
	if err != nil {
		panic(err)
	}
}
