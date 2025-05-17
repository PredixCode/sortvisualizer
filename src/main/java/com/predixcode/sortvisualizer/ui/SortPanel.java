package com.predixcode.sortvisualizer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

/**
 * SortPanel is a JavaFX Pane that uses a Canvas to visualize an array of SortElements.
 * It draws each element as a bar, with color indicating its current state during sorting.
 */
public class SortPanel extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final List<SortElement> elements;
    private int maxValueForScaling = 100; // Default max value, updated by SortController

    private static final double BAR_GAP_PERCENTAGE = 0.1; // 10% gap between bars
    
    // Added custom colors map for element states
    private final Map<ElementState, Color> stateColors = new HashMap<>();
    
    // Added properties for visual customization
    private double barWidthPercentage = 0.85;
    private double barGapPercentage = BAR_GAP_PERCENTAGE;
    private double barCornerRadius = 4.0;
    private boolean useGradients = true;
    private boolean useEffects = true;
    private boolean useAnimations = true;
    private boolean highPerformanceMode = false;

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

        // Initialize state colors with default values
        stateColors.put(ElementState.NORMAL, Theme.BAR_DEFAULT_COLOR);
        stateColors.put(ElementState.COMPARE, Theme.BAR_COMPARE_COLOR);
        stateColors.put(ElementState.SWAP, Theme.BAR_SWAP_COLOR);
        stateColors.put(ElementState.PIVOT, Theme.BAR_PIVOT_COLOR);
        stateColors.put(ElementState.SORTED, Theme.BAR_SORTED_COLOR);

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
        double barGap = totalBarWidth * barGapPercentage;
        double barWidth = totalBarWidth * barWidthPercentage;

        if (barWidth < 1.0 && numElements > 0) barWidth = 1.0;
        if (barWidth > totalBarWidth * (1.0 - barGapPercentage / 2) ) barWidth = totalBarWidth * (1.0 - barGapPercentage / 2);

        double x = (totalBarWidth - barWidth) / 2.0;

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

            // Get color from the custom colors map, or use default if not found
            Color barColor = stateColors.getOrDefault(element.getState(), Theme.BAR_DEFAULT_COLOR);
            gc.setFill(barColor);
            
            // In high performance mode, use simpler rendering
            if (highPerformanceMode) {
                gc.fillRect(x, y, barWidth, barHeight);
            } else {
                // Draw with rounded corners if barCornerRadius > 0
                if (barCornerRadius > 0 && barWidth > barCornerRadius * 2) {
                    gc.fillRoundRect(x, y, barWidth, barHeight, barCornerRadius, barCornerRadius);
                } else {
                    gc.fillRect(x, y, barWidth, barHeight);
                }
            }
            
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
    
    /**
     * Sets the color for a specific element state.
     * @param state The element state to set the color for.
     * @param color The color to use for the specified state.
     */
    public void setElementStateColor(ElementState state, Color color) {
        if (state != null && color != null) {
            stateColors.put(state, color);
            redraw();
        }
    }
    
    /**
     * Sets the bar width percentage (relative to total available width per element).
     * @param percentage The width percentage (0.0-1.0).
     */
    public void setBarWidthPercentage(double percentage) {
        this.barWidthPercentage = Math.max(0.1, Math.min(1.0, percentage));
        redraw();
    }
    
    /**
     * Gets the current bar width percentage.
     * @return The bar width percentage.
     */
    public double getBarWidthPercentage() {
        return barWidthPercentage;
    }
    
    /**
     * Sets the gap percentage between bars.
     * @param percentage The gap percentage (0.0-0.5).
     */
    public void setBarGapPercentage(double percentage) {
        this.barGapPercentage = Math.max(0.0, Math.min(0.5, percentage));
        redraw();
    }
    
    /**
     * Gets the current gap percentage between bars.
     * @return The gap percentage.
     */
    public double getBarGapPercentage() {
        return barGapPercentage;
    }
    
    /**
     * Sets the corner radius for rounded bars.
     * @param radius The corner radius in pixels.
     */
    public void setBarCornerRadius(double radius) {
        this.barCornerRadius = Math.max(0.0, radius);
        redraw();
    }
    
    /**
     * Gets the current bar corner radius.
     * @return The corner radius in pixels.
     */
    public double getBarCornerRadius() {
        return barCornerRadius;
    }
    
    /**
     * Sets whether to use gradient colors for bars.
     * @param use True to use gradients, false otherwise.
     */
    public void setUseGradients(boolean use) {
        this.useGradients = use;
        redraw();
    }
    
    /**
     * Checks if gradients are being used.
     * @return True if gradients are enabled.
     */
    public boolean isUsingGradients() {
        return useGradients;
    }
    
    /**
     * Sets whether to use visual effects like glow and bloom.
     * @param use True to use effects, false otherwise.
     */
    public void setUseEffects(boolean use) {
        this.useEffects = use;
        redraw();
    }
    
    /**
     * Checks if visual effects are being used.
     * @return True if effects are enabled.
     */
    public boolean isUsingEffects() {
        return useEffects;
    }
    
    /**
     * Sets whether to use animations for state changes.
     * @param use True to use animations, false otherwise.
     */
    public void setUseAnimations(boolean use) {
        this.useAnimations = use;
    }
    
    /**
     * Checks if animations are being used.
     * @return True if animations are enabled.
     */
    public boolean isUsingAnimations() {
        return useAnimations;
    }
    
    /**
     * Sets high performance mode for the visualization.
     * When enabled, this mode simplifies rendering to improve performance
     * during sorting operations with large arrays or fast animations.
     * 
     * @param enabled True to enable high performance mode, false to disable
     */
    public void setHighPerformanceMode(boolean enabled) {
        this.highPerformanceMode = enabled;
        
        // In high performance mode, we disable certain visual features
        if (enabled) {
            // Store current values to restore later if needed
            this.barCornerRadius = 0.0; // Disable rounded corners
            this.useGradients = false;  // Disable gradients
            this.useEffects = false;    // Disable effects
        }
        
        // Redraw with the new settings
        redraw();
    }
    
    /**
     * Checks if high performance mode is enabled.
     * @return True if high performance mode is enabled
     */
    public boolean isHighPerformanceMode() {
        return highPerformanceMode;
    }
}
