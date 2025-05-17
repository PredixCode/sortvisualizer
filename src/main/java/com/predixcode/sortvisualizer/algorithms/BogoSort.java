
package com.predixcode.sortvisualizer.algorithms;

import java.util.List;
import java.util.Random;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class BogoSort extends AbstractSortAlgorithm {

    private int n; // Size of the array being sorted
    private boolean isSortedFlag = false;
    private Random random;
    private int shuffleIndex; // Current index for shuffling
    private int checkIndex; // Current index for checking if sorted

    // Internal state for managing steps within nextStep()
    private enum BogoSortInternalState {
        CHECKING_IF_SORTED, // Checking if the array is sorted
        SHUFFLING, // Shuffling the array
        SHUFFLE_COMPLETE // Shuffle is complete, ready to check again
    }
    private BogoSortInternalState currentState;

    public BogoSort() {
        this.random = new Random();
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.isSortedFlag = (n <= 1); // An array of 0 or 1 elements is already sorted.
        this.shuffleIndex = n - 1;
        this.checkIndex = 0;
        this.currentState = BogoSortInternalState.CHECKING_IF_SORTED;

        // Visually reset all elements to NORMAL if elements and callback are present
        if (this.elements != null && this.callback != null) {
            for (int k = 0; k < n; k++) {
                this.elements.get(k).setState(ElementState.NORMAL);
            }
        }
    }

    @Override
    public String getName() {
        return "Bogo Sort";
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
            case CHECKING_IF_SORTED:
                return handleCheckingIfSorted();
                
            case SHUFFLING:
                return handleShuffling();
                
            case SHUFFLE_COMPLETE:
                return handleShuffleComplete();
                
            default:
                isSortedFlag = true;
                return false;
        }
    }

    private boolean handleCheckingIfSorted() {
        if (checkIndex < n - 1) {
            callback.reportCompare(checkIndex, checkIndex + 1);
            
            if (elements.get(checkIndex).getValue() > elements.get(checkIndex + 1).getValue()) {
                // Array is not sorted, reset for shuffling
                for (int k = 0; k < n; k++) {
                    elements.get(k).setState(ElementState.NORMAL);
                }
                shuffleIndex = n - 1;
                currentState = BogoSortInternalState.SHUFFLING;
                callback.requestVisualUpdate();
                return true;
            }
            
            // Mark the checked element as potentially sorted
            callback.reportElementStateChange(checkIndex, ElementState.COMPARE);
            checkIndex++;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Array is sorted
            for (int k = 0; k < n; k++) {
                callback.reportElementStateChange(k, ElementState.SORTED);
            }
            isSortedFlag = true;
            return false;
        }
    }

    private boolean handleShuffling() {
        if (shuffleIndex > 0) {
            // Fisher-Yates shuffle: pick a random element from 0 to shuffleIndex
            int j = random.nextInt(shuffleIndex + 1);
            
            // Highlight the elements being swapped
            callback.reportElementStateChange(j, ElementState.SWAP);
            callback.reportElementStateChange(shuffleIndex, ElementState.SWAP);
            
            // Swap elements
            swap(j, shuffleIndex);
            
            shuffleIndex--;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Shuffle is complete
            currentState = BogoSortInternalState.SHUFFLE_COMPLETE;
            return true;
        }
    }

    private boolean handleShuffleComplete() {
        // Reset for checking if sorted
        for (int k = 0; k < n; k++) {
            elements.get(k).setState(ElementState.NORMAL);
        }
        checkIndex = 0;
        currentState = BogoSortInternalState.CHECKING_IF_SORTED;
        callback.requestVisualUpdate();
        return true;
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
