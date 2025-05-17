package com.predixcode.sortvisualizer.algorithms;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;

import java.util.List;

/**
 * Abstract base class for sorting algorithms that support step-by-step execution.
 */
public abstract class AbstractSortAlgorithm implements Algorithm {

    protected List<SortElement> elements;
    protected StepCallback callback;
    protected boolean isSorted;

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        if (elements == null) {
            throw new IllegalArgumentException("Element list cannot be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("StepCallback cannot be null.");
        }
        this.elements = elements;
        this.callback = callback;
        this.isSorted = false; // Initially not sorted
        reset(); // Call specific algorithm reset
    }

    /**
     * Helper method to swap two elements in the list.
     * This also informs the callback about the swap for visualization.
     * @param i Index of the first element.
     * @param j Index of the second element.
     */
    protected void swap(int i, int j) {
        if (i >= 0 && i < elements.size() && j >= 0 && j < elements.size()) {
            // Inform callback about the intention to swap (for highlighting)
            callback.reportSwap(i, j);

            // Perform the actual swap on the SortElement list
            SortElement temp = elements.get(i);
            elements.set(i, elements.get(j));
            elements.set(j, temp);

            // Request a visual update after the swap
            callback.requestVisualUpdate();
        } else {
            System.err.println("Swap indices out of bounds: i=" + i + ", j=" + j + ", size=" + elements.size());
        }
    }

    @Override
    public abstract boolean nextStep(); // Concrete algorithms must implement their stepping logic

    @Override
    public abstract void reset(); // Concrete algorithms must implement their state reset

    @Override
    public abstract String getName(); // Concrete algorithms must provide their name
}
