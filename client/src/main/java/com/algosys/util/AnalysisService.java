package com.algosys.util;

import com.algosys.model.AnalysisRequest;
import com.algosys.model.AnalysisResult;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalysisService {
    private static final String BASE_URL = "http://localhost:5000";
    private static final Pattern POINT_PATTERN = Pattern.compile(
            "\\{\\s*\"n\"\\s*:\\s*(\\d+)\\s*,\\s*\"time_ns\"\\s*:\\s*(\\d+)\\s*}"
    );

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public AnalysisService() {
        EventBus.getInstance().subscribe("run-analysis", payload -> {
            if (payload instanceof AnalysisRequest request) {
                analyze(request);
            }
        });
        loadLanguages();
    }

    private void loadLanguages() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/loaders"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        publish("loaders-loaded", parseLoaders(response.body()));
                    }
                })
                .exceptionally(error -> {
                    publish("analysis-log", "API offline: start the server on " + BASE_URL);
                    return null;
                });
    }

    private void analyze(AnalysisRequest requestPayload) {
        publish("analysis-started", requestPayload);
        publish("analysis-log", "POST /analyze mode=" + requestPayload.mode + " lang=" + requestPayload.lang + " func=" + requestPayload.func);

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/analyze"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestPayload)))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> handleAnalyzeResponse(requestPayload, response))
                .exceptionally(error -> {
                    publish("analysis-error", apiFailureMessage(error));
                    publish("analysis-finished", null);
                    return null;
                });
    }

    private void handleAnalyzeResponse(AnalysisRequest requestPayload, HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            publish("analysis-error", extractJsonString(response.body(), "error", "API returned HTTP " + response.statusCode()));
            publish("analysis-finished", null);
            return;
        }

        try {
            AnalysisResult result = parseResult(requestPayload.mode, response.body());
            publish("analysis-complete", result);
            publish("analysisComplete", result);
            if (result.mode == 1) {
                publish("analysis-log", "Manual output: " + result.manualOutput);
            }
        } catch (RuntimeException ex) {
            publish("analysis-error", "Could not parse API response: " + ex.getMessage());
        } finally {
            publish("analysis-finished", null);
        }
    }

    private AnalysisResult parseResult(int mode, String body) {
        List<Double> sizes = new ArrayList<>();
        List<Double> timesMs = new ArrayList<>();

        Matcher matcher = POINT_PATTERN.matcher(body);
        while (matcher.find()) {
            sizes.add(Double.parseDouble(matcher.group(1)));
            timesMs.add(Double.parseDouble(matcher.group(2)) / 1_000_000.0);
        }

        String complexity = extractJsonString(body, "class", "Unknown");
        double r2 = extractJsonDouble(body, "r2", 0);
        double coef = extractJsonDouble(body, "coef", 0);
        double intercept = extractJsonDouble(body, "intercept", 0);
        String manualOutput = requestModeIsManual(mode) ? extractJsonValue(body, "output", "null") : "";

        return new AnalysisResult(
                mode,
                toArray(sizes),
                toArray(timesMs),
                complexity,
                r2,
                coef,
                intercept,
                manualOutput
        );
    }

    private List<LoaderOption> parseLoaders(String body) {
        Pattern loaderPattern = Pattern.compile(
                "\\{\\s*\"tag\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"language\"\\s*:\\s*\"([^\"]+)\"\\s*}"
        );
        Matcher matcher = loaderPattern.matcher(body);
        List<LoaderOption> loaders = new ArrayList<>();
        while (matcher.find()) {
            loaders.add(new LoaderOption(unescape(matcher.group(1)), unescape(matcher.group(2))));
        }
        return loaders;
    }

    private String toJson(AnalysisRequest request) {
        return """
                {"mode":%d,"lang":"%s","func":"%s","code":"%s","array":%s}
                """.formatted(request.mode, escape(request.lang), escape(request.func), escape(request.code), arrayJson(request.array)).strip();
    }

    private String extractJsonString(String body, String key, String fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        return matcher.find() ? unescape(matcher.group(1)) : fallback;
    }

    private double extractJsonDouble(String body, String key, double fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)").matcher(body);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : fallback;
    }

    private String extractJsonValue(String body, String key, String fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\[[^\\]]*]|\"[^\"]*\"|-?\\d+(?:\\.\\d+)?|true|false|null)").matcher(body);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private boolean requestModeIsManual(int mode) {
        return mode == 1;
    }

    private double[] toArray(List<Double> values) {
        double[] result = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String arrayJson(int[] values) {
        if (values == null || values.length == 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        return builder.append(']').toString();
    }

    private String unescape(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current instanceof IOException ? "connection failed" : current.getMessage();
    }

    private String apiFailureMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        if (root instanceof HttpTimeoutException) {
            return "Analysis timed out. Slow algorithms such as Bubble Sort can exceed the client wait time.";
        }
        return "Could not reach API at " + BASE_URL + ": " + rootMessage(throwable);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void publish(String event, Object payload) {
        Platform.runLater(() -> EventBus.getInstance().publish(event, payload));
    }

    public record LoaderOption(String tag, String language) {
        @Override
        public String toString() {
            return language + " (" + tag + ")";
        }
    }
}
