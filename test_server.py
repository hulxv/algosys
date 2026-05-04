#!/usr/bin/env python3
"""
Integration test: sends algorithms to the running server and checks
that the detected complexity class matches the expected one.

Usage:
    python3 test_server.py [--url http://localhost:5000]

Per-element computation has to be heavy enough to rise above metacall's
~320 ns/element serialization overhead, otherwise fast O(n) algorithms
look like O(1) after overhead subtraction. Tests for fast languages
(Node.js especially) compensate by doing extra work per element.
"""

import sys
import argparse
import textwrap
import requests


#  JavaScript 
_JS_ON = """\
function f(arr){
    let s = 0;
    for (const x of arr) s += x;
    return s;
}
module.exports={f};
"""

_JS_ONLOGN = """\
function f(arr){ return [...arr].sort((a,b)=>a-b); }
module.exports={f};
"""

_JS_ON2 = """\
function f(arr){
    let s = 0, n = arr.length;
    for (let i = 0; i < n; i++)
        for (let j = 0; j < n; j++)
            s += arr[i] * arr[j];
    return s;
}
module.exports={f};
"""


#  TypeScript: same shape as JS, just with annotations 
_TS_ON = """\
function f(arr: number[]): number {
    let s = 0;
    for (const x of arr) {
        let y = x;
        for (let k = 0; k < 30; k++) y = Math.log(y * y + 1.5);
        s += y;
    }
    return s;
}
module.exports={f};
"""

_TS_ON2 = """\
function f(arr: number[]): number {
    let s = 0, n = arr.length;
    for (let i = 0; i < n; i++)
        for (let j = 0; j < n; j++)
            s += Math.sqrt(arr[i] * arr[j] + 1);
    return s;
}
module.exports={f};
"""

#  C#: top-level static functions (C# 9+ / .NET 8 Roslyn scripting).
_CS_O1 = "static double f(double[] arr) { return arr[0]; }\n"

_CS_ON = """\
static double f(double[] arr) {
    double s = 0;
    for (int i = 0; i < arr.Length; i++) s += arr[i];
    return s;
}
"""

_CS_ON2 = """\
static double f(double[] arr) {
    double s = 0;
    int n = arr.Length;
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            s += arr[i] * arr[j];
    return s;
}
"""

#  C: plain C (TCC). The wrapper allocates the array internally so we
# never have to marshal a Python list across the metacall boundary. User
# functions take (double* arr, int n).
_C_O1 = """\
double f(double* arr, int n) { return arr[0]; }
"""

_C_ON = """\
double f(double* arr, int n) {
    double s = 0;
    int i;
    for (i = 0; i < n; ++i) s += arr[i];
    return s;
}
"""

_C_ON2 = """\
double f(double* arr, int n) {
    double s = 0;
    int i, j;
    for (i = 0; i < n; ++i)
        for (j = 0; j < n; ++j)
            s += arr[i] * arr[j];
    return s;
}
"""


#  Rust: functions take Vec<f64> (ownership transferred per call).
# O(1) is not tested: the bench wrapper creates a fresh Vec<f64> on every
# iteration (O(n) work) so O(1) functions are indistinguishable from O(n).
_RS_ON = """\
pub fn f(arr: Vec<f64>) -> f64 {
    arr.iter().sum()
}
"""

_RS_ON2 = """\
pub fn f(arr: Vec<f64>) -> f64 {
    let n = arr.len();
    let mut s = 0.0f64;
    for i in 0..n {
        for j in 0..n {
            s += (arr[i] - arr[j]).abs();
        }
    }
    s
}
"""


