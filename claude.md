# System Guide & Codebase Architecture (claude.md)

Welcome to **Arena 3** — a premium, zero-dependency Java Swing desktop arcade suite featuring **Snake**, **Ludo**, and **Chess**, each fully equipped with smart AI opponents, sound effects, and a dark luxury retro-neon theme.

This document serves as an architectural blueprint, developer guide, and system instructions for AI assistants (like Claude, Antigravity, etc.) maintaining or expanding this codebase.

---

## 🏗️ Architecture & Component Layout

The codebase is organized under the `arena3` package:

```
arena3/
├── src/arena3/
│   ├── Main.java              # Bootstrapper (App Entry point)
│   ├── core/                  # Engine & Framework infrastructure
│   │   ├── GameLauncher.java  # CardLayout container for screen switching
│   │   ├── GameState.java     # Config structs & state machines
│   │   └── SoundEngine.java   # MIDI synthesizer for retro audio effects
│   ├── menu/                  # Menu & Configuration Screens
│   │   ├── MainMenuScreen.java
│   │   └── PlayerSetupScreen.java
│   ├── ui/                    # Styling & Theming Core
│   │   ├── ThemeConstants.java # Color palette, Fonts, and Dimensions
│   │   └── GameOverScreen.java # Modular screen for showing game results
│   ├── snake/                 # Snake implementation
│   │   ├── SnakeGame.java     # Game controller panel
│   │   ├── SnakeBoard.java    # Grid renderer
│   │   ├── SnakeEngine.java   # Game physics & snake movement rules
│   │   ├── SnakePlayer.java   # Entity data structure
│   │   └── SnakeAI.java       # Pathfinding algorithm (BFS)
│   ├── ludo/                  # Ludo implementation
│   │   ├── LudoGame.java      # Game coordinator panel
│   │   ├── LudoBoard.java     # Custom painted board & mouse triggers
│   │   ├── LudoEngine.java    # Turn logic & Ludo rules
│   │   ├── LudoPlayer.java    # Player attributes (color, AI status)
│   │   ├── LudoToken.java     # Pawn coordinates and home tracking
│   │   └── LudoAI.java        # Heuristics (Capture > Safe Square > Advance)
│   └── chess/                 # Chess implementation
│       ├── ChessGame.java     # Main UI & sidebar logs
│       ├── ChessBoard.java    # Board square painting and mouse interaction
│       ├── ChessEngine.java   # Move verification, Castling, Checkmate, Stalemate
│       ├── ChessPiece.java    # Chess piece enumeration and assets
│       ├── ChessMove.java     # Move representation
│       └── ChessAI.java       # Minimax + Alpha-Beta Pruning + Piece-Square Tables
```

---

## 🎨 Theme & UI Style Guide

To maintain the "Luxury Dark Arcade" aesthetic, follow these strict rules:

- **Primary Colors** (defined in `ThemeConstants`):
  - Background Dark: `new Color(10, 10, 12)`
  - Panel Dark: `new Color(20, 20, 25)`
  - Accent Gold (Neon Gold): `new Color(212, 175, 55)`
  - Accent Cyan (Neon Cyan): `new Color(0, 238, 255)`
  - Accent Red (Neon Red): `new Color(255, 46, 99)`
- **Typography**:
  - Always use monospaced fonts (`Font.MONOSPACED`, `Courier New`, or `Consolas`) to preserve the retro computer terminal vibe.
  - Buttons and interactive items must change their border/foreground color on mouse hover to provide high-fidelity feedback.
- **Rendering**:
  - Turn on Anti-Aliasing (`RenderingHints.KEY_ANTIALIASING`, `RenderingHints.VALUE_ANTIALIAS_ON`) in all `paintComponent` overrides.
  - Make sure fonts use `RenderingHints.KEY_TEXT_ANTIALIASING`, `RenderingHints.VALUE_TEXT_ANTIALIAS_ON`.

---

## 🎵 Sound & Feedback

The `SoundEngine` generates sound effects dynamically using the Java Sound API (MIDI synthesizer).
- **Sound Effects**:
  - Menu Select: `SoundEngine.playMenuSelect()`
  - Game Over: `SoundEngine.playGameOver()`
  - Ludo Dice Roll: `SoundEngine.playDiceRoll()`
  - Chess Move: `SoundEngine.playChessMove()`
  - Snake Eat: `SoundEngine.playSnakeEat()`
  - Crash/Capture: `SoundEngine.playCrash()`
- **Guidelines**:
  - Always check `GameState.soundMuted` before calling play methods, or call the helper functions which check this state internally.

---

## ⚙️ Compilation & Development Workflow

### 1. Build and Compile
We use a standard double-pass build flow (no external compiler required, Java SE 11+ compatible):
```bash
# Clean output
rmdir /s /q out
mkdir out

# Compile
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt

# Package
echo Main-Class: arena3.Main > manifest.txt
jar cfm Arena3.jar manifest.txt -C out .
del manifest.txt
```

### 2. Run
```bash
java -jar Arena3.jar
```

---

## 🧠 AI Rules & Implementation Guides

When writing or modifying AI systems:
1. **Snake AI**: Uses BFS to find the shortest path to the food. If BFS fails (trapped), fallback to any valid open neighbor that doesn't immediately crash.
2. **Ludo AI**: Prioritizes capturing opponents first, entering pieces onto the board second, moving pieces out of danger third, and moving the furthest piece forward last.
3. **Chess AI**:
   - Easy: Picks random legal moves.
   - Medium: Minimax with Alpha-Beta pruning at Depth 2.
   - Hard: Minimax with Alpha-Beta pruning at Depth 3 using piece-square tables for positional scoring.
   - Keep calculations clean to avoid dropping the frame rate of the EDT (Event Dispatch Thread).

---

## ⚠️ Key constraints for AI Developers
- **Do not introduce external libraries** (like Maven/Gradle dependencies). The application must compile with pure Java SE SDK.
- **Thread Safety**: UI transitions and updates should always run on the Event Dispatch Thread (`SwingUtilities.invokeLater`).
- **Input handling**: Add clean keybindings/mnemonics instead of raw `KeyListener` where possible (use `getInputMap` / `getActionMap` to prevent focus issues).
