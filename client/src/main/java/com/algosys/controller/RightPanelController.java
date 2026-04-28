package com.algosys.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import com.algosys.util.EventBus;

public class RightPanelController {

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
    public void initialize() {
        // Subscribe to analysis results from EventBus
        EventBus.getInstance().subscribe("analysisComplete", data -> {
            updateAnalysisResults(data);
        });
        
        // Configure chart appearance
        configureBrand();
    }

    private void configureBrand() {
        executionChart.setLegendVisible(false);
        executionChart.setStyle("-fx-padding: 0;");
    }

    /**
     * Update the analysis results display with data from the backend
     * Expected data format: Map with keys "executionTimes", "inputSizes", "minTime", "maxTime", "mode", etc.
     */
    public void updateAnalysisResults(Object data) {
        if (data instanceof java.util.Map) {
            java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) data;
            
            // Extract metrics
            double minTime = getValue(resultMap, "minTime", 0.0);
            double maxTime = getValue(resultMap, "maxTime", 0.0);
            int dataPoints = getValue(resultMap, "dataPoints", 0);
            int mode = getValue(resultMap, "mode", 1);
            
            // Update metric labels
            minTimeValue.setText(String.format("%.2fms", minTime));
            maxTimeValue.setText(String.format("%.2fms", maxTime));
            dataPointsValue.setText(String.valueOf(dataPoints));
            modeValue.setText("Mode " + mode);
            
            // Calculate and display ratio
            double ratio = minTime > 0 ? maxTime / minTime : 0;
            ratioValue.setText(String.format("%.1fx", ratio));
            
            // Update chart data
            updateChart(resultMap);
        }
    }

    private void updateChart(java.util.Map<String, Object> data) {
        executionChart.getData().clear();
        
        try {
            Object executionTimesObj = data.get("executionTimes");
            Object inputSizesObj = data.get("inputSizes");
            
            if (executionTimesObj instanceof double[] && inputSizesObj instanceof double[]) {
                double[] executionTimes = (double[]) executionTimesObj;
                double[] inputSizes = (double[]) inputSizesObj;
                
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Execution Time");
                
                // Find min and max for axis scaling
                double maxInputSize = 0;
                double maxExecutionTime = 0;
                
                for (int i = 0; i < Math.min(executionTimes.length, inputSizes.length); i++) {
                    series.getData().add(new XYChart.Data<>(inputSizes[i], executionTimes[i]));
                    maxInputSize = Math.max(maxInputSize, inputSizes[i]);
                    maxExecutionTime = Math.max(maxExecutionTime, executionTimes[i]);
                }
                
                // Update axis bounds with some padding
                xAxis.setUpperBound(maxInputSize * 1.1);
                yAxis.setUpperBound(maxExecutionTime * 1.1);
                
                executionChart.getData().add(series);
            }
        } catch (Exception e) {
            System.err.println("Error updating chart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(java.util.Map<String, Object> map, String key, T defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}