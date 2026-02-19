# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.5.1] - 2026-02-19

### Fixed
- Zen Mode: the continue game dialog now shows the moves made
  instead of the elapsed time (Zen mode has no timer)
- Placing a number now automatically deselects the cell
  to prevent accidentally placing a second number
  (except in manual notes mode, where the cell remains selected)

### Improved
- Visual spacing adjustment between the "Daily Sudoku" title and the date
- Visual spacing adjustment between the difficulty label and the timer
- Improved achievement detection and tracking logic

---

## [1.5.0] - 2026-02-18

### Added
- **Zen Mode** - Timer-free gameplay for relaxed solving
    - Independent saved games for Normal vs Zen modes
    - Tracks moves instead of time
    - Toggle available only in Normal game mode
    - Zen Master achievement (50 Zen sudokus completed)
- **New Achievements** - 3 additional challenges
    - **Daily Streak** - 7 consecutive days of Daily Sudoku
    - **Perfect Daily** - 3 Daily Sudokus with no errors or hints
    - **Zen Master** - Complete 50 Zen sudokus
- **Daily Sudoku** - Rotating daily difficulty system
    - One unique puzzle per day with progressive difficulty
    - Tracks daily best time and current streak
    - Prevents replaying the same daily puzzle

### Fixed
- **Achievement calculation accuracy** - Fixed stats tracking for no-hints/no-errors achievements
- **Independent game states** - Zen/Normal modes now have separate saved games
- **Reset button timer bug** - Fixed timer reset to zero on game reset
- **Cell contrast improvements** - Better visual distinction between fixed/user cells and notes

### Improved
- **Statistics tracking** - Enhanced precision for achievement progress
- **Game state management** - Unique keys for all mode combinations (Normal/Zen/Daily)
- **UI contrast** - Improved readability on e-ink displays

### Technical
- **Android Gradle Plugin updated** to 9.0.1
- Version code: 5 → 6
- Version name: 1.4.0 → 1.5.0
- `GameStateManager` now supports `isZenMode` parameter
- `StatisticsManager.recordCompletion()` now tracks `isDaily` and `isZenMode`
- New achievement strings for all languages (CA/ES/EN)
- Enhanced `MainScreen` with Zen Mode toggle component

---

## [1.4.0] - 2026-02-13

### Added
- **Smart Auto Notes Mode** - Intelligent note management system
  [... resto del changelog anterior sin cambios]
