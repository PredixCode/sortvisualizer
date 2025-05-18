package com.predixcode.sortvisualizer.ui;

import com.predixcode.sortvisualizer.algorithms.Algorithm;
import com.predixcode.sortvisualizer.algorithms.BogoSort;
import com.predixcode.sortvisualizer.algorithms.BubbleSort;
import com.predixcode.sortvisualizer.algorithms.CocktailSort;
import com.predixcode.sortvisualizer.algorithms.HeapSort;
import com.predixcode.sortvisualizer.algorithms.InsertionSort;
import com.predixcode.sortvisualizer.algorithms.MergeSort;
import com.predixcode.sortvisualizer.algorithms.QuickSort;
import com.predixcode.sortvisualizer.algorithms.ShellSort;
import com.predixcode.sortvisualizer.algorithms.TreeSort;
import com.predixcode.sortvisualizer.core.SortController;
import com.predixcode.sortvisualizer.sound.MusicalNote; // Import new enum
import com.predixcode.sortvisualizer.sound.ScaleType;   // Import new enum

import javafx.collections.FXCollections;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
    
    // Frequency sliders (for linear mode)
    private Slider minFrequencySlider;
    private Slider maxFrequencySlider;
    private Label minFreqValueLabel;
    private Label maxFreqValueLabel;
    private HBox linearFrequencyControlsBox; // To toggle visibility

    // New UI elements for musical scale and tone
    private ComboBox<ScaleType> scaleTypeComboBox;
    private ComboBox<MusicalNote> baseNoteComboBox;
    private ComboBox<Integer> baseOctaveComboBox;
    private Label scaleTypeLabel;
    private Label baseNoteLabel;
    private Label baseOctaveLabel;


    private boolean isPausedForButtonState = false;

    private static final List<Algorithm> AVAILABLE_ALGORITHMS = new ArrayList<>();

    static {
        AVAILABLE_ALGORITHMS.add(new BubbleSort());
        AVAILABLE_ALGORITHMS.add(new InsertionSort());
        AVAILABLE_ALGORITHMS.add(new QuickSort());
        AVAILABLE_ALGORITHMS.add(new MergeSort());
        AVAILABLE_ALGORITHMS.add(new TreeSort());
        AVAILABLE_ALGORITHMS.add(new BogoSort());
        AVAILABLE_ALGORITHMS.add(new CocktailSort());
        AVAILABLE_ALGORITHMS.add(new HeapSort());
        AVAILABLE_ALGORITHMS.add(new ShellSort());
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

        Label sizeLabel = new Label("Array Size (3-10000):");
        styleLabel(sizeLabel);
        arraySizeField = new TextField(String.valueOf(SortController.DEFAULT_ARRAY_SIZE));
        styleTextField(arraySizeField);
        arraySizeField.setPrefWidth(60);
        
        resetButton = createStyledButton("Generate New Array", Theme.PRIMARY_COLOR);
        resetButton.setOnAction(event -> {
            if (sortController != null) {
                try {
                    int size = Integer.parseInt(arraySizeField.getText());
                    if (size < 3) size = 3;
                    if (size > 10000) size = 10000;
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
            updateSoundControlStates(soundEnabledCheckbox.isSelected(), scaleTypeComboBox.getValue());
        });
        soundControlsGrid.add(soundEnabledCheckbox, 0, 0, 3, 1); // Span 3 columns

        // Scale Type
        scaleTypeLabel = new Label("Scale Type:");
        styleLabel(scaleTypeLabel);
        scaleTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(ScaleType.values()));
        styleComboBoxGeneric(scaleTypeComboBox);
        scaleTypeComboBox.setMinWidth(150);
        scaleTypeComboBox.setOnAction(e -> {
            if (sortController != null) {
                ScaleType selectedType = scaleTypeComboBox.getValue();
                sortController.setMusicalScaleType(selectedType);
                updateSoundControlStates(soundEnabledCheckbox.isSelected(), selectedType);
            }
        });
        soundControlsGrid.add(scaleTypeLabel, 0, 1);
        soundControlsGrid.add(scaleTypeComboBox, 1, 1, 2, 1); // Span 2 columns for combobox

        // Base Note
        baseNoteLabel = new Label("Base Note:");
        styleLabel(baseNoteLabel);
        baseNoteComboBox = new ComboBox<>(FXCollections.observableArrayList(MusicalNote.values()));
        styleComboBoxGeneric(baseNoteComboBox);
        baseNoteComboBox.setMinWidth(150);
        baseNoteComboBox.setOnAction(e -> {
            if (sortController != null) sortController.setMusicalBaseNote(baseNoteComboBox.getValue());
        });
        soundControlsGrid.add(baseNoteLabel, 0, 2);
        soundControlsGrid.add(baseNoteComboBox, 1, 2, 2, 1);

        // Base Octave
        baseOctaveLabel = new Label("Base Octave:");
        styleLabel(baseOctaveLabel);
        List<Integer> octaves = IntStream.rangeClosed(0, 8).boxed().collect(Collectors.toList());
        baseOctaveComboBox = new ComboBox<>(FXCollections.observableArrayList(octaves));
        styleComboBoxGeneric(baseOctaveComboBox);
        baseOctaveComboBox.setMinWidth(150);
        baseOctaveComboBox.setOnAction(e -> {
            if (sortController != null && baseOctaveComboBox.getValue() != null) {
                sortController.setMusicalBaseOctave(baseOctaveComboBox.getValue());
            }
        });
        soundControlsGrid.add(baseOctaveLabel, 0, 3);
        soundControlsGrid.add(baseOctaveComboBox, 1, 3, 2, 1);

        // Linear Frequency Controls (Min/Max Freq Sliders)
        Label minFreqLabel = new Label("Min Frequency (Hz):");
        styleLabel(minFreqLabel);
        minFrequencySlider = new Slider(20, 2000, 150);
        styleSlider(minFrequencySlider);
        minFrequencySlider.setPrefWidth(180);
        minFreqValueLabel = new Label(String.format("%.0f Hz", minFrequencySlider.getValue()));
        styleLabel(minFreqValueLabel);
        minFrequencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double newMinFreq = newVal.doubleValue();
            minFreqValueLabel.setText(String.format("%.0f Hz", newMinFreq));
            if (sortController != null) sortController.setMinToneFrequency(newMinFreq);
            if (newMinFreq >= maxFrequencySlider.getValue()) {
                maxFrequencySlider.setValue(newMinFreq + 50); // Ensure min < max
            }
        });
        

        Label maxFreqLabel = new Label("Max Frequency (Hz):");
        styleLabel(maxFreqLabel);
        maxFrequencySlider = new Slider(200, 5000, 2000);
        styleSlider(maxFrequencySlider);
        maxFrequencySlider.setPrefWidth(180);
        maxFreqValueLabel = new Label(String.format("%.0f Hz", maxFrequencySlider.getValue()));
        styleLabel(maxFreqValueLabel);
        maxFrequencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double newMaxFreq = newVal.doubleValue();
            maxFreqValueLabel.setText(String.format("%.0f Hz", newMaxFreq));
            if (sortController != null) sortController.setMaxToneFrequency(newMaxFreq);
            if (newMaxFreq <= minFrequencySlider.getValue()) {
                 minFrequencySlider.setValue(Math.max(50, newMaxFreq - 50)); // Ensure max > min
            }
        });
        
        // Group linear frequency controls for easier show/hide
        GridPane linearFreqGrid = new GridPane();
        linearFreqGrid.setHgap(10);
        linearFreqGrid.setVgap(10);
        linearFreqGrid.add(minFreqLabel, 0, 0);
        linearFreqGrid.add(minFrequencySlider, 1, 0);
        linearFreqGrid.add(minFreqValueLabel, 2, 0);
        linearFreqGrid.add(maxFreqLabel, 0, 1);
        linearFreqGrid.add(maxFrequencySlider, 1, 1);
        linearFreqGrid.add(maxFreqValueLabel, 2, 1);
        
        soundControlsGrid.add(linearFreqGrid, 0, 4, 3, 1); // Add to main sound grid, span 3 columns

        soundPane.setContent(soundControlsGrid);
        this.getChildren().addAll(algorithmPane, arrayPane, soundPane);

        // Initial state update for sound controls
        if (sortController != null) { // Should be called after sortController is set
             updateSoundControlStates(soundEnabledCheckbox.isSelected(), sortController.getCurrentScaleType());
        } else {
             updateSoundControlStates(soundEnabledCheckbox.isSelected(), ScaleType.MAJOR); // Default if no controller yet
        }
    }

    private void updateSoundControlStates(boolean soundEnabled, ScaleType currentScaleType) {
        boolean isLinear = soundEnabled && (currentScaleType == ScaleType.LINEAR_FREQUENCY);
        boolean isScaled = soundEnabled && (currentScaleType != ScaleType.LINEAR_FREQUENCY);

        // Enable/disable scale and tone controls
        scaleTypeComboBox.setDisable(!soundEnabled);
        baseNoteComboBox.setDisable(!isScaled);
        baseOctaveComboBox.setDisable(!isScaled);
        scaleTypeLabel.setDisable(!soundEnabled);
        baseNoteLabel.setDisable(!isScaled);
        baseOctaveLabel.setDisable(!isScaled);

        // Enable/disable linear frequency sliders
        minFrequencySlider.setDisable(!isLinear);
        maxFrequencySlider.setDisable(!isLinear);
        minFreqValueLabel.setDisable(!isLinear); // Assuming labels should also be disabled
        maxFreqValueLabel.setDisable(!isLinear);
        // The parent labels "Min Freq" and "Max Freq" can also be disabled if needed
        // For simplicity, their direct parent GridPane (linearFreqGrid) can be disabled.
        if (linearFrequencyControlsBox != null) { // If linearFrequencyControlsBox is used
            linearFrequencyControlsBox.setDisable(!isLinear);
        } else { // If individual components are in a grid
            // Accessing labels inside linearFreqGrid to disable them
            // This requires linearFreqGrid's children to be accessible or labels to be fields
        }
        
        // If sound is disabled, all sub-controls should be disabled
        if (!soundEnabled) {
            minFrequencySlider.setDisable(true);
            maxFrequencySlider.setDisable(true);
        }
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
    
    // Generic styling for ComboBoxes (used for ScaleType, MusicalNote, Integer)
    private <T> void styleComboBoxGeneric(ComboBox<T> comboBox) {
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
                          "-fx-text-fill: " + textFillLight + "; " + // For selected item text
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
            ListCell<T> cell = new ListCell<T>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: " + panelBg + "; -fx-text-fill: " + textFillLight + ";");
                    } else {
                        setText(item.toString()); // Assumes T has a meaningful toString()
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
        
        comboBox.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
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


    private void styleComboBox(ComboBox<AlgorithmItem> comboBox) { // Keep specific for AlgorithmItem if needed, or merge
        styleComboBoxGeneric(comboBox); // Use the generic styler
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
            "-fx-base: " + Theme.toHex(Theme.PRIMARY_COLOR) + ";" 
        );
    }

    private TitledPane createSectionPane(String title) {
        TitledPane titledPane = new TitledPane();
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Theme.TEXT_COLOR_LIGHT); 
        titledPane.setGraphic(titleLabel); 
        titledPane.setCollapsible(true);
        titledPane.setExpanded(true);
        titledPane.setStyle(
            "-fx-base: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR.darker()) + "; " + 
            "-fx-text-fill: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT) + "; " + 
            "-fx-mark-color: " + Theme.toHex(Theme.TEXT_COLOR_LIGHT) + ";"
        );
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
            // Initialize sound settings from controller
            soundEnabledCheckbox.setSelected(this.sortController.isSoundEnabled());
            
            // Initialize musical scale controls
            scaleTypeComboBox.setValue(this.sortController.getCurrentScaleType());
            baseNoteComboBox.setValue(this.sortController.getCurrentBaseNote());
            baseOctaveComboBox.setValue(this.sortController.getCurrentBaseOctave());

            // Initialize linear frequency sliders
            minFrequencySlider.setValue(this.sortController.getCurrentMinToneFrequency());
            maxFrequencySlider.setValue(this.sortController.getCurrentMaxToneFrequency());
            
            // Pass initial values to controller (or ensure controller already has them)
            this.sortController.setMusicalScaleType(scaleTypeComboBox.getValue());
            this.sortController.setMusicalBaseNote(baseNoteComboBox.getValue());
            this.sortController.setMusicalBaseOctave(baseOctaveComboBox.getValue());
            this.sortController.setMinToneFrequency(minFrequencySlider.getValue());
            this.sortController.setMaxToneFrequency(maxFrequencySlider.getValue());

            updateSoundControlStates(soundEnabledCheckbox.isSelected(), scaleTypeComboBox.getValue());
        }
    }

    public void disableControlsDuringSort() {
        algorithmComboBox.setDisable(true);
        arraySizeField.setDisable(true);
        resetButton.setDisable(true);
        startButton.setDisable(true);
        pauseResumeButton.setDisable(false);
        stopButton.setDisable(false);
        
        // Disable all sound configuration during sort
        soundEnabledCheckbox.setDisable(true);
        scaleTypeComboBox.setDisable(true);
        baseNoteComboBox.setDisable(true);
        baseOctaveComboBox.setDisable(true);
        minFrequencySlider.setDisable(true);
        maxFrequencySlider.setDisable(true);
        // Also disable labels if desired
        scaleTypeLabel.setDisable(true);
        baseNoteLabel.setDisable(true);
        baseOctaveLabel.setDisable(true);
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
        
        // Enable sound configuration according to current state
        soundEnabledCheckbox.setDisable(false);
        updateSoundControlStates(soundEnabledCheckbox.isSelected(), scaleTypeComboBox.getValue());
        // Re-enable labels
        scaleTypeLabel.setDisable(!soundEnabledCheckbox.isSelected());
        // Further refinement in updateSoundControlStates for baseNote/Octave labels
    }
    
    public Tab getSessionManagerTab() {
        Tab sessionTab = new Tab("Session Manager");
        sessionTab.setClosable(false);
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: " + Theme.toHex(Theme.PANEL_BACKGROUND_COLOR) + ";");
        Label titleLabel = new Label("Session Management");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Theme.TEXT_COLOR_LIGHT);
        Button saveSessionButton = createStyledButton("Save Current Session", Theme.PRIMARY_COLOR);
        Button loadSessionButton = createStyledButton("Load Session", Theme.SECONDARY_COLOR);
        content.getChildren().addAll(titleLabel, saveSessionButton, loadSessionButton);
        sessionTab.setContent(content);
        return sessionTab;
    }
    
    public void selectAlgorithmByName(String name) {
        if (name == null || name.isEmpty() || algorithmComboBox == null) {
            return;
        }
        for (AlgorithmItem item : algorithmComboBox.getItems()) {
            if (item.getAlgorithm().getName().equals(name)) {
                algorithmComboBox.getSelectionModel().select(item);
                if (sortController != null) {
                    sortController.setAlgorithm(item.getAlgorithm());
                }
                return;
            }
        }
    }

    private static class AlgorithmItem {
        private final Algorithm algorithm;
        public AlgorithmItem(Algorithm algorithm) { this.algorithm = algorithm; }
        public Algorithm getAlgorithm() { return algorithm; }
        @Override
        public String toString() { return algorithm.getName(); }
    }
}