import numpy as np


def _r2_score(y_true: np.ndarray, y_pred: np.ndarray) -> float:
    ss_res = np.sum((y_true - y_pred) ** 2)
    ss_tot = np.sum((y_true - np.mean(y_true)) ** 2)
    if ss_tot == 0:
        return 0.0
    return 1.0 - (ss_res / ss_tot)


def _fit_linear(feature: np.ndarray, target: np.ndarray) -> tuple[float, float, float]:
    X = np.column_stack((feature, np.ones_like(feature)))
    coeffs, _, _, _ = np.linalg.lstsq(X, target, rcond=None)
    coef = float(coeffs[0])
    intercept = float(coeffs[1])
    y_pred = X @ coeffs
    r2 = _r2_score(target, y_pred)
    return coef, intercept, r2


def auto_determine_complexity(n_sizes: list[int], run_times: list[int]) -> dict[str, float | str]:
    n = np.array(n_sizes, dtype=float)
    t = np.array(run_times, dtype=float)

    if n.size != t.size:
        raise ValueError("n_sizes and run_times must have the same length")
    if np.any(n <= 0):
        raise ValueError("n_sizes must contain only positive values")

    candidates = {
        "O(1)": np.ones_like(n),
        "O(log n)": np.log2(n),
        "O(n)": n,
        "O(n log n)": n * np.log2(n),
        "O(n^2)": n**2,
        "O(n^2 log n)": (n**2) * np.log2(n),
        "O(n^3)": n**3,
        "O(2^n)": np.clip(2.0**n, 0, 1e308),
    }

    results = []
    for label, feature in candidates.items():
        coef, intercept, r2 = _fit_linear(feature, t)
        results.append({
            "class": label,
            "r2": r2,
            "coef": coef,
            "intercept": intercept,
        })

    results.sort(key=lambda x: x["r2"], reverse=True)
    return results[0]


def analyze_dataset(points: list[dict[str, int]]) -> dict[str, float | str]:
    n_sizes = [point["n"] for point in points]
    run_times = [point["time_ns"] for point in points]
    return auto_determine_complexity(n_sizes, run_times)
