from flask import Flask, request, jsonify
from executer import Executer
from bench import Bench
app = Flask(__name__)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/analyze", methods=["POST"])
def analyze():
    incoming_data = request.get_json()
    executer = Executer(
        incoming_data["code"],
        incoming_data["lang"],
        incoming_data["func"],
    )
    bench = Bench(executer)
    dataset = bench.bench_exhaustive(incoming_data["sizes"])
    return jsonify({"dataset": dataset})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
