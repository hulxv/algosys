# AlgoSys API

Base URL: `http://localhost:5000`

---

## Endpoints

### `GET /health`

Returns server liveness.

**Response**
```json
{ "status": "ok" }
```

---

### `GET /loaders`

Lists the language loaders available on this server.

**Response**
```json
{
  "loaders": [
    { "tag": "py",   "language": "Python" },
    { "tag": "node", "language": "Node.js" },
    { "tag": "ts",   "language": "TypeScript" },
    { "tag": "cs",   "language": "C# (.NET 8)" },
    { "tag": "c",    "language": "C" },
    { "tag": "cob",  "language": "COBOL" },
    { "tag": "rs",   "language": "Rust" },
    { "tag": "wasm", "language": "WebAssembly" }
  ]
}
```

---

### `POST /analyze`

Loads a code snippet into the requested runtime. In Mode 1 it runs the function once on a manual array; in Mode 2 it benchmarks across generated input sizes and returns the detected asymptotic complexity.

**Request body** (`Content-Type: application/json`)

| Field   | Type      | Description                                            |
| ------- | --------- | ------------------------------------------------------ |
| `mode`  | integer   | `1` for manual run, `2` for automatic benchmark sweep  |
| `lang`  | string    | Loader tag (see `/loaders`)                            |
| `func`  | string    | Name of the top-level function to run or benchmark     |
| `code`  | string    | Full source snippet in the target language             |
| `array` | number[]  | Manual input array for Mode 1; ignored by Mode 2       |

```json
{
  "mode": 2,
  "lang": "py",
  "func": "f",
  "code": "def f(arr):\n    return sum(arr)\n",
  "array": [5, 3, 8, 1]
}
```

**Response — Mode 2 success** `200`

```json
{
  "dataset": {
    "points": [
      { "n": 100,    "time_ns": 812 },
      { "n": 300,    "time_ns": 2491 },
      { "n": 1000,   "time_ns": 8330 },
      { "n": 3000,   "time_ns": 25102 },
      { "n": 10000,  "time_ns": 83761 },
      { "n": 30000,  "time_ns": 251880 },
      { "n": 100000, "time_ns": 839204 }
    ],
    "complexity": {
      "class":     "O(n)",
      "r2":        0.9998,
      "coef":      8.37,
      "intercept": -12.4
    }
  }
}
```

**Response — Mode 1 success** `200`

```json
{
  "dataset": {
    "points": [
      { "n": 4, "time_ns": 10421 }
    ],
    "complexity": {
      "class": "Manual",
      "r2": 1.0,
      "coef": 0.0,
      "intercept": 10421.0
    },
    "input": [5.0, 3.0, 8.0, 1.0],
    "output": 17.0
  }
}
```

**Response — unsupported loader** `400`

```json
{
  "error": "Unsupported loader: 'java'",
  "supported": ["py", "node", "ts", "cs", "c", "cob", "rs", "wasm"]
}
```

**Response — runtime error** `500`

```json
{
  "error": "metacall_load_from_memory returned an error"
}
```

---

## Data shapes

### `dataset.points`

Array of measured data points. Each entry is the **median per-call time** at a given input size `n`, after timing is saturated to ~50 ms inside the target runtime.

| Field     | Type    | Description                         |
| --------- | ------- | ----------------------------------- |
| `n`       | integer | Input array length                  |
| `time_ns` | integer | Median per-call time in nanoseconds |

The server uses a fixed grid of `n` values: `[100, 300, 1000, 3000, 10000, 30000, 100000]`. Measurement stops early if a single size takes longer than 5 s, so slow algorithms (e.g. Python O(n^2) at large n) will have fewer points.

### `dataset.complexity`

Result of fitting `time_ns ~ coef * f(n) + intercept` for each candidate complexity class and picking the best R^2.

| Field       | Type   | Description                                            |
| ----------- | ------ | ------------------------------------------------------ |
| `class`     | string | Best-fit complexity class (see table below)            |
| `r2`        | float  | Coefficient of determination (0–1, higher is better)   |
| `coef`      | float  | Slope of the linear fit against the complexity feature |
| `intercept` | float  | Intercept of the fit                                   |

#### Possible `class` values

| Class            | Feature used for fitting |
| ---------------- | ------------------------ |
| `"O(1)"`         | constant `1`             |
| `"O(log n)"`     | `log2(n)`                |
| `"O(n)"`         | `n`                      |
| `"O(n log n)"`   | `n · log2(n)`            |
| `"O(n^2)"`       | `n^2`                    |
| `"O(n^2 log n)"` | `n^2 · log2(n)`          |
| `"O(n^3)"`       | `n³`                     |
| `"O(2^n)"`       | `2ⁿ`                     |

---

## Language-specific notes

### Python (`py`)
Function receives a `list[float]`. Return value is ignored.

```python
def f(arr):
    return sum(arr)
```

### JavaScript / Node.js (`node`)
Function receives a JS array. Must be exported via `module.exports`.

```js
function f(arr) { return arr.reduce((s, x) => s + x, 0); }
module.exports = { f };
```

### TypeScript (`ts`)
Same shape as Node.js with type annotations. Must be exported via `module.exports`.

```ts
function f(arr: number[]): number { return arr.reduce((s, x) => s + x, 0); }
module.exports = { f };
```

### C# (`cs`)
Top-level static function (C# 9+ / Roslyn scripting). Function receives `double[]`.

```csharp
static double f(double[] arr) {
    double s = 0;
    foreach (var x in arr) s += x;
    return s;
}
```

### C (`c`)
Function signature must be `double f(double* arr, int n)`. No `#include` — declare any external symbols you need with `extern`.

```c
double f(double* arr, int n) {
    double s = 0;
    int i;
    for (i = 0; i < n; ++i) s += arr[i];
    return s;
}
```

### COBOL (`cob`), Rust (`rs`), WebAssembly (`wasm`)
Accepted by the server; refer to the metacall loader documentation for the expected source format.

---

## Example — curl

```bash
curl -s -X POST http://localhost:5000/analyze \
  -H 'Content-Type: application/json' \
  -d '{"mode":2,"lang":"py","func":"f","code":"def f(arr): return sum(arr)","array":[]}' \
  | python3 -m json.tool
```
