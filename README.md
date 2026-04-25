# Algorithm Performance Evaluator

A desktop tool that runs user-submitted algorithms against increasing input sizes and estimates their time complexity empirically.

## Requirements

| Tool   | Version |
| ------ | ------- |
| Java   | 21+     |
| Maven  | 3.8+    |
| Python | 3.10+   |

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
