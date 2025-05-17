package com.predixcode.sortvisualizer.ui;

import com.predixcode.sortvisualizer.algorithms.Algorithm;
import com.predixcode.sortvisualizer.algorithms.BubbleSort;
import com.predixcode.sortvisualizer.algorithms.InsertionSort;
import com.predixcode.sortvisualizer.algorithms.MergeSort;
import com.predixcode.sortvisualizer.algorithms.QuickSort;
import com.predixcode.sortvisualizer.algorithms.TreeSort;
import com.predixcode.sortvisualizer.core.SortController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

public class ControlPanel extends VBox {

    private SortController sortController;

    private ComboBox<AlgorithmItem> algorithmComboBox;
    private final Button startButton;
    private final Button resetButton;
    private final Button stopButton;
    private Button pauseResumeButton;
    private final Slider speedSlider;
    private TextField arraySizeField;
    private Label speedValueLabel;
    private CheckBox soundEnabledCheckbox;
    private Slider minFrequencySlider;
    private Slider maxFrequencySlider;
    private Label minFreqValueLabel;
    private Label maxFreqValueLabel;

    private boolean isPausedForButtonState = false;

    private static final List<Algorithm> AVAILABLE_ALGORITHMS = new ArrayList<>();

    static {
        AVAILABLE_ALGORITHMS.add(new BubbleSort());
        AVAILABLE_ALGORITHMS.add(new InsertionSort());
        AVAILABLE_ALGORITHMS.add(new QuickSort());
        AVAILABLE_ALGORITHMS.add(new MergeSort());
        AVAILABLE_ALGORITHMS.add(new TreeSort());
    }

