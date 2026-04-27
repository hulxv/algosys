package com.algosys.model;

public class AnalysisRequest {
    public final int mode;
    public final String code;
    public final int[] array;

    public AnalysisRequest(int mode, String code, int[] array) {
        this.mode = mode;
        this.code = code;
        this.array = array;
    }
}
