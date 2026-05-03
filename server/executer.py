import re
import time
from uuid import uuid4
from typing import Any, Optional
from metacall import metacall_load_from_memory, metacall


# Per-language benchmark wrapper. Each defines a function {bench_name}(arr, k)
# that runs the user's renamed function k times in a tight loop and returns
# the elapsed wall time in nanoseconds - measured *inside* the runtime, so
# metacall serialization is paid once per outer call and never enters the
# timed region. This is what JMH/criterion/Google Benchmark do.
_BENCH_WRAPPERS: dict[str, str] = {
    "py": (
        "import time as _bench_time_{tag}\n"
        "def {bench_name}(arr, k):\n"
        "    {fn_name}(arr)\n"
        "    _t0 = _bench_time_{tag}.perf_counter_ns()\n"
        "    for _ in range(k):\n"
        "        {fn_name}(arr)\n"
        "    return _bench_time_{tag}.perf_counter_ns() - _t0\n"
    ),
    "node": (
        "function {bench_name}(arr, k) {{\n"
        "    {fn_name}(arr);\n"
        "    const t0 = process.hrtime.bigint();\n"
        "    for (let i = 0; i < k; i++) {fn_name}(arr);\n"
        "    return Number(process.hrtime.bigint() - t0);\n"
        "}}\n"
        "module.exports.{bench_name} = {bench_name};\n"
    ),
    "ts": (
        "function {bench_name}(arr: number[], k: number): number {{\n"
        "    {fn_name}(arr);\n"
        "    const t0 = process.hrtime.bigint();\n"
        "    for (let i = 0; i < k; i++) {fn_name}(arr);\n"
        "    return Number(process.hrtime.bigint() - t0);\n"
        "}}\n"
        "module.exports.{bench_name} = {bench_name};\n"
    ),
    "c": (
        # Plain C via TCC. No #include - TCC's header search path is unreliable.
        # clock_gettime declared with void* to avoid redefining struct timespec
        # across multiple metacall_load_from_memory calls: TCC shares its type
        # table within a process, so a second struct definition is a hard error.
        # long _ts[2] has the same memory layout as struct timespec {long;long;}.
        # All declarations are at the top of the function body (C89 compatible).
        "extern void *malloc(unsigned long n);\n"
        "extern void free(void *p);\n"
        "extern void srand(unsigned int seed);\n"
        "extern int rand(void);\n"
        "#define RAND_MAX 2147483647\n"
        "#define CLOCK_MONOTONIC 1\n"
        "extern int clock_gettime(int clk_id, void *tp);\n"
        "double {bench_name}(int n, int k) {{\n"
        "    double *arr;\n"
        "    long _ts0[2], _ts1[2];\n"
        "    int _ii, _i;\n"
        "    arr = (double *)malloc(n * sizeof(double));\n"
        "    srand(n);\n"
        "    for (_ii = 0; _ii < n; ++_ii) arr[_ii] = (double)rand() / RAND_MAX;\n"
        "    {fn_name}(arr, n);\n"
        "    clock_gettime(CLOCK_MONOTONIC, _ts0);\n"
        "    for (_i = 0; _i < k; ++_i) {fn_name}(arr, n);\n"
        "    clock_gettime(CLOCK_MONOTONIC, _ts1);\n"
        "    free(arr);\n"
        "    return (double)((_ts1[0]-_ts0[0])*1000000000+(_ts1[1]-_ts0[1]));\n"
        "}}\n"
    ),
    # Ruby removed: MRI crashes (SIGSEGV) when called via metacall from a
    # non-main OS thread - same root cause as the Java loader removal.

    # C# — top-level static functions (C# 9+ / Roslyn scripting).
    # Stopwatch.GetTimestamp() / Frequency converts ticks → nanoseconds.
    "cs": (
        "static long {bench_name}(double[] arr, int k) {{\n"
        "    {fn_name}(arr);\n"
        "    var _sw = System.Diagnostics.Stopwatch.GetTimestamp();\n"
        "    for (int _i = 0; _i < k; _i++) {fn_name}(arr);\n"
        "    long _el = System.Diagnostics.Stopwatch.GetTimestamp() - _sw;\n"
        "    return (long)((double)_el / System.Diagnostics.Stopwatch.Frequency * 1e9);\n"
        "}}\n"
    ),

    # Go: function-body only (no package/import); those are prepended in __init__.
    # Functions must be exported (start with uppercase) for metacall to expose them.
    "go": (
        "func {bench_name}(arr []float64, k int) int64 {{\n"
        "    {fn_name}(arr)\n"
        "    t0 := time.Now()\n"
        "    for i := 0; i < k; i++ {{\n"
        "        {fn_name}(arr)\n"
        "    }}\n"
        "    return time.Since(t0).Nanoseconds()\n"
        "}}\n"
    ),
}


class Executer:
    def __init__(self, code: str, lang: str, func: str, args: Optional[list[Any]] = None):
        self.lang = lang
        self.args = args or []

        # Go exported names must start with uppercase so metacall can call them.
        if lang == "go":
            unique_fn = f"Fn_{uuid4().hex}"
            unique_bench = f"Bench_{uuid4().hex}"
        else:
            unique_fn = f"fn_{uuid4().hex}"
            unique_bench = f"bench_{uuid4().hex}"
        self._fn_name = unique_fn
        self._bench_name = unique_bench

        # Rename user's function so concurrent loads don't collide in
        # metacall's permanent global namespace.
        renamed = re.sub(r'(?<!\.)\b' + re.escape(func) + r'\b', unique_fn, code)

        # Compose the user's code + the in-language benchmark wrapper. Both
        # share a single load so the wrapper can call the renamed function
        # directly without going through metacall.
        wrapper_template = _BENCH_WRAPPERS.get(lang)
        if wrapper_template is not None:
            wrapper = wrapper_template.format(
                fn_name=unique_fn,
                bench_name=unique_bench,
                tag=uuid4().hex[:8],
            )
            if lang == "go":
                # Go needs package declaration + time import at the top.
                # User code is just function bodies without package/import headers.
                self.code = 'package main\n\nimport "time"\n\n' + renamed + "\n" + wrapper
            else:
                self.code = renamed + "\n" + wrapper
            self.func = unique_bench
        else:
            self.code = renamed
            self.func = unique_fn

    def load(self) -> None:
        metacall_load_from_memory(self.lang, self.code)

    def call(self, *args: Any) -> Any:
        return metacall(self._fn_name, *args)

    def bench(self, arr: Any, k: int) -> int:
        """Run the user's function k times and return elapsed nanoseconds.

        For languages with an in-runtime wrapper (the common case), timing
        happens inside the runtime - metacall's per-call serialization cost
        is *not* in the timed region. For languages without a wrapper, fall
        back to outer wall-clock timing."""
        if self.lang in _BENCH_WRAPPERS:
            # C wrapper self-allocates its array - only pass (n, k), not the list.
            if self.lang == "c":
                return int(metacall(self._bench_name, len(arr), k))
            return int(metacall(self._bench_name, arr, k))
        t0 = time.perf_counter_ns()
        for _ in range(k):
            metacall(self._fn_name, arr)
        return time.perf_counter_ns() - t0

    def exec(self) -> Any:
        self.load()
        return self.call(*self.args)
