package main

import (
	"fmt"
	"image"
	_ "image/jpeg"
	"os"
	"strconv"
)

const thresh = 24

func main() {
	f, _ := os.Open(os.Args[1])
	defer f.Close()
	img, _, err := image.Decode(f)
	if err != nil {
		panic(err)
	}
	b := img.Bounds()
	w, h := b.Dx(), b.Dy()
	x0, _ := strconv.Atoi(os.Args[2])
	x1, _ := strconv.Atoi(os.Args[3])
	for x := x0; x <= x1 && x < w; x++ {
		c := 0
		for y := 0; y < h; y++ {
			r, g, bl, _ := img.At(b.Min.X+x, b.Min.Y+y).RGBA()
			lum := (299*(r>>8) + 587*(g>>8) + 114*(bl>>8)) / 1000
			if lum > thresh {
				c++
			}
		}
		fmt.Printf("x=%d count=%d\n", x, c)
	}
}
