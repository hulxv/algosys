package com.algosys.controller;

import com.algosys.model.AnalysisRequest;
import com.algosys.util.EventBus;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LeftPanelController {
    private static final String BUBBLE_SORT = """
            public static void algorithm(int[] arr) {
                int n = arr.length;
                for (int i = 0; i < n - 1; i++) {
                    for (int j = 0; j < n - i - 1; j++) {
                        if (arr[j] > arr[j + 1]) {
                            int temp = arr[j];
                            arr[j] = arr[j + 1];
                            arr[j + 1] = temp;
                        }
                    }
                }
            }
            """;

    private static final String LINEAR_SEARCH = """
            public static int algorithm(int[] arr) {
                int target = arr[arr.length / 2];
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] == target) return i;
                }
                return -1;
            }
            """;

    private static final String BINARY_SEARCH = """
            public static int algorithm(int[] arr) {
                Arrays.sort(arr);
                int target = arr[arr.length / 2];
                int lo = 0, hi = arr.length - 1;
                while (lo <= hi) {
                    int mid = (lo + hi) / 2;
                    if (arr[mid] == target) return mid;
                    else if (arr[mid] < target) lo = mid + 1;
                    else hi = mid - 1;
                }
                return -1;
            }
            """;

    private static final String MERGE_SORT = """
            public static int[] algorithm(int[] arr) {
                if (arr.length <= 1) return arr;
                int mid = arr.length / 2;
                int[] left = algorithm(Arrays.copyOfRange(arr, 0, mid));
                int[] right = algorithm(Arrays.copyOfRange(arr, mid, arr.length));
                return merge(left, right);
            }
            private static int[] merge(int[] l, int[] r) {
                int[] res = new int[l.length + r.length];
                int i = 0, j = 0, k = 0;
                while (i < l.length && j < r.length)
                    res[k++] = l[i] < r[j] ? l[i++] : r[j++];
                while (i < l.length) res[k++] = l[i++];
                while (j < r.length) res[k++] = r[j++];
                return res;
            }
            """;

    private static final String ARRAY_ACCESS = """
            public static int algorithm(int[] arr) {
                return arr[0];
            }
            """;

    private static final String CUSTOM_ALGORITHM = """
            public static void algorithm(int[] arr) {
                // Write your algorithm here.
            }
            """;

    @FXML private ToggleButton codeTab;
    @FXML private ToggleButton arrayTab;
    @FXML private ToggleGroup tabGroup;
    @FXML private VBox codePane;
    @FXML private VBox arrayPane;
    @FXML private ScrollPane lineNumberScrollPane;
    @FXML private VBox lineNumberGutter;
    @FXML private ComboBox<String> languageSelector;
    @FXML private TextArea codeEditor;
    @FXML private FlowPane presetRow;
    @FXML private ToggleButton bubbleSortToggle;
    @FXML private ToggleButton linearSearchToggle;
    @FXML private ToggleButton binarySearchToggle;
    @FXML private ToggleButton mergeSortToggle;
    @FXML private ToggleButton arrayAccessToggle;
    @FXML private Button addAlgorithmButton;
    @FXML private Button removeAlgorithmButton;
    @FXML private HBox customAlgorithmForm;
    @FXML private TextField customAlgorithmNameField;
    @FXML private Button createAlgorithmButton;
    @FXML private ToggleGroup presetGroup;
    @FXML private TextArea arrayInput;
    @FXML private Label arrayErrorLabel;
    @FXML private FlowPane chipPane;
    @FXML private ScrollPane consoleScrollPane;
    @FXML private VBox consoleLines;
    @FXML private ToggleButton mode1Toggle;
    @FXML private ToggleButton mode2Toggle;
    @FXML private ToggleGroup modeGroup;
    @FXML private Label sizesLabel;
    @FXML private javafx.scene.control.Button runButton;

    private int[] parsedArray = {5, 3, 8, 1, 9, 2, 7, 4, 6};
    private int customAlgorithmCount;
    private final Map<ToggleButton, String> customAlgorithms = new HashMap<>();
    private boolean updatingLineNumbers;

    @FXML
    private void initialize() {
        configureTabs();
        configurePresetButtons();
        configureLanguageSelector();
        configureCodeEditor();
        configureCustomAlgorithmControls();
        configureArrayInput();
        configureModeButtons();
        configureRunButton();
        configureButtonAnimations();
        subscribeToEvents();

        codeTab.setSelected(true);
        mode1Toggle.setSelected(true);
        bubbleSortToggle.setSelected(true);
        setCodeTemplate(BUBBLE_SORT);
        parseArrayInput(arrayInput.getText());
    }

    private void configureTabs() {
        tabGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            updateActiveStyle(codeTab, "tab-btn-active", codeTab.isSelected());
            updateActiveStyle(arrayTab, "tab-btn-active", arrayTab.isSelected());
            switchPane(codeTab.isSelected() ? codePane : arrayPane);
        });
    }

    private void switchPane(Node targetPane) {
        Node oldPane = targetPane == codePane ? arrayPane : codePane;
        if (targetPane.isVisible()) {
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), oldPane);
        fadeOut.setFromValue(oldPane.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            oldPane.setVisible(false);
            oldPane.setManaged(false);
            targetPane.setOpacity(0);
            targetPane.setVisible(true);
            targetPane.setManaged(true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), targetPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void configurePresetButtons() {
        presetGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            updatePresetStyles();
            updateRemoveAlgorithmState();
            applyAlgorithmTemplate();
        });
    }

    private void applyAlgorithmTemplate() {
        Toggle toggle = presetGroup.getSelectedToggle();
        String lang = languageSelector != null ? languageSelector.getValue() : "Java";
        if (lang == null) lang = "Java";

        if (toggle instanceof ToggleButton tb && customAlgorithms.containsKey(tb)) {
            setCodeTemplate(customAlgorithms.get(tb));
        } else {
            setCodeTemplate(getAlgorithmTemplate(toggle, lang));
        }
    }

    private String getAlgorithmTemplate(Toggle toggle, String language) {
        if (toggle == bubbleSortToggle) {
            return getBubbleSortTemplate(language);
        } else if (toggle == linearSearchToggle) {
            return getLinearSearchTemplate(language);
        } else if (toggle == binarySearchToggle) {
            return getBinarySearchTemplate(language);
        } else if (toggle == mergeSortToggle) {
            return getMergeSortTemplate(language);
        } else if (toggle == arrayAccessToggle) {
            return getArrayAccessTemplate(language);
        }
        return "";
    }

    private String getBubbleSortTemplate(String language) {
        return switch (language) {
            case "Python" -> """
                def algorithm(arr):
                    n = len(arr)
                    for i in range(n - 1):
                        for j in range(0, n - i - 1):
                            if arr[j] > arr[j + 1]:
                                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                    return arr
                """;
            case "JavaScript" -> """
                function algorithm(arr) {
                    let n = arr.length;
                    for (let i = 0; i < n - 1; i++) {
                        for (let j = 0; j < n - i - 1; j++) {
                            if (arr[j] > arr[j + 1]) {
                                let temp = arr[j];
                                arr[j] = arr[j + 1];
                                arr[j + 1] = temp;
                            }
                        }
                    }
                    return arr;
                }
                """;
            case "C++" -> """
                void algorithm(std::vector<int>& arr) {
                    int n = arr.size();
                    for (int i = 0; i < n - 1; i++) {
                        for (int j = 0; j < n - i - 1; j++) {
                            if (arr[j] > arr[j + 1]) {
                                int temp = arr[j];
                                arr[j] = arr[j + 1];
                                arr[j + 1] = temp;
                            }
                        }
                    }
                }
                """;
            default -> BUBBLE_SORT;
        };
    }

    private String getLinearSearchTemplate(String language) {
        return switch (language) {
            case "Python" -> """
                def algorithm(arr):
                    target = arr[len(arr) // 2]
                    for i in range(len(arr)):
                        if arr[i] == target:
                            return i
                    return -1
                """;
            case "JavaScript" -> """
                function algorithm(arr) {
                    let target = arr[Math.floor(arr.length / 2)];
                    for (let i = 0; i < arr.length; i++) {
                        if (arr[i] === target) return i;
                    }
                    return -1;
                }
                """;
            case "C++" -> """
                int algorithm(const std::vector<int>& arr) {
                    int target = arr[arr.size() / 2];
                    for (int i = 0; i < arr.size(); i++) {
                        if (arr[i] == target) return i;
                    }
                    return -1;
                }
                """;
            default -> LINEAR_SEARCH;
        };
    }

    private String getBinarySearchTemplate(String language) {
        return switch (language) {
            case "Python" -> """
                def algorithm(arr):
                    arr.sort()
                    target = arr[len(arr) // 2]
                    left, right = 0, len(arr) - 1
                    while left <= right:
                        mid = left + (right - left) // 2
                        if arr[mid] == target:
                            return mid
                        elif arr[mid] < target:
                            left = mid + 1
                        else:
                            right = mid - 1
                    return -1
                """;
            case "JavaScript" -> """
                function algorithm(arr) {
                    arr.sort((a, b) => a - b);
                    let target = arr[Math.floor(arr.length / 2)];
                    let left = 0, right = arr.length - 1;
                    while (left <= right) {
                        let mid = Math.floor(left + (right - left) / 2);
                        if (arr[mid] === target) return mid;
                        if (arr[mid] < target) left = mid + 1;
                        else right = mid - 1;
                    }
                    return -1;
                }
                """;
            case "C++" -> """
                #include <algorithm>
                int algorithm(std::vector<int>& arr) {
                    std::sort(arr.begin(), arr.end());
                    int target = arr[arr.size() / 2];
                    int left = 0, right = arr.size() - 1;
                    while (left <= right) {
                        int mid = left + (right - left) / 2;
                        if (arr[mid] == target) return mid;
                        if (arr[mid] < target) left = mid + 1;
                        else right = mid - 1;
                    }
                    return -1;
                }
                """;
            default -> BINARY_SEARCH;
        };
    }

    private String getMergeSortTemplate(String language) {
        return switch (language) {
            case "Python" -> """
                def merge(left, right):
                    res = []
                    i = j = 0
                    while i < len(left) and j < len(right):
                        if left[i] < right[j]:
                            res.append(left[i])
                            i += 1
                        else:
                            res.append(right[j])
                            j += 1
                    res.extend(left[i:])
                    res.extend(right[j:])
                    return res

                def algorithm(arr):
                    if len(arr) <= 1:
                        return arr
                    mid = len(arr) // 2
                    left = algorithm(arr[:mid])
                    right = algorithm(arr[mid:])
                    return merge(left, right)
                """;
            case "JavaScript" -> """
                function merge(left, right) {
                    let res = [], i = 0, j = 0;
                    while (i < left.length && j < right.length) {
                        if (left[i] < right[j]) res.push(left[i++]);
                        else res.push(right[j++]);
                    }
                    return res.concat(left.slice(i)).concat(right.slice(j));
                }

                function algorithm(arr) {
                    if (arr.length <= 1) return arr;
                    let mid = Math.floor(arr.length / 2);
                    let left = algorithm(arr.slice(0, mid));
                    let right = algorithm(arr.slice(mid));
                    return merge(left, right);
                }
                """;
            case "C++" -> """
                std::vector<int> merge(std::vector<int>& left, std::vector<int>& right) {
                    std::vector<int> res;
                    int i = 0, j = 0;
                    while (i < left.size() && j < right.size()) {
                        if (left[i] < right[j]) res.push_back(left[i++]);
                        else res.push_back(right[j++]);
                    }
                    while (i < left.size()) res.push_back(left[i++]);
                    while (j < right.size()) res.push_back(right[j++]);
                    return res;
                }

                std::vector<int> algorithm(std::vector<int> arr) {
                    if (arr.size() <= 1) return arr;
                    int mid = arr.size() / 2;
                    std::vector<int> left(arr.begin(), arr.begin() + mid);
                    std::vector<int> right(arr.begin() + mid, arr.end());
                    left = algorithm(left);
                    right = algorithm(right);
                    return merge(left, right);
                }
                """;
            default -> MERGE_SORT;
        };
    }

    private String getArrayAccessTemplate(String language) {
        return switch (language) {
            case "Python" -> """
                def algorithm(arr):
                    return arr[0]
                """;
            case "JavaScript" -> """
                function algorithm(arr) {
                    return arr[0];
                }
                """;
            case "C++" -> """
                int algorithm(const std::vector<int>& arr) {
                    return arr[0];
                }
                """;
            default -> ARRAY_ACCESS;
        };
    }

    private String getCustomAlgorithmTemplate(String language) {
        return switch (language) {
            case "Python" -> """
                def algorithm(arr):
                    # Write your algorithm here
                    pass
                """;
            case "JavaScript" -> """
                function algorithm(arr) {
                    // Write your algorithm here
                }
                """;
            case "C++" -> """
                void algorithm(std::vector<int>& arr) {
                    // Write your algorithm here
                }
                """;
            default -> CUSTOM_ALGORITHM;
        };
    }

    private void updatePresetStyles() {
        presetGroup.getToggles().stream()
                .filter(ToggleButton.class::isInstance)
                .map(ToggleButton.class::cast)
                .forEach(toggle -> updateActiveStyle(toggle, "preset-btn-active", toggle.isSelected()));
    }

    private void setCodeTemplate(String template) {
        codeEditor.setText(template.stripTrailing());
        updateLineNumbers();
    }

    private void configureCodeEditor() {
        codeEditor.textProperty().addListener((obs, oldValue, newValue) -> updateLineNumbers());
        codeEditor.scrollTopProperty().addListener((obs, oldValue, newValue) ->
                lineNumberGutter.setTranslateY(-newValue.doubleValue()));
        codeEditor.textProperty().addListener((obs, oldValue, newValue) -> saveSelectedCustomAlgorithm(newValue));
        codeEditor.addEventFilter(KeyEvent.KEY_PRESSED, this::handleCodeEditorKeyPress);
        codeEditor.addEventFilter(KeyEvent.KEY_TYPED, this::handleCodeEditorTyped);
        updateLineNumbers();
    }

    private void configureLanguageSelector() {
        if (languageSelector != null) {
            languageSelector.getItems().addAll("Java", "Python", "JavaScript", "C++");
            languageSelector.getSelectionModel().selectFirst();
            languageSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    applyAlgorithmTemplate();
                }
            });
        }
    }

    private void handleCodeEditorKeyPress(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.SLASH) {
            toggleLineComment();
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.BACK_SPACE && removePairedCharacters()) {
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.TAB) {
            codeEditor.replaceSelection("    ");
            event.consume();
            return;
        }

        if (event.getCode() == KeyCode.ENTER) {
            int caret = codeEditor.getCaretPosition();
            String text = codeEditor.getText();
            int lineStart = text.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
            String currentLine = text.substring(lineStart, caret);
            String indent = leadingWhitespace(currentLine);
            if (currentLine.trim().endsWith("{")) {
                indent += "    ";
            }
            codeEditor.replaceSelection(System.lineSeparator() + indent);
            event.consume();
        }
    }

    private void handleCodeEditorTyped(KeyEvent event) {
        if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }

        String character = event.getCharacter();
        if (character == null || character.length() != 1) {
            return;
        }

        String close = closingPair(character.charAt(0));
        if (close == null) {
            return;
        }

        int start = codeEditor.getSelection().getStart();
        String selected = codeEditor.getSelectedText();
        codeEditor.replaceSelection(character + selected + close);
        codeEditor.positionCaret(start + 1 + selected.length());
        event.consume();
    }

    private String closingPair(char open) {
        return switch (open) {
            case '(' -> ")";
            case '[' -> "]";
            case '{' -> "}";
            case '"' -> "\"";
            case '\'' -> "'";
            default -> null;
        };
    }

    private boolean removePairedCharacters() {
        int caret = codeEditor.getCaretPosition();
        String text = codeEditor.getText();
        if (caret <= 0 || caret >= text.length()) {
            return false;
        }

        char before = text.charAt(caret - 1);
        char after = text.charAt(caret);
        String close = closingPair(before);
        if (close == null || close.charAt(0) != after) {
            return false;
        }

        codeEditor.selectRange(caret - 1, caret + 1);
        codeEditor.replaceSelection("");
        return true;
    }

    private void toggleLineComment() {
        int caret = codeEditor.getCaretPosition();
        String text = codeEditor.getText();
        int lineStart = text.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
        int lineEnd = text.indexOf('\n', caret);
        if (lineEnd == -1) {
            lineEnd = text.length();
        }

        String line = text.substring(lineStart, lineEnd);
        int indentLength = leadingWhitespace(line).length();
        int commentStart = lineStart + indentLength;
        if (line.substring(indentLength).startsWith("//")) {
            codeEditor.selectRange(commentStart, Math.min(commentStart + 2, text.length()));
            codeEditor.replaceSelection("");
            codeEditor.positionCaret(Math.max(lineStart, caret - 2));
        } else {
            codeEditor.insertText(commentStart, "// ");
            codeEditor.positionCaret(caret + 3);
        }
    }

    private String leadingWhitespace(String line) {
        int index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return line.substring(0, index);
    }

    private void configureCustomAlgorithmControls() {
        addAlgorithmButton.setOnAction(e -> {
            customAlgorithmForm.setVisible(true);
            customAlgorithmForm.setManaged(true);
            customAlgorithmNameField.requestFocus();
        });

        removeAlgorithmButton.setOnAction(e -> removeSelectedAlgorithm());
        createAlgorithmButton.setOnAction(e -> createCustomAlgorithm());
        customAlgorithmNameField.setOnAction(e -> createCustomAlgorithm());
    }

    private void createCustomAlgorithm() {
        String name = customAlgorithmNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            customAlgorithmCount++;
            name = "Custom " + customAlgorithmCount;
        } else {
            name = name.trim();
        }

        ToggleButton customToggle = new ToggleButton(name);
        customToggle.setFocusTraversable(false);
        customToggle.setToggleGroup(presetGroup);
        customToggle.getStyleClass().add("preset-btn");
        String lang = languageSelector != null ? languageSelector.getValue() : "Java";
        customAlgorithms.put(customToggle, getCustomAlgorithmTemplate(lang != null ? lang : "Java").stripTrailing());

        int addButtonIndex = presetRow.getChildren().indexOf(addAlgorithmButton);
        presetRow.getChildren().add(Math.max(0, addButtonIndex), customToggle);
        configureButtonAnimation(customToggle);

        customAlgorithmNameField.clear();
        customAlgorithmForm.setVisible(false);
        customAlgorithmForm.setManaged(false);
        
        customToggle.setSelected(true);
        codeEditor.requestFocus();
    }

    private void removeSelectedAlgorithm() {
        if (!(presetGroup.getSelectedToggle() instanceof ToggleButton selected)) {
            return;
        }

        if (customAlgorithms.containsKey(selected)) {
            customAlgorithms.remove(selected);
        }
        
        if (presetRow.getChildren().contains(selected)) {
            presetRow.getChildren().remove(selected);
        }
        
        // Find the next available algorithm to select
        ToggleButton nextToSelect = (ToggleButton) presetRow.getChildren().stream()
                .filter(node -> node instanceof ToggleButton && node != addAlgorithmButton && node != removeAlgorithmButton)
                .findFirst()
                .orElse(null);

        if (nextToSelect != null) {
            nextToSelect.setSelected(true);
            updateRemoveAlgorithmState();
        } else {
            // No algorithms left
            presetGroup.selectToggle(null);
            codeEditor.clear();
            updateRemoveAlgorithmState();
        }
        addLog("✓", "#22c55e", "Removed algorithm.");
    }

    private void updateRemoveAlgorithmState() {
        boolean canRemove = presetGroup.getSelectedToggle() instanceof ToggleButton;
        removeAlgorithmButton.setDisable(!canRemove);
    }

    private void saveSelectedCustomAlgorithm(String code) {
        if (presetGroup.getSelectedToggle() instanceof ToggleButton selected && customAlgorithms.containsKey(selected)) {
            customAlgorithms.put(selected, code);
        }
    }

    private void updateLineNumbers() {
        if (updatingLineNumbers) {
            return;
        }
        updatingLineNumbers = true;
        int lines = Math.max(1, codeEditor.getText().split("\\R", -1).length);
        while (lineNumberGutter.getChildren().size() < lines) {
            Label label = new Label();
            label.getStyleClass().add("line-number-label");
            label.setMaxWidth(Double.MAX_VALUE);
            lineNumberGutter.getChildren().add(label);
        }
        while (lineNumberGutter.getChildren().size() > lines) {
            lineNumberGutter.getChildren().remove(lineNumberGutter.getChildren().size() - 1);
        }
        for (int i = 0; i < lines; i++) {
            ((Label) lineNumberGutter.getChildren().get(i)).setText(Integer.toString(i + 1));
        }
        updatingLineNumbers = false;
    }

    private void configureArrayInput() {
        arrayInput.textProperty().addListener((obs, oldValue, newValue) -> parseArrayInput(newValue));
    }

    private void parseArrayInput(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                parsedArray = new int[0];
                arrayErrorLabel.setVisible(false);
                arrayErrorLabel.setManaged(false);
                updateChips(parsedArray);
                return;
            }

            parsedArray = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();
            arrayErrorLabel.setVisible(false);
            arrayErrorLabel.setManaged(false);
            updateChips(parsedArray);
        } catch (NumberFormatException ex) {
            arrayErrorLabel.setVisible(true);
            arrayErrorLabel.setManaged(true);
        }
    }

    private void updateChips(int[] values) {
        chipPane.getChildren().clear();
        ParallelTransition chipTransitions = new ParallelTransition();

        for (int i = 0; i < values.length; i++) {
            Label chip = new Label(Integer.toString(values[i]));
            chip.getStyleClass().add("chip");
            chip.setOpacity(0);
            chipPane.getChildren().add(chip);

            PauseTransition pause = new PauseTransition(Duration.millis(i * 30.0));
            FadeTransition fade = new FadeTransition(Duration.millis(200), chip);
            fade.setFromValue(0);
            fade.setToValue(1);
            chipTransitions.getChildren().add(new SequentialTransition(pause, fade));
        }

        chipTransitions.play();
    }

    private void configureModeButtons() {
        sizesLabel.visibleProperty().bind(mode2Toggle.selectedProperty());
        sizesLabel.managedProperty().bind(mode2Toggle.selectedProperty());

        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            updateActiveStyle(mode1Toggle, "mode-btn-active", mode1Toggle.isSelected());
            updateActiveStyle(mode2Toggle, "mode-btn-active", mode2Toggle.isSelected());
            EventBus.getInstance().publish("mode-change", currentMode());
        });
    }

    private void configureRunButton() {
        runButton.setOnAction(e -> {
            ScaleTransition press = new ScaleTransition(Duration.millis(80), runButton);
            press.setToX(0.97);
            press.setToY(0.97);

            ScaleTransition release = new ScaleTransition(Duration.millis(80), runButton);
            release.setToX(1.0);
            release.setToY(1.0);
            press.setOnFinished(done -> release.play());
            press.play();

            int mode = currentMode();
            String code = codeEditor.getText();
            int[] requestArray = Arrays.copyOf(parsedArray, parsedArray.length);
            EventBus.getInstance().publish("run-analysis", new AnalysisRequest(mode, code, requestArray));
            addLog("▶", "#a855f7", "Starting analysis — Mode " + mode + "...");
        });
    }

    private void subscribeToEvents() {
        EventBus.getInstance().subscribe("analysis-log", payload ->
                Platform.runLater(() -> addLog("▶", "#a855f7", String.valueOf(payload))));
        EventBus.getInstance().subscribe("analysis-complete", payload ->
                Platform.runLater(() -> addLog("✓", "#22c55e", "Analysis complete: " + payload)));
    }

    private int currentMode() {
        return mode2Toggle.isSelected() ? 2 : 1;
    }

    private void addLog(String icon, String color, String message) {
        HBox line = new HBox(8);
        line.getStyleClass().add("console-line");
        line.setOpacity(0);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().addAll("console-icon", iconStyleClass(color));

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("console-message");
        messageLabel.setWrapText(true);

        line.getChildren().addAll(iconLabel, messageLabel);
        consoleLines.getChildren().add(line);

        FadeTransition fade = new FadeTransition(Duration.millis(200), line);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        Platform.runLater(() -> consoleScrollPane.setVvalue(1.0));
    }

    private void updateActiveStyle(Node node, String styleClass, boolean active) {
        if (active && !node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        } else if (!active) {
            node.getStyleClass().remove(styleClass);
        }
    }

    private String iconStyleClass(String color) {
        if ("#22c55e".equalsIgnoreCase(color)) {
            return "console-icon-success";
        }
        return "console-icon-start";
    }

    private void configureButtonAnimations() {
        ButtonBase[] buttons = {
                codeTab, arrayTab,
                bubbleSortToggle, linearSearchToggle, binarySearchToggle, mergeSortToggle, arrayAccessToggle,
                addAlgorithmButton, removeAlgorithmButton, createAlgorithmButton, mode1Toggle, mode2Toggle, runButton
        };
        for (ButtonBase button : buttons) {
            configureButtonAnimation(button);
        }
    }

    private void configureButtonAnimation(ButtonBase button) {
        button.setOnMouseEntered(e -> animateScale(button, 1.01, 1.01, 100));
        button.setOnMouseExited(e -> animateScale(button, 1.0, 1.0, 100));
        button.setOnMousePressed(e -> animateScale(button, 0.98, 0.98, 80));
        button.setOnMouseReleased(e -> animateScale(button, button.isHover() ? 1.01 : 1.0, button.isHover() ? 1.01 : 1.0, 80));
    }

    private void animateScale(Node node, double x, double y, int millis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(x);
        transition.setToY(y);
        transition.play();
    }
}
