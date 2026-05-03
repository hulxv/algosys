package com.algosys.model;

public class AnalysisRequest {
    public final int mode;
    public final String lang;
    public final String func;
    public final String code;

    public AnalysisRequest(int mode, String lang, String func, String code) {
        this.mode = mode;
        this.lang = lang;
        this.func = func;
        this.code = code;
    }
}
