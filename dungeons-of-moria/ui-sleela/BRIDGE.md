<p align="right"><img src="../../images/debian-vs-ubuntu.jpeg" alt="Debian vs Ubuntu" width="180"></p>

# Moria UI — SLVM bridge built-ins

The Moria front-end uses the vendored SleelaUI toolkit (`../../1/sources/user-interface`,
byte-for-byte upstream) plus a few **presentation-only helpers in this folder**.
Those helpers reach the native toolkit through the same `ui*` SLVM bridge the
rest of SleelaUI uses. Most of the built-ins they call already exist in the
toolkit (`uiImage`, `uiSeparator`, `uiSpinner`, `uiAppPump`, `uiWindowRedraw`,
…). This file lists the **new** bridge built-ins the Moria helpers introduce, so
a native SleelaUI build can implement them. Every call site degrades gracefully
when a built-in is absent (negative handle / no-op), so the game still reads.

| Built-in | Used by | Signature (SLVM) | C ABI it should call | Behaviour |
|---|---|---|---|---|
| `uiImageFile` | `SLImageFile` | `uiImageFile(parentHandle, path, maxW, maxH) -> widgetHandle` | `slui_image_file()` | Load the image **file** at `path`, preserving alpha, scaled into `maxW x maxH` (0 on an axis = size to source / preserve aspect). Returns the new widget handle, or `-1` if the file or decoder is unavailable. |
| `uiFontEffectBind` | `SLFontEffect` | `uiFontEffectBind(widgetHandle, kind, color0xRRGGBBAA, radiusPx, intensity, direction) -> effectHandle` | `slui_font_effect_bind()` | Bind a light/glow emitter onto a widget. `kind` ∈ {NONE,SHADOW,GLOW,LIGHT,EMITTER}; `direction` ∈ {RADIAL,UP,DOWN,LEFT,RIGHT} masks the cast so the light only leaves that side. Returns the effect handle, or `-1` if effects are unsupported. |
| `uiFontEffectUpdate` | `SLFontEffect` | `uiFontEffectUpdate(effectHandle, kind, color, radiusPx, intensity, direction) -> void` | `slui_font_effect_update()` | Push new emitter state onto a bound effect each frame (animation). No-op on an invalid handle. |
| `uiSleepMillis` | `MoriaUI.animate` | `uiSleepMillis(ms) -> void` | `slui_sleep_ms()` | Frame-pacing sleep for the constant throbber loop (~16 ms ≈ 60 fps). Harmless no-op if absent. |

## Notes

- **Colour packing** is `0xRRGGBBAA`, identical to `SLColor.rgba()` and the
  `CityLights` scoreboard — a native build can hand the word straight to an
  `SLUIColor` / `SLFontEffect` emitter.
- **Directional emitters.** The throbber binds an `EMITTER` with `direction =
  DOWN`, so its radiant white/yellow light falls only onto the Descriptive
  Canvas beneath the strip and never washes up over the title.
- **The animation loop** drives the toolkit with `uiAppPump(block=false)` (an
  existing built-in) rather than the blocking `uiAppRun`, ticking the throbber
  and calling `uiWindowRedraw` every frame until `pump` reports the window has
  closed. If `pump` is unavailable the loop falls back to `uiAppRun` so the
  window still shows (the throbber then holds a lit, static state).
