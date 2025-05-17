
package com.predixcode.sortvisualizer.algorithms;

import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class CocktailSort extends AbstractSortAlgorithm {

    private int start; // Start index of the unsorted portion
    private int end; // End index of the unsorted portion
    private int i; // Current index for comparison
    private boolean swappedInCurrentPass; // Optimization flag
    private boolean isSortedFlag = false;
    private int n; // Size of the array being sorted
    private boolean isForward; // Direction of the current pass

    // Internal state for managing steps within nextStep()
    private enum CocktailSortInternalState {
        FORWARD_PASS, // Moving from left to right
        BACKWARD_PASS, // Moving from right to left
        CHECKING_PASS_COMPLETION // Checking if any swaps occurred
    }
    private CocktailSortInternalState currentState;

    public CocktailSort() {
        // Constructor can be empty
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.start = 0;
        this.end = n - 1;
        this.i = start;
        this.swappedInCurrentPass = false;
        this.isSortedFlag = (n <= 1); // An array of 0 or 1 elements is already sorted.
        this.isForward = true;
        this.currentState = CocktailSortInternalState.FORWARD_PASS;

        // Visually reset all elements to NORMAL if elements and callback are present
        if (this.elements != null && this.callback != null) {
            for (int k = 0; k < n; k++) {
                this.elements.get(k).setState(ElementState.NORMAL);
            }
        }
    }

    @Override
    public String getName() {
        return "Cocktail Sort";
    }

    @Override
    public boolean nextStep() {
        if (isSortedFlag || callback.isStopRequested() || n == 0) {
            if (!isSortedFlag && n > 0) {
                for (SortElement el : elements) {
                    if (el.getState() != ElementState.SORTED) el.setState(ElementState.NORMAL);
                }
                callback.requestVisualUpdate();
            }
            isSortedFlag = true;
            return false;
        }

        // Check if the array is sorted (start and end have crossed)
        if (start >= end) {
            // Mark all elements as sorted
            for (int k = 0; k < n; k++) {
                if (elements.get(k).getState() != ElementState.SORTED) {
                    callback.reportElementStateChange(k, ElementState.SORTED);
                }
            }
            isSortedFlag = true;
            return false;
        }

        switch (currentState) {
            case FORWARD_PASS:
                return handleForwardPass();
                
            case BACKWARD_PASS:
                return handleBackwardPass();
                
            case CHECKING_PASS_COMPLETION:
                return handleCheckingPassCompletion();
                
            default:
                isSortedFlag = true;
                return false;
        }
    }

    private boolean handleForwardPass() {
        if (i < end) {
            callback.reportCompare(i, i + 1);
            
            if (elements.get(i).getValue() > elements.get(i + 1).getValue()) {
                swap(i, i + 1);
                swappedInCurrentPass = true;
            }
            
            i++;
            return true;
        } else {
            // Forward pass complete, mark the last element as sorted
            callback.reportElementStateChange(end, ElementState.SORTED);
            end--;
            
            // Reset for backward pass
            i = end;
            isForward = false;
            currentState = CocktailSortInternalState.BACKWARD_PASS;
            
            // Reset non-sorted element states to NORMAL
            for (int k = start; k <= end; k++) {
                if (elements.get(k).getState() != ElementState.SORTED) {
                    elements.get(k).setState(ElementState.NORMAL);
                }
            }
            
            callback.requestVisualUpdate();
            return true;
        }
    }

    private boolean handleBackwardPass() {
        if (i > start) {
            callback.reportCompare(i - 1, i);
            
            if (elements.get(i - 1).getValue() > elements.get(i).getValue()) {
                swap(i - 1, i);
                swappedInCurrentPass = true;
            }
            
            i--;
            return true;
        } else {
            // Backward pass complete, mark the first element as sorted
            callback.reportElementStateChange(start, ElementState.SORTED);
            start++;
            
            // Check if any swaps occurred in this complete cycle
            currentState = CocktailSortInternalState.CHECKING_PASS_COMPLETION;
            return true;
        }
    }

    private boolean handleCheckingPassCompletion() {
        if (!swappedInCurrentPass) {
            // No swaps occurred, array is sorted
            for (int k = start; k <= end; k++) {
                if (elements.get(k).getState() != ElementState.SORTED) {
                    callback.reportElementStateChange(k, ElementState.SORTED);
                }
            }
            isSortedFlag = true;
            return false;
        }
        
        // Reset for next forward pass
        i = start;
        isForward = true;
        swappedInCurrentPass = false;
        currentState = CocktailSortInternalState.FORWARD_PASS;
        
        // Reset non-sorted element states to NORMAL
        for (int k = start; k <= end; k++) {
            if (elements.get(k).getState() != ElementState.SORTED) {
                elements.get(k).setState(ElementState.NORMAL);
            }
        }
        
        callback.requestVisualUpdate();
        return true;
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
