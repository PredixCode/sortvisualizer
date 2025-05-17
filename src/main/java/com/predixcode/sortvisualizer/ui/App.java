package com.predixcode.sortvisualizer.ui;

import com.predixcode.sortvisualizer.core.SortController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * App is the main window for the sorting algorithm visualizer.
 * It sets up the primary stage and scene, and organizes the main UI components.
 */
public class App extends Application { // Renamed from VisualizerFrame

    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 700;

    private SortPanel sortPanel;
    private ControlPanel controlPanel;
    private SortController sortController;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sorting Algorithm Visualizer");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + Theme.BACKGROUND_COLOR_HEX + ";");

        // Initialize SortPanel
        sortPanel = new SortPanel(800, 500); // Initial preferred size

        // Initialize ControlPanel
        controlPanel = new ControlPanel();

        // Initialize the SortController
        sortController = new SortController(sortPanel);
        sortController.setControlPanel(controlPanel); // Link controller to control panel
        controlPanel.setSortController(sortController); // Link control panel to controller


        root.setCenter(sortPanel);
        root.setBottom(controlPanel);

        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        // Optional: Add a CSS stylesheet
        // scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Application closing...");
            if (sortController != null) {
                sortController.stopSort(); // Attempt to stop sorting thread if active
            }
            Platform.exit(); // Ensures JavaFX platform exits cleanly
            System.exit(0); // Ensures non-JavaFX threads also terminate
        });
        primaryStage.show();

        // Initial array generation is now handled in SortController constructor
        // sortController.generateNewArray(50, 10, 100); // Example
    }

    /**
     * Helper method to show an alert dialog.
     * @param title Title of the alert.
     * @param content Content message of the alert.
     */
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
