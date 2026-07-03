# SVG Viewer (Java Swing)

A lightweight, standalone SVG viewer for Windows/Linux/macOS, built with Java 8
Swing and [Apache Batik](https://xmlgraphics.apache.org/batik/). No install,
no browser, no dependencies beyond the JAR — just open an `.svg` and look at it.

## Features

- **Open** — `File → Open`, filtered to `.svg` files
- **Pan** — click and drag
- **Zoom** — mouse wheel, toolbar buttons, or `Ctrl +` / `Ctrl -`
- **Fit to screen** — automatic on load, or `Ctrl+0` any time
- **Actual size** — `Ctrl+1` resets to 100%
- Remembers the last folder you opened a file from, for the current session

## Requirements

- Java 8 or newer
- NetBeans

All required Batik libraries are already included in the project — no Maven,
no downloading dependencies, nothing to configure.

## Build & run

1. Open the project in NetBeans
2. Build and run — that's it

## How it renders

The viewer uses Batik's `JSVGCanvas` for SVG parsing and rendering, but drives
pan/zoom itself via `AffineTransform` manipulation rather than relying on
Batik's built-in mouse gestures — this keeps the interaction model simple and
predictable (plain click-drag to pan, wheel to zoom, no modifier keys needed).

## License

MIT
