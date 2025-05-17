package com.predixcode.sortvisualizer.ui;

import java.util.ArrayList;
import java.util.List;

import com.predixcode.sortvisualizer.algorithms.Algorithm;
import com.predixcode.sortvisualizer.algorithms.BubbleSort;
import com.predixcode.sortvisualizer.algorithms.InsertionSort;
import com.predixcode.sortvisualizer.algorithms.MergeSort;
import com.predixcode.sortvisualizer.algorithms.QuickSort;
import com.predixcode.sortvisualizer.algorithms.TreeSort;
import com.predixcode.sortvisualizer.core.SortController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ControlPanel extends VBox {

    private SortController sortController; // Set via setter or constructor

    private ComboBox<AlgorithmItem> algorithmComboBox;
    private Button startButton;
    private Button resetButton;
    private Button stopButton; // Added stop button
    private Slider speedSlider;
    private TextField arraySizeField;
    private Label speedValueLabel;

    //Config
    private static final int ELEMENT_SIZE_CAP = 10000; 


    private static final List<Algorithm> AVAILABLE_ALGORITHMS = new ArrayList<>();

    static {
        AVAILABLE_ALGORITHMS.add(new QuickSort());
        AVAILABLE_ALGORITHMS.add(new BubbleSort());
        AVAILABLE_ALGORITHMS.add(new InsertionSort());
        AVAILABLE_ALGORITHMS.add(new MergeSort());
        AVAILABLE_ALGORITHMS.add(new TreeSort());
    }

    public ControlPanel() {
        this.setPadding(new Insets(15, 12, 15, 12));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: " + Theme.BACKGROUND_COLOR_HEX + ";");
        this.setAlignment(Pos.CENTER);

        // --- Algorithm Selection ---
        Label algoLabel = new Label("Algorithm:");
        algoLabel.setTextFill(Theme.TEXT_COLOR_LIGHT);
        algorithmComboBox = new ComboBox<>();
        for (Algorithm algo : AVAILABLE_ALGORITHMS) {
            algorithmComboBox.getItems().add(new AlgorithmItem(algo));
        }
        algorithmComboBox.setPromptText("Select Algorithm");
        algorithmComboBox.setOnAction(event -> {
            AlgorithmItem selected = algorithmComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && sortController != null) {
                sortController.setAlgorithm(selected.getAlgorithm());
            }
        });

        // --- Array Size ---
        Label sizeLabel = new Label("Array Size (10-200):");
        sizeLabel.setTextFill(Theme.TEXT_COLOR_LIGHT);
        arraySizeField = new TextField("50");
        arraySizeField.setPrefWidth(60);

        // --- Reset Button ---
        resetButton = new Button("Generate New Array");
        resetButton.setStyle(Theme.BUTTON_STYLE_PRIMARY);
        resetButton.setOnAction(event -> {
            if (sortController != null) {
                try {
                    int size = Integer.parseInt(arraySizeField.getText());
                    if (size < 10) size = 10;
                    if (size > ELEMENT_SIZE_CAP) size = ELEMENT_SIZE_CAP; // Cap max size for performance
                    arraySizeField.setText(String.valueOf(size)); // Update field with validated size
                    sortController.generateNewArray(size, 10, 100);
                } catch (NumberFormatException e) {
                    App.showAlert("Invalid Size", "Please enter a valid number for array size.");
                    arraySizeField.setText("50");
                }
            }
        });

        // --- Start Button ---
        startButton = new Button("Start Sort");
        startButton.setStyle(Theme.BUTTON_STYLE_PRIMARY.replace(Theme.toHex(Theme.PRIMARY_COLOR), Theme.toHex(Theme.SECONDARY_COLOR))); // Greenish
        startButton.setOnAction(event -> {
            if (sortController != null) {
                sortController.startSort();
            }
        });

        // --- Stop Button ---
        stopButton = new Button("Stop Sort");
        stopButton.setStyle(Theme.BUTTON_STYLE_PRIMARY.replace(Theme.toHex(Theme.PRIMARY_COLOR), Theme.toHex(Theme.ACCENT_COLOR))); // Orangish
        stopButton.setOnAction(event -> {
            if (sortController != null) {
                sortController.stopSort();
            }
        });
        stopButton.setDisable(true); // Initially disabled

        // --- Speed Control ---
        Label speedLabel = new Label("Speed (Delay ms):");
        speedLabel.setTextFill(Theme.TEXT_COLOR_LIGHT);
        speedSlider = new Slider(0, 500, 50); // Min, Max, Initial
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(100);
        speedSlider.setBlockIncrement(10);
        speedValueLabel = new Label(String.format("%.0f ms", speedSlider.getValue()));
        speedValueLabel.setTextFill(Theme.TEXT_COLOR_LIGHT);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (sortController != null) {
                sortController.setAnimationDelay(newVal.intValue());
            }
            speedValueLabel.setText(String.format("%.0f ms", newVal.doubleValue()));
        });


        HBox topControls = new HBox(10, algoLabel, algorithmComboBox, sizeLabel, arraySizeField, resetButton);
        topControls.setAlignment(Pos.CENTER);
        HBox bottomControls = new HBox(10, startButton, stopButton, speedLabel, speedSlider, speedValueLabel);
        bottomControls.setAlignment(Pos.CENTER);

        this.getChildren().addAll(topControls, bottomControls);
    }

    public void setSortController(SortController controller) {
        this.sortController = controller;
        // Initialize controller's delay with slider's current value
        if (this.sortController != null) {
            this.sortController.setAnimationDelay((int) speedSlider.getValue());
            if (!algorithmComboBox.getItems().isEmpty()) {
                 algorithmComboBox.getSelectionModel().selectFirst(); // Select first algorithm by default
                 this.sortController.setAlgorithm(algorithmComboBox.getSelectionModel().getSelectedItem().getAlgorithm());
            }
        }
    }

    public void disableControlsDuringSort() {
        algorithmComboBox.setDisable(true);
        arraySizeField.setDisable(true);
        resetButton.setDisable(true);
        startButton.setDisable(true);
        stopButton.setDisable(false); // Enable stop button
        speedSlider.setDisable(true);
    }

    public void enableControls() {
        algorithmComboBox.setDisable(false);
        arraySizeField.setDisable(false);
        resetButton.setDisable(false);
        startButton.setDisable(false);
        stopButton.setDisable(true); // Disable stop button
        speedSlider.setDisable(false);
    }

    // Helper class to display algorithm names in ComboBox
    private static class AlgorithmItem {
        private final Algorithm algorithm;

        public AlgorithmItem(Algorithm algorithm) {
            this.algorithm = algorithm;
        }

        public Algorithm getAlgorithm() {
            return algorithm;
        }

        @Override
        public String toString() {
            return algorithm.getName(); // Assumes getName() is implemented in Algorithm interface
        }
    }
}
