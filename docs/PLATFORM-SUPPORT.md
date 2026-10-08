# Airport Tycoon — Platform Support

Airport Tycoon is maintained as a cross-platform SLeeLa/JavaFX game.

## Supported desktop platforms

| Platform | Status | Primary build path |
|---|---|---|
| Windows 10 x64 | Supported | PowerShell + Maven/Java 21 |
| Windows 11 x64 | Supported | PowerShell + Maven/Java 21 |
| macOS 11+ Intel | Supported | POSIX shell + Maven/Java 21 |
| macOS 11+ Apple Silicon | Supported | POSIX shell + Maven/Java 21 |
| Linux x64 | Supported | Make + Maven/Java 21 |

The game logic remains SLeeLa source. The JavaFX layer is the desktop presentation/runtime mirror.

## Windows

Requirements: Windows 10 or newer, x64, Java 21 JDK, Maven 3.9+, and the SLeeLa toolchain for native `.sleela` execution. PowerShell 5.1+ is sufficient.

Run:

```powershell
.\build\windows\build.ps1
```

The PowerShell path builds and tests Editions 1-8 and prepares `/build`. GNU Make is optional on Windows.

## macOS

Requirements: macOS 11 (Big Sur) or newer, Intel or Apple Silicon, Java 21 JDK, Maven 3.9+, and the SLeeLa toolchain for native `.sleela` execution.

Run:

```bash
chmod +x build/macos/build.sh
./build/macos/build.sh
```

The same JavaFX project is used on Intel and Apple Silicon; Maven resolves the platform-native JavaFX artifacts.

## SLeeLa runtime

Native SLeeLa execution is enabled when `sleela` is on `PATH`, or when `SLEELA` points to the installed executable.

## Continuous integration

GitHub Actions tests the JavaFX layer on Linux, Windows, and macOS. GitHub provides hosted Windows and macOS runners, including Intel and Apple Silicon macOS options. The CI matrix catches path, Java, Maven, JavaFX, and process portability regressions.
