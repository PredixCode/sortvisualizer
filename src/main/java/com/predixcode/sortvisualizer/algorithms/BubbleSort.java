package com.predixcode.sortvisualizer.algorithms;

import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class BubbleSort extends AbstractSortAlgorithm {

    private int i; // Outer loop counter for passes
    private int j; // Inner loop counter for comparisons
    private boolean swappedInCurrentPass; // Optimization flag
    private boolean isSortedFlag = false;
    private int n; // Size of the array being sorted

    // Internal state for managing steps within nextStep()
    private enum BubbleSortInternalState {
        COMPARING,
        CHECKING_PASS_COMPLETION
    }
    private BubbleSortInternalState currentState;

    public BubbleSort() {
        // Constructor can be empty
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback); // This calls our reset()
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.i = 0;
        this.j = 0;
        this.swappedInCurrentPass = false;
        this.isSortedFlag = (n <= 1); // An array of 0 or 1 elements is already sorted.
        this.currentState = BubbleSortInternalState.COMPARING;

        // Visually reset all elements to NORMAL if elements and callback are present
        if (this.elements != null && this.callback != null) {
            for (int k = 0; k < n; k++) {
                this.elements.get(k).setState(ElementState.NORMAL);
            }
            // No immediate visual update here; initialize/reset is usually followed by one.
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean nextStep() {
        if (isSortedFlag || callback.isStopRequested() || n == 0) {
            if (!isSortedFlag && n > 0) { // If stopped early or before completion
                // Ensure visual states are reset if sort didn't complete normally
                for (SortElement el : elements) {
                    if (el.getState() != ElementState.SORTED) el.setState(ElementState.NORMAL);
                }
                callback.requestVisualUpdate();
            }
            isSortedFlag = true; // Mark as sorted to stop further steps if not already
            // callback.reportSortCompleted(); // Controller will call this if loop finishes due to isSortedFlag
            return false; // No more steps
        }

        // --- Main Bubble Sort Logic ---
        if (i < n - 1) { // Outer loop: controls passes
            if (currentState == BubbleSortInternalState.COMPARING) {
                if (j < n - i - 1) { // Inner loop: controls comparisons in current pass
                    callback.reportCompare(j, j + 1); // Highlight elements
                    // Visual update will be triggered by SortController after this step returns true

                    if (elements.get(j).getValue() > elements.get(j + 1).getValue()) {
                        swap(j, j + 1); // AbstractSortAlgorithm.swap handles callback.reportSwap and actual swap
                        swappedInCurrentPass = true;
                    }
                    // else: no swap, elements were just compared. callback.reportCompare handled highlight.
                    
                    j++; // Move to next comparison in the inner loop
                    return true; // Still more work to do in this pass
                } else {
                    // Inner loop (j) for current pass (i) is complete
                    // The element at (n - 1 - i) is now in its sorted position
                    if (n - 1 - i >= 0 && n - 1 - i < n) {
                         callback.reportElementStateChange(n - 1 - i, ElementState.SORTED);
                    }
                    currentState = BubbleSortInternalState.CHECKING_PASS_COMPLETION;
                    return true; // Transition to checking pass completion
                }
            } else if (currentState == BubbleSortInternalState.CHECKING_PASS_COMPLETION) {
                // Check if any swaps occurred in the last pass
                if (!swappedInCurrentPass) {
                    isSortedFlag = true; // Optimization: if no swaps, array is sorted
                    // Mark all remaining (non-SORTED) elements as sorted
                    for (int k = 0; k < n - 1 - i; k++) {
                        if (elements.get(k).getState() != ElementState.SORTED) {
                           callback.reportElementStateChange(k, ElementState.SORTED);
                        }
                    }
                    // callback.reportSortCompleted(); // Controller will handle this
                    return false; // Sorting finished
                }

                // Prepare for the next pass
                i++;
                j = 0;
                swappedInCurrentPass = false;
                currentState = BubbleSortInternalState.COMPARING;
                // Reset non-sorted element states to NORMAL before starting next pass
                for(SortElement el : elements) {
                    if (el.getState() != ElementState.SORTED) el.setState(ElementState.NORMAL);
                }
                // callback.requestVisualUpdate(); // Controller will do this after nextStep returns true
                return true; // More passes to do (or i might now be >= n-1)
            }
        } else {
            // Outer loop (i) is complete, all elements are sorted
            isSortedFlag = true;
            // Ensure all elements are marked as SORTED if not already
            for(int k=0; k<n; k++) {
                if(elements.get(k).getState() != ElementState.SORTED) {
                    callback.reportElementStateChange(k, ElementState.SORTED);
                }
            }
            // callback.reportSortCompleted();
            return false; // Sorting finished
        }
        // This line should ideally not be reached if logic is correct
        return !isSortedFlag;
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
