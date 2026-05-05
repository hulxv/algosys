package com.algosys.controller;

import java.util.Map;

import com.algosys.model.AnalysisResult;
import com.algosys.util.EventBus;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class RightPanelController {
    private static final String CONSTANT_COLOR = "#7f8aa3";
    private static final String FAST_COLOR = "#00c7c7";
    private static final String WARNING_COLOR = "#f2b84b";
    private static final String SLOW_COLOR = "#ff5f57";
    private static final String MANUAL_COLOR = "#a6b0c7";

    @FXML
    private VBox rightPanelRoot;
    
    @FXML
    private LineChart<Number, Number> executionChart;
    
    @FXML
    private NumberAxis xAxis;
    
    @FXML
    private NumberAxis yAxis;
    
    @FXML
    private Label minTimeValue;
    
    @FXML
    private Label maxTimeValue;
    
    @FXML
    private Label dataPointsValue;
    
    @FXML
    private Label modeValue;
    
    @FXML
    private Label ratioValue;

    @FXML
    private Label complexityValue;

    @FXML
    private Label complexityName;

    @FXML
    private Label complexityDescription;

    @FXML
    private Label chartComplexityValue;

    @FXML private HBox constantPill;
    @FXML private HBox logPill;
    @FXML private HBox linearPill;
    @FXML private HBox linearithmicPill;
    @FXML private HBox quadraticPill;
    @FXML private HBox cubicPill;
    @FXML private HBox exponentialPill;
    @FXML private Label constantDot;
    @FXML private Label logDot;
    @FXML private Label linearDot;
    @FXML private Label linearithmicDot;
    @FXML private Label quadraticDot;
    @FXML private Label cubicDot;
    @FXML private Label exponentialDot;
    @FXML private Label constantLabel;
    @FXML private Label logLabel;
    @FXML private Label linearLabel;
    @FXML private Label linearithmicLabel;
    @FXML private Label quadraticLabel;
    @FXML private Label cubicLabel;
    @FXML private Label exponentialLabel;

    @FXML
    public void initialize() {
        EventBus.getInstance().subscribe("analysis-complete", data -> {
            javafx.application.Platform.runLater(() -> updateAnalysisResults(data));
        });
        
        // Configure chart appearance
        configureBrand();
    }

    private void configureBrand() {
        executionChart.setLegendVisible(false);
        executionChart.setStyle("-fx-padding: 0;");
    }

    /**
     * Update the analysis results display with data from the backend.
     */
    public void updateAnalysisResults(Object data) {
        if (data instanceof AnalysisResult result) {
            minTimeValue.setText(formatTime(result.minTimeMs()));
            maxTimeValue.setText(formatTime(result.maxTimeMs()));
            dataPointsValue.setText(String.valueOf(result.dataPoints()));
            modeValue.setText("Mode " + result.mode);

            double ratio = result.minTimeMs() > 0 ? result.maxTimeMs() / result.minTimeMs() : 0;
            ratioValue.setText(String.format("%.1fx", ratio));

            String color = complexityColor(result.complexityClass);
            complexityValue.setText(result.complexityClass);
            complexityValue.setStyle("-fx-text-fill: " + color + ";");
            complexityName.setText(complexityName(result.complexityClass));
            complexityName.setStyle("-fx-text-fill: " + color + ";");
            complexityDescription.setText(complexityDescription(result.complexityClass, result.r2));
            chartComplexityValue.setText(result.complexityClass);
            chartComplexityValue.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 700;");
            if (result.mode == 1) {
                clearReferenceHighlight();
            } else {
                updateReferenceHighlight(result.complexityClass, color);
            }

            updateChart(result);
        }
    }

    private void updateChart(AnalysisResult result) {
        executionChart.getData().clear();
        
        try {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Execution Time");

            double maxInputSize = 0;
            double maxExecutionTime = 0;

            for (int i = 0; i < result.dataPoints(); i++) {
                series.getData().add(new XYChart.Data<>(result.inputSizes[i], result.executionTimesMs[i]));
                maxInputSize = Math.max(maxInputSize, result.inputSizes[i]);
                maxExecutionTime = Math.max(maxExecutionTime, result.executionTimesMs[i]);
            }

            xAxis.setUpperBound(Math.max(1, maxInputSize * 1.1));
            yAxis.setUpperBound(Math.max(0.001, maxExecutionTime * 1.1));
            configureYAxisUnits(maxExecutionTime);

            executionChart.getData().add(series);
        } catch (Exception e) {
            System.err.println("Error updating chart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatTime(double ms) {
        if (ms < 0.001 && ms > 0) {
            return String.format("%.0fns", ms * 1_000_000);
        }
        if (ms < 1) {
            return String.format("%.1fus", ms * 1_000);
        }
        return String.format("%.2fms", ms);
    }

    private void configureYAxisUnits(double maxExecutionTimeMs) {
        if (maxExecutionTimeMs < 0.001 && maxExecutionTimeMs > 0) {
            yAxis.setLabel("Execution Time (ns)");
            yAxis.setTickLabelFormatter(axisFormatter(1_000_000, "ns"));
        } else if (maxExecutionTimeMs < 1) {
            yAxis.setLabel("Execution Time (us)");
            yAxis.setTickLabelFormatter(axisFormatter(1_000, "us"));
        } else {
            yAxis.setLabel("Execution Time (ms)");
            yAxis.setTickLabelFormatter(axisFormatter(1, "ms"));
        }
    }

    private StringConverter<Number> axisFormatter(double multiplier, String unit) {
        return new StringConverter<>() {
            @Override
            public String toString(Number value) {
                double converted = value.doubleValue() * multiplier;
                if (converted >= 100 || converted == Math.rint(converted)) {
                    return String.format("%.0f%s", converted, unit);
                }
                if (converted >= 10) {
                    return String.format("%.1f%s", converted, unit);
                }
                return String.format("%.2f%s", converted, unit);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        };
    }

    private String complexityName(String value) {
        return switch (value) {
            case "O(1)" -> "Constant";
            case "O(log n)" -> "Logarithmic";
            case "O(n)" -> "Linear";
            case "O(n log n)" -> "Linearithmic";
            case "O(n^2)" -> "Quadratic";
            case "O(n^2 log n)" -> "Quadratic log";
            case "O(n^3)" -> "Cubic";
            case "O(2^n)" -> "Exponential";
            case "Manual" -> "Manual run";
            default -> "Unknown";
        };
    }

    private String complexityDescription(String value, double r2) {
        return switch (value) {
            case "O(1)" -> String.format("Runtime stayed nearly flat across measured input sizes. Fit R2 %.4f.", r2);
            case "O(log n)" -> String.format("Runtime grew slowly as input size increased. Fit R2 %.4f.", r2);
            case "O(n)" -> String.format("Time scaled proportionally with input size. Fit R2 %.4f.", r2);
            case "O(n log n)" -> String.format("Growth matched sorting-style n log n behavior. Fit R2 %.4f.", r2);
            case "O(n^2)" -> String.format("Time grew roughly with the square of input size. Fit R2 %.4f.", r2);
            case "O(n^2 log n)" -> String.format("Growth matched n squared log n behavior. Fit R2 %.4f.", r2);
            case "O(n^3)" -> String.format("Runtime grew cubically across the benchmark sweep. Fit R2 %.4f.", r2);
            case "O(2^n)" -> String.format("Runtime matched exponential growth on the measured points. Fit R2 %.4f.", r2);
            case "Manual" -> "Ran once on the array you entered. Use Mode 2 for Big O estimation.";
            default -> String.format("The API returned an unrecognized class. Fit R2 %.4f.", r2);
        };
    }

    private String complexityColor(String value) {
        return switch (value) {
            case "Manual" -> MANUAL_COLOR;
            case "O(1)" -> CONSTANT_COLOR;
            case "O(log n)", "O(n)" -> FAST_COLOR;
            case "O(n log n)" -> WARNING_COLOR;
            case "O(n^2)", "O(n^2 log n)", "O(n^3)", "O(2^n)" -> SLOW_COLOR;
            default -> FAST_COLOR;
        };
    }

    private void updateReferenceHighlight(String complexityClass, String color) {
        Map<String, ReferenceItem> items = Map.of(
                "O(1)", new ReferenceItem(constantPill, constantDot, constantLabel, CONSTANT_COLOR),
                "O(log n)", new ReferenceItem(logPill, logDot, logLabel, FAST_COLOR),
                "O(n)", new ReferenceItem(linearPill, linearDot, linearLabel, FAST_COLOR),
                "O(n log n)", new ReferenceItem(linearithmicPill, linearithmicDot, linearithmicLabel, WARNING_COLOR),
                "O(n^2)", new ReferenceItem(quadraticPill, quadraticDot, quadraticLabel, SLOW_COLOR),
                "O(n^3)", new ReferenceItem(cubicPill, cubicDot, cubicLabel, SLOW_COLOR),
                "O(2^n)", new ReferenceItem(exponentialPill, exponentialDot, exponentialLabel, SLOW_COLOR)
        );

        resetReferenceItems(items);

        ReferenceItem active = items.get(complexityClass);
        if (active != null) {
            active.label.getStyleClass().add("complexity-btn-active");
            active.pill.getStyleClass().add("complexity-pill-active");
            active.dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 8px;");
            active.label.setStyle("");
            active.pill.setStyle("-fx-border-color: " + color + "; -fx-background-color: rgba(255,255,255,0.04);");
        }
    }

    private void clearReferenceHighlight() {
        resetReferenceItems(Map.of(
                "O(1)", new ReferenceItem(constantPill, constantDot, constantLabel, CONSTANT_COLOR),
                "O(log n)", new ReferenceItem(logPill, logDot, logLabel, FAST_COLOR),
                "O(n)", new ReferenceItem(linearPill, linearDot, linearLabel, FAST_COLOR),
                "O(n log n)", new ReferenceItem(linearithmicPill, linearithmicDot, linearithmicLabel, WARNING_COLOR),
                "O(n^2)", new ReferenceItem(quadraticPill, quadraticDot, quadraticLabel, SLOW_COLOR),
                "O(n^3)", new ReferenceItem(cubicPill, cubicDot, cubicLabel, SLOW_COLOR),
                "O(2^n)", new ReferenceItem(exponentialPill, exponentialDot, exponentialLabel, SLOW_COLOR)
        ));
    }

    private void resetReferenceItems(Map<String, ReferenceItem> items) {
        items.forEach((label, item) -> {
            item.label.getStyleClass().remove("complexity-btn-active");
            item.pill.getStyleClass().remove("complexity-pill-active");
            item.dot.setStyle("-fx-text-fill: " + item.defaultColor + "; -fx-font-size: 8px;");
            item.label.setStyle("");
            item.pill.setStyle("");
        });
    }

    private record ReferenceItem(HBox pill, Label dot, Label label, String defaultColor) {
    }
}
