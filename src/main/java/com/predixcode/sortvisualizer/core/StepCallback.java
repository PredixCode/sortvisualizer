package com.predixcode.sortvisualizer.core;

import com.predixcode.sortvisualizer.ui.SortElement;

/**
 * Interface for algorithms to communicate back to the SortController during step-by-step execution.
 */
public interface StepCallback {

    /**
     * Reports that two elements are being compared.
     * The controller should update their visual state.
     * @param index1 Index of the first element.
     * @param index2 Index of the second element.
     */
    void reportCompare(int index1, int index2);

    /**
     * Reports that two elements should be swapped.
     * The controller should perform the swap on its list of SortElements
     * and update their visual state.
     * @param index1 Index of the first element.
     * @param index2 Index of the second element.
     */
    void reportSwap(int index1, int index2);

    /**
     * Reports a change in the state of a single element.
     * Useful for marking elements as pivots, sorted, etc.
     * @param index The index of the element.
     * @param newState The new state for the element.
     */
    void reportElementStateChange(int index, SortElement.ElementState newState);

    /**
     * Resets the visual state of a range of elements to NORMAL.
     * Typically called after a comparison or swap is visualized.
     * @param startIndex Inclusive start index.
     * @param endIndex Inclusive end index.
     */
    void reportResetStates(int startIndex, int endIndex);

    /**
     * Resets the visual state of specified elements to NORMAL.
     * @param indices The indices of elements to reset.
     */
    void reportResetStates(int... indices);


    /**
     * Gets the current animation delay in milliseconds.
     * Algorithms should use this to pace themselves if they manage their own sleep (less common).
     * More commonly, the SortController manages the sleep between steps.
     * @return The animation delay in milliseconds.
     */
    int getAnimationDelayMs();

    /**
     * Checks if the user has requested to stop the sorting process.
     * Algorithms should check this periodically and halt if true.
     * @return true if a stop has been requested, false otherwise.
     */
    boolean isStopRequested();

    /**
     * Notifies the controller that the sort is complete.
     */
    void reportSortCompleted();

    /**
     * Notifies the controller that a step has been completed and a repaint might be needed.
     * The controller will then handle updating the SortPanel on the UI thread.
     */
    void requestVisualUpdate();
}
