package com.algosys.model;

public class AnalysisResult {
    public final int mode;
    public final double[] inputSizes;
    public final double[] executionTimesMs;
    public final String complexityClass;
    public final double r2;
    public final double coef;
    public final double intercept;

    public AnalysisResult(
            int mode,
            double[] inputSizes,
            double[] executionTimesMs,
            String complexityClass,
            double r2,
            double coef,
            double intercept
    ) {
        this.mode = mode;
        this.inputSizes = inputSizes;
        this.executionTimesMs = executionTimesMs;
        this.complexityClass = complexityClass;
        this.r2 = r2;
        this.coef = coef;
        this.intercept = intercept;
    }

    public double minTimeMs() {
        if (executionTimesMs.length == 0) {
            return 0;
        }
        double min = executionTimesMs[0];
        for (double value : executionTimesMs) {
            min = Math.min(min, value);
        }
        return min;
    }

    public double maxTimeMs() {
        if (executionTimesMs.length == 0) {
            return 0;
        }
        double max = executionTimesMs[0];
        for (double value : executionTimesMs) {
            max = Math.max(max, value);
        }
        return max;
    }

    public int dataPoints() {
        return Math.min(inputSizes.length, executionTimesMs.length);
    }
}
