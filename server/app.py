import time
from typing import Any, cast
from flask import Flask, request, jsonify
from executer import Executer
from bench import Bench

app = Flask(__name__)

# Metacall loader tags and their human-readable names
SUPPORTED_LOADERS: dict[str, str] = {
    "py":   "Python",
    "node": "Node.js",
    # "ts":   "TypeScript",
    # "cs":   "C# (.NET 8)",
    # "c":    "C",
    # "cob":  "COBOL",
    "rs":   "Rust",
    # "wasm": "WebAssembly",
    # "rb"  - removed; MRI Ruby crashes (SIGSEGV) when called from a non-main thread
    # "go"  - no go_loader in metacall; only a Go port exists (TODO upstream)
    # "java" - removed; causes SIGSEGV in the multithreaded Flask process
}


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/loaders", methods=["GET"])
def loaders():
    return jsonify({
        "loaders": [
            {"tag": tag, "language": name}
            for tag, name in SUPPORTED_LOADERS.items()
        ]
    })


@app.route("/analyze", methods=["POST"])
def analyze():
    incoming_data = cast(dict[str, Any], request.get_json())  # type: ignore[no-untyped-call]

    lang = incoming_data.get("lang", "")
    if lang not in SUPPORTED_LOADERS:
        return jsonify({
            "error": f"Unsupported loader: '{lang}'",
            "supported": list(SUPPORTED_LOADERS.keys()),
        }), 400

    try:
        mode = int(incoming_data.get("mode", 2))
        executer = Executer(
            incoming_data["code"],
            lang,
            incoming_data["func"],
        )
        if mode == 1:
            return jsonify({"dataset": run_manual(executer, incoming_data.get("array"))})

        bench = Bench(executer)
        dataset = bench.bench_exhaustive()
        return jsonify({"dataset": dataset})
    except Exception as e:
        app.logger.exception("analyze failed")
        return jsonify({"error": str(e)}), 500


def run_manual(executer: Executer, raw_array: Any) -> dict[str, Any]:
    if not isinstance(raw_array, list):
        raise ValueError("Mode 1 requires an 'array' field containing a JSON array")

    arr = [float(value) for value in raw_array]
    timed_arr = list(arr)
    output_arr = list(arr)

    executer.load()

    start = time.perf_counter_ns()
    output = executer.call(timed_arr)
    elapsed_ns = max(1, time.perf_counter_ns() - start)

    display_output = output if output is not None else timed_arr
    return {
        "points": [{"n": len(arr), "time_ns": elapsed_ns}],
        "complexity": {
            "class": "Manual",
            "r2": 1.0,
            "coef": 0.0,
            "intercept": float(elapsed_ns),
        },
        "input": output_arr,
        "output": json_safe(display_output),
    }


def json_safe(value: Any) -> Any:
    if value is None or isinstance(value, (str, int, float, bool)):
        return value
    if isinstance(value, (list, tuple)):
        return [json_safe(item) for item in value]
    if isinstance(value, dict):
        return {str(key): json_safe(item) for key, item in value.items()}
    return str(value)


if __name__ == "__main__":
    # threaded=False: Ruby (MRI) and other runtimes embedded via metacall are
    # not safe to call from arbitrary OS threads. Single-threaded mode keeps
    # all metacall invocations on the main thread, matching what Java needed too.
    app.run(host="0.0.0.0", port=5000, threaded=False)