    public ControlPanel() {
        this.setPadding(new Insets(10));
        this.setSpacing(15);
        this.setStyle("-fx-background-color: " + Theme.toHex(Theme.BACKGROUND_COLOR) + ";");
        this.setAlignment(Pos.TOP_CENTER);

        // --- Algorithm Section ---
        TitledPane algorithmPane = createSectionPane("Algorithm & Speed Controls");
        VBox algorithmControlsContainer = new VBox(10);
        algorithmControlsContainer.setPadding(new Insets(10));
        algorithmControlsContainer.setStyle("-fx-background-color: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR) + "; -fx-background-radius: 5;");

        Label algoLabel = new Label("Algorithm:");
        styleLabel(algoLabel);
        algorithmComboBox = new ComboBox<>();
        for (Algorithm algo : AVAILABLE_ALGORITHMS) {
            algorithmComboBox.getItems().add(new AlgorithmItem(algo));
        }
        algorithmComboBox.setPromptText("Select Algorithm");
        algorithmComboBox.setMinWidth(180);
        styleComboBox(algorithmComboBox);
        algorithmComboBox.setOnAction(event -> {
            AlgorithmItem selected = algorithmComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && sortController != null) {
                sortController.setAlgorithm(selected.getAlgorithm());
            }
        });
        HBox algoBox = new HBox(10, algoLabel, algorithmComboBox);
        algoBox.setAlignment(Pos.CENTER_LEFT);

        startButton = createStyledButton("Start Sort", Theme.SECONDARY_COLOR);
        startButton.setOnAction(event -> {
            if (sortController != null) {
                isPausedForButtonState = false;
                pauseResumeButton.setText("Pause");
                sortController.startSort();
            }
        });

        pauseResumeButton = createStyledButton("Pause", Theme.PRIMARY_COLOR);
        pauseResumeButton.setOnAction(event -> {
            if (sortController != null) {
                if (isPausedForButtonState) {
                    sortController.resumeSort();
                    pauseResumeButton.setText("Pause");
                    isPausedForButtonState = false;
                } else {
                    sortController.pauseSort();
                    pauseResumeButton.setText("Resume");
                    isPausedForButtonState = true;
                }
            }
        });
        pauseResumeButton.setDisable(true);

        stopButton = createStyledButton("Stop Sort", Theme.ACCENT_COLOR);
        stopButton.setOnAction(event -> {
            if (sortController != null) {
                sortController.stopSort();
                pauseResumeButton.setText("Pause");
                isPausedForButtonState = false;
            }
        });
        stopButton.setDisable(true);

        HBox mainActionButtonsBox = new HBox(10, startButton, pauseResumeButton, stopButton);
        mainActionButtonsBox.setAlignment(Pos.CENTER);
        
        Label speedLabel = new Label("Animation Delay (ms):");
        styleLabel(speedLabel);
        speedSlider = new Slider(1, 1000, 20);
        styleSlider(speedSlider);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(100);
        speedSlider.setBlockIncrement(10);
        speedSlider.setPrefWidth(500);
        speedValueLabel = new Label(String.format("%.0f ms", speedSlider.getValue()));
        styleLabel(speedValueLabel);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            speedValueLabel.setText(String.format("%.0f ms", newVal.doubleValue()));
            if (sortController != null) sortController.setAnimationDelay(newVal.intValue());
        });
        HBox speedBox = new HBox(10, speedLabel, speedSlider, speedValueLabel);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        speedBox.setPadding(new Insets(10, 0, 0, 0));

        algorithmControlsContainer.getChildren().addAll(algoBox, mainActionButtonsBox, speedBox);
        algorithmPane.setContent(algorithmControlsContainer);

        // --- Array Section ---
        TitledPane arrayPane = createSectionPane("Array Configuration");
        VBox arrayControlsContainer = new VBox(10);
        arrayControlsContainer.setPadding(new Insets(10));
        arrayControlsContainer.setStyle("-fx-background-color: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR) + "; -fx-background-radius: 5;");

        Label sizeLabel = new Label("Array Size (10-500):");
        styleLabel(sizeLabel);
        arraySizeField = new TextField(String.valueOf(SortController.DEFAULT_ARRAY_SIZE));
        styleTextField(arraySizeField);
        arraySizeField.setPrefWidth(60);
        
        resetButton = createStyledButton("Generate New Array", Theme.PRIMARY_COLOR);
        resetButton.setOnAction(event -> {
            if (sortController != null) {
                try {
                    int size = Integer.parseInt(arraySizeField.getText());
                    if (size < 10) size = 10;
                    if (size > 500) size = 500;
                    arraySizeField.setText(String.valueOf(size));
                    sortController.generateNewArray(size, SortController.DEFAULT_MIN_VALUE, SortController.DEFAULT_MAX_VALUE);
                    pauseResumeButton.setText("Pause");
                    isPausedForButtonState = false;
                } catch (NumberFormatException e) {
                    App.showAlert("Invalid Size", "Please enter a valid number for array size.");
                    arraySizeField.setText(String.valueOf(SortController.DEFAULT_ARRAY_SIZE));
                }
            }
        });
        HBox arrayConfigBox = new HBox(10, sizeLabel, arraySizeField, resetButton);
        arrayConfigBox.setAlignment(Pos.CENTER_LEFT);
        arrayControlsContainer.getChildren().add(arrayConfigBox);
        arrayPane.setContent(arrayControlsContainer);

        // --- Sound Section ---
        TitledPane soundPane = createSectionPane("Sound Configuration");
        GridPane soundControlsGrid = new GridPane();
        soundControlsGrid.setHgap(10);
        soundControlsGrid.setVgap(10);
        soundControlsGrid.setPadding(new Insets(10));
        soundControlsGrid.setStyle("-fx-background-color: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR) + "; -fx-background-radius: 5;");

        soundEnabledCheckbox = new CheckBox("Enable Sound");
        styleCheckBox(soundEnabledCheckbox);
        soundEnabledCheckbox.setSelected(true); 
        soundEnabledCheckbox.setOnAction(e -> {
            if (sortController != null) sortController.setSoundEnabled(soundEnabledCheckbox.isSelected());
        });
        soundControlsGrid.add(soundEnabledCheckbox, 0, 0, 3, 1);

        Label minFreqLabel = new Label("Min Freq (Hz):");
        styleLabel(minFreqLabel);
        minFrequencySlider = new Slider(50, 800, 220); 
        styleSlider(minFrequencySlider);
        minFrequencySlider.setPrefWidth(180);
        minFreqValueLabel = new Label(String.format("%.0f Hz", minFrequencySlider.getValue()));
        styleLabel(minFreqValueLabel);
        minFrequencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double newMinFreq = newVal.doubleValue();
            minFreqValueLabel.setText(String.format("%.0f Hz", newMinFreq));
            if (sortController != null) sortController.setMinToneFrequency(newMinFreq);
            if (newMinFreq >= maxFrequencySlider.getValue()) {
                maxFrequencySlider.setValue(newMinFreq + 50);
            }
        });
        soundControlsGrid.add(minFreqLabel, 0, 1);
        soundControlsGrid.add(minFrequencySlider, 1, 1);
        soundControlsGrid.add(minFreqValueLabel, 2, 1);

        Label maxFreqLabel = new Label("Max Freq (Hz):");
        styleLabel(maxFreqLabel);
        maxFrequencySlider = new Slider(200, 2000, 1046);
        styleSlider(maxFrequencySlider);
        maxFrequencySlider.setPrefWidth(180);
        maxFreqValueLabel = new Label(String.format("%.0f Hz", maxFrequencySlider.getValue()));
        styleLabel(maxFreqValueLabel);
        maxFrequencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double newMaxFreq = newVal.doubleValue();
            maxFreqValueLabel.setText(String.format("%.0f Hz", newMaxFreq));
            if (sortController != null) sortController.setMaxToneFrequency(newMaxFreq);
            if (newMaxFreq <= minFrequencySlider.getValue()) {
                 minFrequencySlider.setValue(newMaxFreq - 50);
            }
        });
        soundControlsGrid.add(maxFreqLabel, 0, 2);
        soundControlsGrid.add(maxFrequencySlider, 1, 2);
        soundControlsGrid.add(maxFreqValueLabel, 2, 2);
        
        soundPane.setContent(soundControlsGrid);

        this.getChildren().addAll(algorithmPane, arrayPane, soundPane);
    }
    
    private void styleLabel(Label label) {
        label.setTextFill(Theme.TEXT_COLOR_LIGHT);
        label.setFont(Theme.FONT_DEFAULT);
    }

    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setStyle(
            "-fx-text-fill: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT) + "; " +
            "-fx-font-family: 'Arial'; " +
            "-fx-mark-color: " + Theme.toHex(Theme.SECONDARY_COLOR) + "; " +
            "-fx-focus-color: " + Theme.toHex(Theme.ACCENT_COLOR) + ";"
        );
    }

    private void styleComboBox(ComboBox<AlgorithmItem> comboBox) {
        String textFillLight = Theme.toHex(Theme.TEXT_COLOR_LIGHT);
        String panelBgBrighter = Theme.toHex(Theme.PANEL_BACKGROUND_COLOR.brighter());
        String panelBg = Theme.toHex(Theme.PANEL_BACKGROUND_COLOR);
        String borderFocusedColor = Theme.toHex(Theme.PRIMARY_COLOR);
        String borderNormalColor = Theme.toHex(Theme.TEXT_COLOR_LIGHT.darker());

        String baseStyle = "-fx-font-family: 'Arial'; " +
                           "-fx-background-radius: 4px; " +
                           "-fx-border-radius: 4px; " +
                           "-fx-border-width: 1px; ";

        comboBox.setStyle(baseStyle +
                          "-fx-background-color: " + panelBgBrighter + "; " +
                          "-fx-text-fill: " + textFillLight + "; " +
                          "-fx-border-color: " + borderNormalColor + ";");
        
        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                comboBox.setStyle(baseStyle +
                                  "-fx-background-color: " + panelBgBrighter + "; " +
                                  "-fx-text-fill: " + textFillLight + "; " +
                                  "-fx-border-color: " + borderFocusedColor + ";" +
                                  "-fx-effect: dropshadow(gaussian, " + borderFocusedColor + ", 5, 0.2, 0, 0);");
            } else {
                comboBox.setStyle(baseStyle +
                                  "-fx-background-color: " + panelBgBrighter + "; " +
                                  "-fx-text-fill: " + textFillLight + "; " +
                                  "-fx-border-color: " + borderNormalColor + ";");
            }
        });

        comboBox.setCellFactory(lv -> {
            ListCell<AlgorithmItem> cell = new ListCell<AlgorithmItem>() {
                @Override
                protected void updateItem(AlgorithmItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: " + panelBg + "; -fx-text-fill: " + textFillLight + ";");
                    } else {
                        setText(item.toString());
                        setTextFill(Theme.TEXT_COLOR_LIGHT);
                        setStyle("-fx-background-color: " + panelBg + "; -fx-padding: 5px 8px;");
                    }
                }
            };
            cell.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered && !cell.isEmpty()) {
                    cell.setStyle("-fx-background-color: " + Theme.toHex(Theme.PRIMARY_COLOR) + "; -fx-text-fill: white; -fx-padding: 5px 8px;");
                } else if (!cell.isEmpty()){
                    cell.setStyle("-fx-background-color: " + panelBg + "; -fx-text-fill: " + textFillLight + "; -fx-padding: 5px 8px;");
                }
            });
            return cell;
        });
        
        comboBox.setButtonCell(new ListCell<AlgorithmItem>() {
            @Override
            protected void updateItem(AlgorithmItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(comboBox.getPromptText() != null ? comboBox.getPromptText() : "");
                    setTextFill(Theme.TEXT_COLOR_LIGHT.deriveColor(0, 1.0, 0.75, 1.0)); 
                } else {
                    setText(item.toString());
                    setTextFill(Theme.TEXT_COLOR_LIGHT);
                }
                setPadding(new Insets(3,5,3,5));
            }
        });
    }
    
    private void styleTextField(TextField textField) {
        String panelBgBrighter = Theme.toHex(Theme.PANEL_BACKGROUND_COLOR.brighter());
        String textLight = Theme.toHex(Theme.TEXT_COLOR_LIGHT);
        String borderNormal = Theme.toHex(Theme.TEXT_COLOR_LIGHT.darker());
        String borderFocused = Theme.toHex(Theme.PRIMARY_COLOR);

        String baseStyle = "-fx-font-family: 'Arial'; " +
                           "-fx-text-fill: " + textLight + "; " +
                           "-fx-background-radius: 4px; " +
                           "-fx-border-radius: 4px; " +
                           "-fx-border-width: 1px; " +
                           "-fx-prompt-text-fill: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT.deriveColor(0,1.0,0.7,1.0)) + "; " +
                           "-fx-highlight-fill: " + Theme.toHex(Theme.PRIMARY_COLOR.brighter()) + ";";
        
        textField.setStyle(baseStyle + "-fx-background-color: " + panelBgBrighter + "; -fx-border-color: " + borderNormal + ";");

        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                textField.setStyle(baseStyle + "-fx-background-color: " + panelBgBrighter + "; -fx-border-color: " + borderFocused + "; -fx-effect: dropshadow(gaussian, " + borderFocused + ", 5, 0.2, 0, 0);");
            } else {
                textField.setStyle(baseStyle + "-fx-background-color: " + panelBgBrighter + "; -fx-border-color: " + borderNormal + ";");
            }
        });
    }

    private void styleSlider(Slider slider) {
        slider.setStyle(
            "-fx-tick-label-fill: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT) + "; " +
            "-fx-base: " + Theme.toHex(Theme.PRIMARY_COLOR) + ";" // Hint for thumb color
        );
    }

    private TitledPane createSectionPane(String title) {
        TitledPane titledPane = new TitledPane();
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        // CHANGE: Title text color to light grey
        titleLabel.setTextFill(Theme.TEXT_COLOR_LIGHT); 
        titledPane.setGraphic(titleLabel); 
        titledPane.setCollapsible(true);
        titledPane.setExpanded(true);

        // Style the TitledPane header.
        // -fx-control-inner-background is often used for the header background in Modena.
        // Alternatively, -fx-base can influence it.
        // -fx-text-fill for general text in header (if not using graphic).
        // -fx-mark-color for the arrow.
        titledPane.setStyle(
            // "-fx-control-inner-background: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR.darker()) + "; " + // More specific for header
            "-fx-base: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR.darker()) + "; " + // General base color for header
            "-fx-text-fill: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT) + "; " + // General text fill for header
            "-fx-mark-color: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT) + ";"  // Arrow color to light grey
            // "-fx-border-color: " + Theme.toHex(Theme.BACKGROUND_COLOR) + "; " + 
            // "-fx-border-width: 0 0 1px 0;" // Bottom border for separation
        );
        // The content area (VBox/GridPane) inside the TitledPane is styled separately.
        return titledPane;
    }

    private Button createStyledButton(String text, Color bgColor) {
        Button button = new Button(text);
        String hexColor = Theme.toHex(bgColor);
        String baseStyle = 
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Arial'; " +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 4px;" +
            "-fx-padding: 7px 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0.1, 2, 2);";
        
        button.setStyle("-fx-background-color: " + hexColor + ";" + baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: " + Theme.toHex(bgColor.deriveColor(0, 1.0, 0.85, 1.0)) + ";" + baseStyle
        ));
        button.setOnMouseExited(e -> button.setStyle(
             "-fx-background-color: " + hexColor + ";" + baseStyle
        ));
        return button;
    }

    public void setSortController(SortController controller) {
        this.sortController = controller;
        if (this.sortController != null) {
            this.sortController.setAnimationDelay((int) speedSlider.getValue());
            if (!algorithmComboBox.getItems().isEmpty()) {
                 algorithmComboBox.getSelectionModel().selectFirst();
                 if (algorithmComboBox.getSelectionModel().getSelectedItem() != null) {
                    this.sortController.setAlgorithm(algorithmComboBox.getSelectionModel().getSelectedItem().getAlgorithm());
                 }
            }
            minFrequencySlider.setValue(this.sortController.getCurrentMinToneFrequency());
            maxFrequencySlider.setValue(this.sortController.getCurrentMaxToneFrequency());
            soundEnabledCheckbox.setSelected(this.sortController.isSoundEnabled());
            
            this.sortController.setMinToneFrequency(minFrequencySlider.getValue());
            this.sortController.setMaxToneFrequency(maxFrequencySlider.getValue());
        }
    }

    public void disableControlsDuringSort() {
        algorithmComboBox.setDisable(true);
        arraySizeField.setDisable(true);
        resetButton.setDisable(true);
        startButton.setDisable(true);
        pauseResumeButton.setDisable(false);
        stopButton.setDisable(false);
        minFrequencySlider.setDisable(true);
        maxFrequencySlider.setDisable(true);
        soundEnabledCheckbox.setDisable(true);
    }

    public void enableControls() {
        algorithmComboBox.setDisable(false);
        arraySizeField.setDisable(false);
        resetButton.setDisable(false);
        startButton.setDisable(false);
        pauseResumeButton.setDisable(true);
        pauseResumeButton.setText("Pause");
        isPausedForButtonState = false;
        stopButton.setDisable(true);
        speedSlider.setDisable(false);
        minFrequencySlider.setDisable(false);
        maxFrequencySlider.setDisable(false);
        soundEnabledCheckbox.setDisable(false);
    }

    private static class AlgorithmItem {
        private final Algorithm algorithm;
        public AlgorithmItem(Algorithm algorithm) { this.algorithm = algorithm; }
        public Algorithm getAlgorithm() { return algorithm; }
        @Override public String toString() { return algorithm.getName(); }
    }
}