TESTS = [
    #  Python 
    ("Python O(1) - index access",       "py",   "f", "def f(arr): return arr[0]",            "O(1)"),
    ("Python O(n) - linear sum",         "py",   "f", "def f(arr): return sum(arr)",          "O(n)"),
    ("Python O(n log n) - sort",         "py",   "f", "def f(arr): return sorted(arr)",       "O(n log n)"),
    ("Python O(n^2) - nested loops",     "py",   "f", textwrap.dedent("""\
        def f(arr):
            s = 0.0
            n = len(arr)
            for i in range(n):
                for j in range(n):
                    s += arr[i] * arr[j]
            return s
    """), "O(n^2)"),

    #  JavaScript (Node.js) 
    ("JavaScript O(1) - index access",   "node", "f", "function f(arr){return arr[0];}\nmodule.exports={f};", "O(1)"),
    ("JavaScript O(n) - linear sum",     "node", "f", _JS_ON,     "O(n)"),
    ("JavaScript O(n log n) - sort",     "node", "f", _JS_ONLOGN, "O(n log n)"),
    ("JavaScript O(n^2) - nested loops", "node", "f", _JS_ON2,    "O(n^2)"),

    #  C# (.NET 8)
    # ("C# O(1) - index access",           "cs",   "f", _CS_O1,  "O(1)"),
    # ("C# O(n) - linear sum",             "cs",   "f", _CS_ON,  "O(n)"),
    # ("C# O(n^2) - nested loops",         "cs",   "f", _CS_ON2, "O(n^2)"),

    #  C (plain C, TCC): TODO
    # ("C O(1) - index access",            "c",    "f", _C_O1,  "O(1)"),
    # ("C O(n) - linear sum",              "c",    "f", _C_ON,  "O(n)"),
    # ("C O(n^2) - nested loops",          "c",    "f", _C_ON2, "O(n^2)"),

    #  Ruby - removed: MRI crashes (SIGSEGV) when embedded via metacall

    #  Lua
    # Disabled: upstream lua_loader has unfixed build bugs (missing lauxlib.h).

    #  R 
    # No R loader in metacall's standard build (no sub_r in environment script).

    #  TypeScript 
    # ("TypeScript O(1) - index access",   "ts",   "f", "function f(arr: number[]): number { return arr[0]; }\nmodule.exports={f};", "O(1)"),
    # ("TypeScript O(n) - heavy log loop", "ts",   "f", _TS_ON,  "O(n)"),
    # ("TypeScript O(n^2) - nested loops", "ts",   "f", _TS_ON2, "O(n^2)"),

    #  Go 
    # Go loader is TODO in upstream metacall - only a Go *port* exists.
    # metacall_load_from_memory("go", ...) will fail until upstream implements it.
    # ("Go O(1) - index access",   "go", "F", "func F(arr []float64) float64 { return arr[0] }\n", "O(1)"),
    # ("Go O(n) - linear sum",     "go", "F", _GO_ON,  "O(n)"),
    # ("Go O(n^2) - nested loops", "go", "F", _GO_ON2, "O(n^2)"),

    #  Rust
    # O(1) omitted: bench wrapper allocates Vec<f64> each iteration (O(n) overhead)
    # which masks constant-time functions. O(n) and above are reliable.
    ("Rust O(n) — linear sum",     "rs", "f", _RS_ON,  "O(n)"),
    ("Rust O(n^2) — nested loops", "rs", "f", _RS_ON2, "O(n^2)"),

    #  WebAssembly
    # WASM tests are not included: the metacall wasm loader expects binary .wasm
    # files, not source code. Inline WASM source testing is not practical.
    # To test WASM, pre-compile a .wasm file and load it via the /analyze endpoint.
]


# ---------------------------------------------------------------------------

def run_test(url: str, desc: str, lang: str, func: str, code: str, expected: str) -> bool:
    payload = {"code": code, "lang": lang, "func": func}
    try:
        resp = requests.post(f"{url}/analyze", json=payload, timeout=300)
        data = resp.json()
    except requests.exceptions.ConnectionError:
        print(f"  SKIP  {desc}")
        print(f"        Could not connect to {url}")
        return False
    except Exception as e:
        print(f"  FAIL  {desc}")
        print(f"        Request error: {e}")
        return False

    if "error" in data:
        print(f"  FAIL  {desc}")
        print(f"        Server error: {data['error']}")
        return False

    got = data.get("dataset", {}).get("complexity", {}).get("class", "<missing>")
    r2  = data.get("dataset", {}).get("complexity", {}).get("r2", 0.0)

    if got == expected:
        print(f"  PASS  {desc}")
        print(f"        {got}  (R²={r2:.4f})")
        return True
    else:
        print(f"  FAIL  {desc}")
        print(f"        expected {expected!r}, got {got!r}  (R²={r2:.4f})")
        return False


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:5000",
                        help="Server base URL (default: http://localhost:5000)")
    args = parser.parse_args()

    url = args.url.rstrip("/")
    passed = 0
    failed = 0

    print(f"Testing server at {url}\n")

    for test in TESTS:
        ok = run_test(url, *test)
        if ok:
            passed += 1
        else:
            failed += 1
        print()

    total = passed + failed
    print(f"Results: {passed}/{total} passed", end="")
    if failed:
        print(f", {failed} failed")
        sys.exit(1)
    else:
        print()


if __name__ == "__main__":
    main()
