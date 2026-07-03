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
- Maven (for building from source)

## Build & run

```bash
mvn clean package
java -jar target/SvgViewer.jar
```

`mvn package` pulls in Apache Batik and bundles everything into a single
runnable jar via the shade plugin — no manual classpath setup needed.

## Building without Maven

1. Download the Batik 1.17 binary distribution from the
   [Apache Batik downloads page](https://xmlgraphics.apache.org/batik/download.html)
2. Add every JAR in its `lib/` folder to your project's classpath
3. Compile and run `SvgViewer.java` directly

## How it renders

The viewer uses Batik's `JSVGCanvas` for SVG parsing and rendering, but drives
pan/zoom itself via `AffineTransform` manipulation rather than relying on
Batik's built-in mouse gestures — this keeps the interaction model simple and
predictable (plain click-drag to pan, wheel to zoom, no modifier keys needed).

## License

MIT
