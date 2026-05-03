import numpy as np

# Same constants as your original code to ensure no breaking changes
_EXPECTED_SLOPES: dict[str, float] = {
    "O(1)":       0.0,
    "O(log n)":   0.14,
    "O(n)":       1.0,
    "O(n log n)": 1.14,
    "O(n^2)":     2.0,
    "O(n^3)":     3.0,
}

# Tighter tolerance: ensures O(n) and O(n log n) aren't always forced into a tie-break.
_TIEBREAK_TOLERANCE = 0.05 
_O1_SPEARMAN_THRESHOLD = 0.7

def _r2_score(y_true: np.ndarray, y_pred: np.ndarray) -> float:
    ss_res = float(np.sum((y_true - y_pred) ** 2))
    ss_tot = float(np.sum((y_true - np.mean(y_true)) ** 2))
    return 1.0 - (ss_res / ss_tot) if ss_tot != 0 else 1.0

def _fit_linear(feature: np.ndarray, target: np.ndarray) -> tuple[float, float, float]:
    X = np.column_stack((feature, np.ones_like(feature)))
    coeffs, _, _, _ = np.linalg.lstsq(X, target, rcond=None)
    r2 = _r2_score(target, X @ coeffs)
    return float(coeffs[0]), float(coeffs[1]), r2

def _spearman(x: np.ndarray, y: np.ndarray) -> float:
    rx = np.argsort(np.argsort(x)).astype(float)
    ry = np.argsort(np.argsort(y)).astype(float)
    if np.std(rx) == 0 or np.std(ry) == 0: return 0.0
    return float(np.corrcoef(rx, ry)[0, 1])

def _get_global_log_slope(n: np.ndarray, t: np.ndarray) -> float:
    """Calculates the scaling exponent using global log-log regression."""
    log_n = np.log2(n)
    log_t = np.log2(t)
    X = np.column_stack((log_n, np.ones_like(log_n)))
    coeffs, _, _, _ = np.linalg.lstsq(X, log_t, rcond=None)
    return float(coeffs[0])

def auto_determine_complexity(n_sizes: list[int], run_times: list[int]) -> dict[str, float | str]:
    n = np.array(n_sizes, dtype=float)
    t = np.maximum(np.array(run_times, dtype=float), 1.0)

    if n.size != t.size: raise ValueError("n_sizes and run_times must have the same length")
    if n.size < 2: raise ValueError("need at least 2 points")

    # Noise check
    if _spearman(n, t) < _O1_SPEARMAN_THRESHOLD:
        coef, intercept, r2 = _fit_linear(np.ones_like(n), t)
        return {"class": "O(1)", "r2": r2, "coef": coef, "intercept": intercept}

    # IMPROVEMENT: Use global log-log slope instead of median pairwise
    slope = _get_global_log_slope(n, t)

    candidates = {
        "O(1)":       np.ones_like(n),
        "O(log n)":   np.log2(n),
        "O(n)":       n,
        "O(n log n)": n * np.log2(n),
        "O(n^2)":     n ** 2,
        "O(n^3)":     n ** 3,
    }
    
    fits: dict[str, dict[str, float]] = {}
    for label, feature in candidates.items():
        coef, intercept, r2 = _fit_linear(feature, t)
        fits[label] = {"r2": r2, "coef": coef, "intercept": intercept}

    # Tie-breaking logic (now with tighter tolerance to separate n from n log n)
    distances = {label: abs(exp - slope) for label, exp in _EXPECTED_SLOPES.items()}
    best_dist = min(distances.values())
    in_band = [label for label, d in distances.items() if d - best_dist < _TIEBREAK_TOLERANCE]
    chosen = max(in_band, key=lambda label: fits[label]["r2"])

    f = fits[chosen]
    return {"class": chosen, "r2": f["r2"], "coef": f["coef"], "intercept": f["intercept"]}

def analyze_dataset(points: list[dict[str, int]]) -> dict[str, float | str]:
    n_sizes   = [p["n"]       for p in points]
    run_times = [p["time_ns"] for p in points]
    return auto_determine_complexity(n_sizes, run_times)
