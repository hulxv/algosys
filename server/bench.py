import time
import random
from typing import Any
from analyzer import analyze_dataset
from executer import Executer


class Bench:
    def __init__(self, executer: Executer):
        self.executer = executer
        executer.load()

    def bench_array(self, arr: list[Any]) -> int:
        """Benchmark with a specific array. Returns elapsed time in nanoseconds."""
        start = time.perf_counter_ns()
        self.executer.call(arr)
        return time.perf_counter_ns() - start

    def bench_random(self, n: int) -> int:
        """Benchmark with n random floats. Returns elapsed time in nanoseconds."""
        arr = [random.random() for _ in range(n)]
        return self.bench_array(arr)

    def bench_exhaustive(self, sizes: list[int]) -> dict[str, Any]:
        """Run bench_random for each size. Returns points and complexity analysis."""
        points = [{"n": n, "time_ns": self.bench_random(n)} for n in sizes]
        complexity = analyze_dataset(points)
        return {"points": points, "complexity": complexity}
