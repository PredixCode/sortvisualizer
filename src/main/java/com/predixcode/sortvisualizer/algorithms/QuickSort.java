package com.predixcode.sortvisualizer.algorithms;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class QuickSort extends AbstractSortAlgorithm {

    // Stack to manage sub-arrays for iterative QuickSort
    private Deque<Integer> stack;
    private int currentBegin;
    private int currentEnd;
    private int pivotIndexForHighlight; // To keep pivot highlighted during partition
    private boolean isSortedFlag = false;

    private enum QuickSortInternalState {
        SELECTING_SUB_ARRAY, // Pop from stack
        PARTITIONING,        // Performing one step of partition
        PUSHING_SUB_ARRAYS   // After partition, push new sub-arrays to stack
    }
    private QuickSortInternalState currentState;

    // Variables for partition step
    private int pivotValue;
    private int i_partition; // 'i' in Lomuto partition
    private int j_partition; // 'j' in Lomuto partition

    public QuickSort() {
        // Constructor
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.stack = new ArrayDeque<>();
        this.isSortedFlag = (this.elements == null || this.elements.isEmpty());
        this.pivotIndexForHighlight = -1;

        if (!this.isSortedFlag && this.elements != null && !this.elements.isEmpty()) {
            // Push initial full array range onto the stack
            stack.push(this.elements.size() - 1); // end
            stack.push(0);                         // begin
            currentState = QuickSortInternalState.SELECTING_SUB_ARRAY;
        } else {
            currentState = null; // Or some terminal state
        }
        
        // Visually reset all elements
        if (this.elements != null && this.callback != null) {
            for (SortElement el : this.elements) {
                el.setState(ElementState.NORMAL);
            }
        }
    }

    @Override
    public String getName() {
        return "Quick Sort";
    }

    @Override
    public boolean nextStep() {
        if (isSortedFlag || callback.isStopRequested() || elements.isEmpty()) {
            if (!isSortedFlag && elements != null && !elements.isEmpty()) { // Stopped early
                 for (SortElement el : elements) {
                    if (el.getState() != ElementState.SORTED) el.setState(ElementState.NORMAL);
                }
                callback.requestVisualUpdate();
            }
            isSortedFlag = true;
            return false;
        }

        switch (currentState) {
            case SELECTING_SUB_ARRAY:
                return handleSelectSubArray();
            case PARTITIONING:
                return handlePartitioningStep();
            case PUSHING_SUB_ARRAYS:
                return handlePushingSubArrays();
            default:
                isSortedFlag = true;
                return false;
        }
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }

    private boolean handleSelectSubArray() {
        if (stack.isEmpty()) {
            isSortedFlag = true; // All sub-arrays processed
            // Mark all as sorted (some might have been missed if partition was trivial)
            for(SortElement el : elements) el.setState(ElementState.SORTED);
            // callback.reportSortCompleted(); // Controller will handle this
            return false;
        }

        // Pop next sub-array to process
        currentBegin = stack.pop();
        currentEnd = stack.pop();

        if (currentBegin < currentEnd) {
            // Setup for partitioning
            pivotValue = elements.get(currentEnd).getValue();
            callback.reportElementStateChange(currentEnd, ElementState.PIVOT); // Highlight pivot
            pivotIndexForHighlight = currentEnd;
            i_partition = currentBegin - 1;
            j_partition = currentBegin;
            currentState = QuickSortInternalState.PARTITIONING;
        } else {
            // This sub-array is 0 or 1 element, effectively sorted in context
            // Mark elements in this small range as sorted if not already
            if (currentBegin <= currentEnd && currentBegin >= 0 && currentEnd < elements.size()) {
                for (int k = currentBegin; k <= currentEnd; k++) {
                     if(elements.get(k).getState() != ElementState.SORTED)
                        callback.reportElementStateChange(k, ElementState.SORTED);
                }
            }
            currentState = QuickSortInternalState.SELECTING_SUB_ARRAY; // Look for more work
        }
        return true; // Continue to next state or next sub-array
    }

    private boolean handlePartitioningStep() {
        // Reset previous comparison highlights (excluding pivot)
        for(int k=0; k < elements.size(); k++) {
            if (k != pivotIndexForHighlight && elements.get(k).getState() != ElementState.SORTED) {
                elements.get(k).setState(ElementState.NORMAL);
            }
        }

        if (j_partition < currentEnd) { // Loop for partitioning (j from begin to end-1)
            callback.reportCompare(j_partition, pivotIndexForHighlight); // Compare current element with pivot

            if (elements.get(j_partition).getValue() <= pivotValue) {
                i_partition++;
                if (i_partition != j_partition) { // Avoid swapping element with itself
                    swap(i_partition, j_partition); // This calls reportSwap and updates UI
                }
            }
            j_partition++;
            return true; // More partitioning steps for this sub-array
        } else {
            // Partitioning loop for this sub-array is done. Place pivot.
            // Swap pivot (at currentEnd) with element at i_partition + 1
            if (pivotIndexForHighlight != (i_partition + 1)) {
                 swap(i_partition + 1, currentEnd);
            }
            // Pivot is now at its sorted position
            callback.reportElementStateChange(i_partition + 1, ElementState.SORTED);
            pivotIndexForHighlight = -1; // Clear pivot highlight for next sub-array

            // Store the partition index to use for pushing new sub-arrays
            this.pivotIndexForHighlight = i_partition + 1; // Re-using field temporarily
            currentState = QuickSortInternalState.PUSHING_SUB_ARRAYS;
            return true; // Transition to pushing state
        }
    }

    private boolean handlePushingSubArrays() {
        int partitionIndex = this.pivotIndexForHighlight; // Retrieve stored partition index

        // Reset states of the just-partitioned range (excluding the now-sorted pivot)
        for (int k = currentBegin; k <= currentEnd; k++) {
            if (elements.get(k).getState() != ElementState.SORTED) {
                elements.get(k).setState(ElementState.NORMAL);
            }
        }

        // Push left sub-array (if it has elements)
        if (currentBegin < partitionIndex - 1) {
            stack.push(partitionIndex - 1); // end of left sub-array
            stack.push(currentBegin);       // begin of left sub-array
        } else if (currentBegin <= partitionIndex - 1) { // Single element or empty left part
             for(int k = currentBegin; k <= partitionIndex -1; k++) callback.reportElementStateChange(k, ElementState.SORTED);
        }


        // Push right sub-array (if it has elements)
        if (partitionIndex + 1 < currentEnd) {
            stack.push(currentEnd);           // end of right sub-array
            stack.push(partitionIndex + 1);   // begin of right sub-array
        } else if (partitionIndex + 1 <= currentEnd) { // Single element or empty right part
            for(int k = partitionIndex + 1; k <= currentEnd; k++) callback.reportElementStateChange(k, ElementState.SORTED);
        }

        currentState = QuickSortInternalState.SELECTING_SUB_ARRAY; // Go back to pick next sub-array
        return true; // Continue processing
    }
}
