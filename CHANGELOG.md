# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-06-25

This is the first release of the **Sudoku E-Ink HTR** fork. This project was forked from [SudokuEink by ktacrack](https://github.com/ktacrack/SudokuEink) at version 1.5.1.

### Added
- **Stylus-first gameplay:** Write directly on the board and keep original handwriting throughout the game for a paper-like feel.
- **Advanced Handwriting Recognition Models:** Support for ONNX (AGPL 3.0) and Google ML Kit (Apache 2.0) models. To save APK size and comply with licenses, these models are downloaded on-demand.
- **Action-first interaction:** Select a tool or digit first, then tap the target cell to apply it (one-shot action).
- **Flattened Navigation:** Simplified menus and centralized settings for a less distracting game flow.

### Changed
- **E-ink optimized UI:** Replaced the UI with Mudita UI components for a strict black-and-white, high-contrast experience, removing cell highlights, background patterns, and generic grays to minimize e-ink ghosting.
- Changed app name to "Sudoku E-Ink HTR" and package name to `io.github.serg987.sudokueinkhtr`.
- Removed old upstream changelog history to focus purely on the forked version's trajectory.
- *Note: Most of the code changes in this fork were made with the assistance of AI.*

### Removed
- Dark mode temporarily disabled due to incompatibility with the new high-contrast UI approach.
