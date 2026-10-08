package main

import (
	"fmt"
	"image"
	"image/color"
	_ "image/jpeg"
	"image/png"
	"os"
	"path/filepath"
	"sort"
	"strconv"
)

const thresh = 24

type box struct{ x0, y0, x1, y1 int }

// usage: crop2 <src.jpg> <outDir> <prefix> <canvasW> <canvasH>
func main() {
	src := os.Args[1]
	outDir := os.Args[2]
	prefix := os.Args[3]
	canvasW, _ := strconv.Atoi(os.Args[4])
	canvasH, _ := strconv.Atoi(os.Args[5])

	f, err := os.Open(src)
	must(err)
	defer f.Close()
	img, _, err := image.Decode(f)
	must(err)
	b := img.Bounds()
	w, h := b.Dx(), b.Dy()

	lum := func(x, y int) uint32 {
		r, g, bl, _ := img.At(b.Min.X+x, b.Min.Y+y).RGBA()
		return (299*(r>>8) + 587*(g>>8) + 114*(bl>>8)) / 1000
	}

	colCount := make([]int, w)
	for x := 0; x < w; x++ {
		for y := 0; y < h; y++ {
			if lum(x, y) > thresh {
				colCount[x]++
			}
		}
	}

	// initial runs separated by all-black columns
	type run struct{ a, b int }
	var runs []run
	inRun := false
	start := 0
	for x := 0; x <= w; x++ {
		content := x < w && colCount[x] > 0
		if content && !inRun {
			inRun, start = true, x
		} else if !content && inRun {
			inRun = false
			if x-start >= 3 {
				runs = append(runs, run{start, x - 1})
			}
		}
	}

	// median run width
	widths := make([]int, len(runs))
	for i, r := range runs {
		widths[i] = r.b - r.a + 1
	}
	sort.Ints(widths)
	median := widths[len(widths)/2]

	// split any run wider than 1.6*median by recursively cutting at the
	// lowest-content column within the central 40% band of the run.
	var finalRuns []run
	var splitRun func(r run)
	splitRun = func(r run) {
		width := r.b - r.a + 1
		if width <= int(1.6*float64(median)) {
			finalRuns = append(finalRuns, r)
			return
		}
		// search central band [a+0.3w, b-0.3w] for min column count
		lo := r.a + int(0.30*float64(width))
		hi := r.b - int(0.30*float64(width))
		cut := lo
		min := colCount[lo]
		for x := lo; x <= hi; x++ {
			if colCount[x] < min {
				min = colCount[x]
				cut = x
			}
		}
		splitRun(run{r.a, cut})
		splitRun(run{cut + 1, r.b})
	}
	for _, r := range runs {
		splitRun(r)
	}

	// tight box per final run (trim empty rows)
	var boxes []box
	for _, r := range finalRuns {
		x0, x1 := r.a, r.b
		// trim empty leading/trailing cols
		for x0 <= x1 && colCount[x0] == 0 {
			x0++
		}
		for x1 >= x0 && colCount[x1] == 0 {
			x1--
		}
		y0, y1 := -1, -1
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

	// report + check fit
	maxW, maxH := 0, 0
	for _, bx := range boxes {
		if dw := bx.x1 - bx.x0 + 1; dw > maxW {
			maxW = dw
		}
		if dh := bx.y1 - bx.y0 + 1; dh > maxH {
			maxH = dh
		}
	}
	fmt.Printf("median run width=%d; detected %d sprites; max tight box=%dx%d; canvas=%dx%d\n",
		median, len(boxes), maxW, maxH, canvasW, canvasH)
	if maxW > canvasW || maxH > canvasH {
		fmt.Printf("WARNING: a sprite (%dx%d) exceeds canvas (%dx%d); it will be clipped\n", maxW, maxH, canvasW, canvasH)
	}

	must(os.MkdirAll(outDir, 0o755))
	for i, bx := range boxes {
		dst := image.NewRGBA(image.Rect(0, 0, canvasW, canvasH))
		sw := bx.x1 - bx.x0 + 1
		sh := bx.y1 - bx.y0 + 1
		offX := (canvasW - sw) / 2
		offY := canvasH - sh // bottom-anchor
		for y := 0; y < sh; y++ {
			for x := 0; x < sw; x++ {
				dx, dy := offX+x, offY+y
				if dx < 0 || dy < 0 || dx >= canvasW || dy >= canvasH {
					continue
				}
				sx, sy := bx.x0+x, bx.y0+y
				r, g, bl, _ := img.At(b.Min.X+sx, b.Min.Y+sy).RGBA()
				r8, g8, b8 := uint8(r>>8), uint8(g>>8), uint8(bl>>8)
				l := (299*uint32(r8) + 587*uint32(g8) + 114*uint32(b8)) / 1000
				var a uint8 = 255
				if l <= thresh {
					a = 0
				}
				dst.Set(dx, dy, color.RGBA{r8, g8, b8, a})
			}
		}
		name := filepath.Join(outDir, fmt.Sprintf("%s-%02d.png", prefix, i+1))
		of, err := os.Create(name)
		must(err)
		must(png.Encode(of, dst))
		of.Close()
		fmt.Printf("sprite %2d: x=[%d..%d] y=[%d..%d] (%dx%d) -> %s\n",
			i+1, bx.x0, bx.x1, bx.y0, bx.y1, sw, sh, filepath.Base(name))
	}
}

func must(err error) {
	if err != nil {
		panic(err)
	}
}
