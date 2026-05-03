from typing import cast
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
    "cob":  "COBOL",
    "rs":   "Rust",
    "wasm": "WebAssembly",
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
    incoming_data = cast(dict[str, str], request.get_json())  # type: ignore[no-untyped-call]

    lang = incoming_data.get("lang", "")
    if lang not in SUPPORTED_LOADERS:
        return jsonify({
            "error": f"Unsupported loader: '{lang}'",
            "supported": list(SUPPORTED_LOADERS.keys()),
        }), 400

    try:
        executer = Executer(
            incoming_data["code"],
            lang,
            incoming_data["func"],
        )
        bench = Bench(executer)
        dataset = bench.bench_exhaustive()
        return jsonify({"dataset": dataset})
    except Exception as e:
        app.logger.exception("analyze failed")
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    # threaded=False: Ruby (MRI) and other runtimes embedded via metacall are
    # not safe to call from arbitrary OS threads. Single-threaded mode keeps
    # all metacall invocations on the main thread, matching what Java needed too.
    app.run(host="0.0.0.0", port=5000, threaded=False)
