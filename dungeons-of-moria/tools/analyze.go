package main

import (
	"fmt"
	"image"
	_ "image/jpeg"
	"os"
)

// luminance threshold: pixels brighter than this count as "content"
const thresh = 24

func main() {
	f, err := os.Open(os.Args[1])
	if err != nil {
		panic(err)
	}
	defer f.Close()
	img, _, err := image.Decode(f)
	if err != nil {
		panic(err)
	}
	b := img.Bounds()
	w, h := b.Dx(), b.Dy()
	fmt.Printf("size: %dx%d\n", w, h)

	// per-column content pixel count
	colCount := make([]int, w)
	rowCount := make([]int, h)
	for y := 0; y < h; y++ {
		for x := 0; x < w; x++ {
			r, g, bl, _ := img.At(b.Min.X+x, b.Min.Y+y).RGBA()
			// 0..65535 -> 0..255
			r8, g8, b8 := r>>8, g>>8, bl>>8
			lum := (299*r8 + 587*g8 + 114*b8) / 1000
			if lum > thresh {
				colCount[x]++
				rowCount[y]++
			}
		}
	}

	// find column runs of content separated by empty gaps
	fmt.Println("--- column content runs (content width >= 3px) ---")
	inRun := false
	start := 0
	n := 0
	for x := 0; x <= w; x++ {
		content := x < w && colCount[x] > 0
		if content && !inRun {
			inRun = true
			start = x
		} else if !content && inRun {
			inRun = false
			if x-start >= 3 {
				n++
				fmt.Printf("run %2d: x=[%d..%d] width=%d\n", n, start, x-1, x-start)
			}
		}
	}

	// vertical content extent
	top, bot := -1, -1
	for y := 0; y < h; y++ {
		if rowCount[y] > 0 {
			if top < 0 {
				top = y
			}
			bot = y
		}
	}
	fmt.Printf("vertical content extent: y=[%d..%d] height=%d\n", top, bot, bot-top+1)
}
