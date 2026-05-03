import time
import random
import statistics
from typing import Any
from executer import Executer

# Fixed n grid: same sizes every run → reproducible JIT / cache state.
_N_GRID          = [100, 300, 1000, 3000, 10000, 30000, 100000]

# Stop adding sizes once a single phase exceeds this. Slow algorithms
# (Python O(n²) at n=100k) terminate at the same n on a stable machine.
_WALL_BUDGET_NS  = 5_000_000_000

# Outer warm-up calls before the first calibration - enough for V8/JVM JIT
# to reach steady state.
_WARMUP_CALLS    = 30

# Outer repetitions per size; the median wins.
_BENCH_RUNS      = 5

# Each timed inner loop targets ~50 ms - long enough to drown out timer
# resolution and OS jitter, short enough that 7 sizes × 5 runs fit in
# a few seconds for the fast algorithms.
_TARGET_BENCH_NS = 50_000_000
_MIN_K           = 1
_MAX_K           = 10_000_000


def _seeded_array(n: int) -> list[float]:
    """Same n always yields the same array - removes data-dependent variance
    (sort patterns, branch prediction) from run to run."""
    rng = random.Random(n)
    return [rng.random() for _ in range(n)]


class Bench:
    def __init__(self, executer: Executer):
        self.executer = executer
        executer.load()

    def _warmup(self) -> None:
        arr = _seeded_array(_N_GRID[0])
        for _ in range(_WARMUP_CALLS):
            self.executer.bench(arr, 1)

    def _measure_per_call_ns(self, arr: list[float]) -> int:
        """Calibrate k for a ~50 ms inner loop, then take the median of
        _BENCH_RUNS timed inner loops. Returns nanoseconds per single call.

        With timing happening inside the runtime, the only noise sources
        left are CPU/cache contention; averaging over k iterations reduces
        them to a fraction of the per-call cost."""
        # Calibration: one inner loop at k=1 to estimate per-call cost.
        elapsed = max(1, self.executer.bench(arr, 1))
        k = max(_MIN_K, min(_MAX_K, _TARGET_BENCH_NS // elapsed))

        per_call: list[int] = []
        for _ in range(_BENCH_RUNS):
            elapsed = self.executer.bench(arr, k)
            per_call.append(max(1, elapsed // k))
        return int(statistics.median(per_call))

    def bench_exhaustive(self) -> dict[str, Any]:
        self._warmup()

        points: list[dict[str, int]] = []
        for n in _N_GRID:
            wall_start = time.perf_counter_ns()
            arr = _seeded_array(n)
            t = self._measure_per_call_ns(arr)
            del arr

            points.append({"n": n, "time_ns": t})

            if time.perf_counter_ns() - wall_start >= _WALL_BUDGET_NS:
                break

        return {"points": points, "complexity": analyze_dataset(points)}
