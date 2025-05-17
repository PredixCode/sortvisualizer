package com.predixcode.sortvisualizer.algorithms;

import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;

/**
 * Interface for sorting algorithms designed for step-by-step visualization.
 */
public interface Algorithm {

    /**
     * Initializes the algorithm with the data to be sorted and a callback mechanism.
     * This should be called before any calls to nextStep().
     * @param elements The list of SortElement objects to be sorted. The algorithm will modify this list directly.
     * @param callback The StepCallback instance for reporting UI updates and checking control states.
     */
    void initialize(List<SortElement> elements, StepCallback callback);

    /**
     * Executes the next logical step of the sorting algorithm.
     * For example, one comparison, one swap, or one pass in an outer loop.
     * @return true if there are more steps to perform, false if the algorithm has completed sorting.
     */
    boolean nextStep();

    /**
     * Resets the internal state of the algorithm.
     * Called when a sort is stopped or a new array is generated.
     */
    void reset();

    /**
     * Gets the display name of the sorting algorithm.
     * @return The name of the algorithm.
     */
    String getName();

    boolean isSorted();
}
