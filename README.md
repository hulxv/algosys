# Algorithm Performance Evaluator

A desktop tool that runs user-submitted algorithms against increasing input sizes and estimates their time complexity empirically.

## Architecture

```
client/   JavaFX desktop app (Maven)   →  sends code + config via HTTP
server/   Flask REST API               →  runs code, measures time, fits complexity curve
```

## Requirements

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.8+ |
| Python | 3.10+ |

## Setup

```bash
make install   # create Python venv + install deps, resolve Maven deps
```

## Running

```bash
make server    # start Flask on http://localhost:5000
make client    # launch JavaFX app
```

## Other Commands

```bash
make build     # compile + package the client JAR
make clean     # remove build artifacts
```

## Project Structure

```
algosys/
├── client/                          # JavaFX Maven project
│   ├── pom.xml
│   └── src/main/java/com/algosys/
│       └── Main.java
├── server/                          # Flask server
│   ├── app.py
│   └── requirements.txt
├── Makefile
└── README.md
```

## How It Works

1. User submits algorithm code and an input generator from the client.
2. Server executes the algorithm at sizes `n = [10, 50, 100, 500, 1000, 5000, 10000]` and records runtimes.
3. Runtimes are fitted against O(1), O(log n), O(n), O(n log n), O(n²), O(n³) models using `scipy`.
4. Best-fit complexity is returned alongside a chart of measured vs. fitted curves.
