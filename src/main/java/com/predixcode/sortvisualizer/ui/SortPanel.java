package com.predixcode.sortvisualizer.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

/**
 * SortPanel is a JavaFX Pane that uses a Canvas to visualize an array of SortElements.
 * It draws each element as a bar, with color indicating its current state during sorting.
 */
public class SortPanel extends Pane {

    private Canvas canvas;
    private GraphicsContext gc;
    private List<SortElement> elements;
    private int maxValueForScaling = 100; // Default max value, updated by SortController

    private static final double BAR_GAP_PERCENTAGE = 0.1; // 10% gap between bars

    /**
     * Constructs a SortPanel with specified initial width and height.
     * @param initialWidth The initial width of the panel and its internal canvas.
     * @param initialHeight The initial height of the panel and its internal canvas.
     */
    public SortPanel(double initialWidth, double initialHeight) {
        this.elements = new ArrayList<>();
        this.canvas = new Canvas(initialWidth, initialHeight);
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        // Bind canvas size to pane size for responsiveness
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // Redraw when size changes
        this.widthProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::redraw));
        this.heightProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::redraw));

        Platform.runLater(this::clearPanel);
    }

    /**
     * Sets the maximum value present in the current array.
     * This is used for scaling the height of the bars correctly.
     * This method is called by the SortController before updating elements.
     * @param maxValue The maximum value in the current dataset.
     */
    public void setMaxValueForScaling(int maxValue) {
        // Ensure maxValueForScaling is at least 1 to prevent division by zero
        // and to provide a sensible scale even if all values are 0 (though unlikely for sorting).
        this.maxValueForScaling = (maxValue <= 0) ? 1 : maxValue;
        // Debugging: System.out.println("SortPanel: Max value for scaling set to " + this.maxValueForScaling);
        // A redraw is not explicitly called here because this method is typically called
        // just before updateElements, which itself will trigger a redraw.
    }


    /**
     * Updates the internal list of SortElements and triggers a redraw.
     * This method should be called from the JavaFX Application Thread.
     * @param newElements The new list of SortElements.
     */
    public synchronized void updateElements(List<SortElement> newElements) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateElementsInternal(newElements));
        } else {
            updateElementsInternal(newElements);
        }
    }

    private void updateElementsInternal(List<SortElement> newElements) {
        this.elements.clear();
        if (newElements != null && !newElements.isEmpty()) {
            this.elements.addAll(newElements);
        }
        redraw();
    }

    /**
     * Clears the canvas to the background color.
     * This method should be called from the JavaFX Application Thread.
     */
    public void clearPanel() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::clearPanelInternal);
            return;
        }
        clearPanelInternal();
    }

    private void clearPanelInternal() {
        gc.setFill(Theme.PANEL_BACKGROUND_COLOR);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Redraws all elements on the canvas.
     * This should be called whenever the array or its states change.
     * This method should be called from the JavaFX Application Thread.
     */
    public synchronized void redraw() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::redrawInternal);
            return;
        }
        redrawInternal();
    }

    private void redrawInternal() {
        clearPanelInternal();

        if (elements.isEmpty()) {
            return;
        }

        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();

        if (canvasWidth <= 0 || canvasHeight <= 0) {
            return; 
        }

        int numElements = elements.size();
        double totalBarWidth = canvasWidth / numElements;
        double barGap = totalBarWidth * BAR_GAP_PERCENTAGE;
        double barWidth = totalBarWidth - barGap;

        if (barWidth < 1.0 && numElements > 0) barWidth = 1.0;
        if (barWidth > totalBarWidth * (1.0 - BAR_GAP_PERCENTAGE / 2) ) barWidth = totalBarWidth * (1.0 - BAR_GAP_PERCENTAGE / 2);

        double x = barGap / 2.0;

        for (SortElement element : elements) {
            // Use maxValueForScaling here. It's guaranteed to be at least 1.
            double barHeightPercentage = (double) element.getValue() / this.maxValueForScaling;
            double barHeight = barHeightPercentage * canvasHeight;

            if (barHeight < 0) barHeight = 0;
            // It's possible for barHeight to slightly exceed canvasHeight due to double precision
            // or if an element's value is unexpectedly larger than maxValueForScaling.
            // Clamping it ensures it doesn't draw outside bounds.
            if (barHeight > canvasHeight) barHeight = canvasHeight;


            double y = canvasHeight - barHeight;

            switch (element.getState()) {
                case COMPARE:
                    gc.setFill(Theme.BAR_COMPARE_COLOR);
                    break;
                case SWAP:
                    gc.setFill(Theme.BAR_SWAP_COLOR);
                    break;
                case PIVOT:
                    gc.setFill(Theme.BAR_PIVOT_COLOR);
                    break;
                case SORTED:
                    gc.setFill(Theme.BAR_SORTED_COLOR);
                    break;
                case NORMAL:
                default:
                    gc.setFill(Theme.BAR_DEFAULT_COLOR);
                    break;
            }
            gc.fillRect(x, y, barWidth, barHeight);
            x += totalBarWidth;
        }
    }

    /**
     * Gets the current list of SortElements.
     * @return A new list containing copies of SortElement objects.
     */
    public synchronized List<SortElement> getElements() {
        // Returning a new list of new SortElement objects if external modification is a concern
        // For now, just a new list wrapper.
        return new ArrayList<>(elements);
    }
}
