
package com.predixcode.sortvisualizer.algorithms;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class QuickSort extends AbstractSortAlgorithm {

    // Structure to hold ranges for partitioning
    private static class QuickSortRange {
        int low, high;
        
        QuickSortRange(int low, int high) {
            this.low = low;
            this.high = high;
        }
    }

    private Deque<QuickSortRange> taskStack; // To simulate recursion iteratively
    private boolean isSortedFlag = false;
    private int n;

    // State for current partition operation
    private int pivotIndex;
    private int i; // Current index being compared
    private int j; // Index for elements less than pivot

    private enum QuickSortInternalState {
        IDLE,
        SELECTING_PIVOT,
        PARTITIONING,
        PARTITION_COMPLETE
    }
    private QuickSortInternalState currentState;
    private QuickSortRange currentRange;

    public QuickSort() {
        // Constructor
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.taskStack = new ArrayDeque<>();
        this.isSortedFlag = (n <= 1);
        this.currentState = QuickSortInternalState.IDLE;
        this.currentRange = null;

        if (!isSortedFlag && n > 0) {
            taskStack.push(new QuickSortRange(0, n - 1)); // Initial sort task for the whole array
            currentState = QuickSortInternalState.SELECTING_PIVOT;
        }
        
        if (this.elements != null && this.callback != null) {
            for (SortElement el : this.elements) el.setState(ElementState.NORMAL);
        }
    }

    @Override
    public String getName() {
        return "Quick Sort";
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
            case IDLE:
                if (taskStack.isEmpty()) {
                    // All partitioning tasks are done
                    isSortedFlag = true;
                    for (SortElement el : elements) el.setState(ElementState.SORTED);
                    return false;
                } else {
                    currentState = QuickSortInternalState.SELECTING_PIVOT;
                    return true;
                }

            case SELECTING_PIVOT:
                return handleSelectingPivot();

            case PARTITIONING:
                return handlePartitioning();

            case PARTITION_COMPLETE:
                return handlePartitionComplete();

            default:
                isSortedFlag = true;
                return false;
        }
    }

    private boolean handleSelectingPivot() {
        if (taskStack.isEmpty()) {
            currentState = QuickSortInternalState.IDLE;
            return true;
        }

        currentRange = taskStack.pop();
        
        // Base case: If the range has 0 or 1 elements, it's already sorted
        if (currentRange.low >= currentRange.high) {
            if (currentRange.low >= 0 && currentRange.low < n) {
                callback.reportElementStateChange(currentRange.low, ElementState.SORTED);
            }
            currentState = QuickSortInternalState.IDLE;
            return true;
        }

        // Choose the rightmost element as pivot
        pivotIndex = currentRange.high;
        callback.reportElementStateChange(pivotIndex, ElementState.PIVOT);
        
        // Initialize partition indices
        i = currentRange.low;
        j = currentRange.low - 1; // Will be incremented before first use
        
        currentState = QuickSortInternalState.PARTITIONING;
        callback.requestVisualUpdate();
        return true;
    }

    private boolean handlePartitioning() {
        if (i < pivotIndex) {
            callback.reportCompare(i, pivotIndex);
            
            if (elements.get(i).getValue() <= elements.get(pivotIndex).getValue()) {
                j++;
                if (i != j) {
                    swap(i, j);
                } else {
                    callback.reportElementStateChange(i, ElementState.SWAP);
                    callback.requestVisualUpdate();
                }
            } else {
                callback.requestVisualUpdate();
            }
            
            i++;
            return true;
        } else {
            // Partition is complete, place pivot in its correct position
            j++;
            if (j != pivotIndex) {
                swap(j, pivotIndex);
            }
            
            // Mark pivot as sorted
            callback.reportElementStateChange(j, ElementState.SORTED);
            
            currentState = QuickSortInternalState.PARTITION_COMPLETE;
            return true;
        }
    }

    private boolean handlePartitionComplete() {
        // Reset states of elements in the current partition (except the pivot)
        for (int k = currentRange.low; k <= currentRange.high; k++) {
            if (k != j && elements.get(k).getState() != ElementState.SORTED) {
                elements.get(k).setState(ElementState.NORMAL);
            }
        }
        
        // Push the two sub-partitions onto the stack (right first, then left)
        if (j + 1 < currentRange.high) {
            taskStack.push(new QuickSortRange(j + 1, currentRange.high));
        } else if (j + 1 == currentRange.high) {
            // Single element sub-array on the right
            callback.reportElementStateChange(currentRange.high, ElementState.SORTED);
        }
        
        if (currentRange.low < j - 1) {
            taskStack.push(new QuickSortRange(currentRange.low, j - 1));
        } else if (currentRange.low == j - 1) {
            // Single element sub-array on the left
            callback.reportElementStateChange(currentRange.low, ElementState.SORTED);
        }
        
        currentState = QuickSortInternalState.IDLE;
        callback.requestVisualUpdate();
        return true;
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
