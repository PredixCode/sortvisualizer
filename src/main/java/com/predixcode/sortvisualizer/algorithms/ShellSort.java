
package com.predixcode.sortvisualizer.algorithms;

import java.util.ArrayList;
import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class ShellSort extends AbstractSortAlgorithm {

    private int n; // Size of the array being sorted
    private boolean isSortedFlag = false;
    private List<Integer> gaps; // Sequence of gaps
    private int gapIndex; // Current index in the gaps list
    private int i; // Outer loop counter
    private int j; // Inner loop counter
    private int temp; // Temporary value for insertion

    // Internal state for managing steps within nextStep()
    private enum ShellSortInternalState {
        GAP_INITIALIZATION, // Initializing the gap sequence
        OUTER_LOOP, // Starting a new pass with the current gap
        INNER_LOOP, // Processing elements within the current gap
        INSERTION, // Inserting an element in its correct position
        GAP_COMPLETE // Current gap processing is complete
    }
    private ShellSortInternalState currentState;

    public ShellSort() {
        // Constructor
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.isSortedFlag = (n <= 1); // An array of 0 or 1 elements is already sorted.
        this.gaps = new ArrayList<>();
        this.currentState = ShellSortInternalState.GAP_INITIALIZATION;

        // Visually reset all elements to NORMAL if elements and callback are present
        if (this.elements != null && this.callback != null) {
            for (int k = 0; k < n; k++) {
                this.elements.get(k).setState(ElementState.NORMAL);
            }
        }
    }

    @Override
    public String getName() {
        return "Shell Sort";
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

        switch (currentState) {
            case GAP_INITIALIZATION:
                return handleGapInitialization();
                
            case OUTER_LOOP:
                return handleOuterLoop();
                
            case INNER_LOOP:
                return handleInnerLoop();
                
            case INSERTION:
                return handleInsertion();
                
            case GAP_COMPLETE:
                return handleGapComplete();
                
            default:
                isSortedFlag = true;
                return false;
        }
    }

    private boolean handleGapInitialization() {
        // Initialize the gap sequence (using Knuth's sequence: h = 3*h + 1)
        gaps.clear();
        int gap = 1;
        while (gap < n / 3) {
            gap = 3 * gap + 1;
        }
        
        while (gap > 0) {
            gaps.add(gap);
            gap = gap / 3;
        }
        
        gapIndex = 0;
        currentState = ShellSortInternalState.OUTER_LOOP;
        return true;
    }

    private boolean handleOuterLoop() {
        if (gapIndex < gaps.size()) {
            int gap = gaps.get(gapIndex);
            
            // Reset for the current gap
            i = gap;
            
            if (i < n) {
                // Highlight elements in the current gap
                for (int k = 0; k < n; k++) {
                    if (k % gap == 0) {
                        callback.reportElementStateChange(k, ElementState.PIVOT);
                    } else {
                        elements.get(k).setState(ElementState.NORMAL);
                    }
                }
                
                currentState = ShellSortInternalState.INNER_LOOP;
                callback.requestVisualUpdate();
                return true;
            } else {
                // No elements to process for this gap
                gapIndex++;
                return true;
            }
        } else {
            // All gaps processed, array is sorted
            for (int k = 0; k < n; k++) {
                callback.reportElementStateChange(k, ElementState.SORTED);
            }
            isSortedFlag = true;
            return false;
        }
    }

    private boolean handleInnerLoop() {
        int gap = gaps.get(gapIndex);
        
        if (i < n) {
            // Save the current element
            temp = elements.get(i).getValue();
            j = i;
            
            // Highlight the current element
            callback.reportElementStateChange(i, ElementState.SWAP);
            
            currentState = ShellSortInternalState.INSERTION;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Current gap processing is complete
            currentState = ShellSortInternalState.GAP_COMPLETE;
            return true;
        }
    }

    private boolean handleInsertion() {
        int gap = gaps.get(gapIndex);
        
        if (j >= gap && elements.get(j - gap).getValue() > temp) {
            // Compare elements
            callback.reportCompare(j, j - gap);
            
            // Move element
            elements.set(j, elements.get(j - gap));
            callback.reportElementStateChange(j, ElementState.COMPARE);
            
            j -= gap;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Insert the element at its correct position
            SortElement tempElement = elements.get(j);
            tempElement.setValue(temp);
            callback.reportElementStateChange(j, ElementState.SWAP);
            
            // Move to the next element
            i++;
            currentState = ShellSortInternalState.INNER_LOOP;
            callback.requestVisualUpdate();
            return true;
        }
    }

    private boolean handleGapComplete() {
        // Move to the next gap
        gapIndex++;
        
        // Reset element states
        for (int k = 0; k < n; k++) {
            elements.get(k).setState(ElementState.NORMAL);
        }
        
        currentState = ShellSortInternalState.OUTER_LOOP;
        callback.requestVisualUpdate();
        return true;
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
