from flask import Flask, request, jsonify

app = Flask(__name__)


@app.route("/health", methods=["GET"])
def health():
    pass


@app.route("/analyze", methods=["POST"])
def analyze():
    # expects JSON: { "code": str, "input_generator": str }
    pass


if __name__ == "__main__":
    app.run(debug=True, port=5000)
