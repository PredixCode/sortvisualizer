package com.predixcode.sortvisualizer.algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class MergeSort extends AbstractSortAlgorithm {

    // Structure to hold ranges for merging/sorting
    private static class MergeRange {
        int left, mid, right; // For merge operation
        int start, end;      // For sort operation (recursive call simulation)
        boolean forMerge;    // True if this range is for a merge, false for a sort call

        MergeRange(int s, int e, boolean isMerge) {
            this.start = s; this.end = e; this.forMerge = isMerge;
        }
        MergeRange(int l, int m, int r) {
            this.left = l; this.mid = m; this.right = r; this.forMerge = true;
        }
    }

    private Deque<MergeRange> taskStack; // To simulate recursion iteratively

    // State for current merge operation
    private List<SortElement> tempMergeArray;
    private int k_merge; // Index for tempMergeArray
    private int i_merge; // Index for left sub-array
    private int j_merge; // Index for right sub-array
    private MergeRange currentMergeOp; // Holds left, mid, right for current merge
    private boolean isSortedFlag = false;

    private enum MergeSortInternalState {
        IDLE,               // Initial state or between major operations
        SPLITTING,          // Simulating recursive calls by pushing to stack
        PREPARING_MERGE,    // Setting up indices for a merge operation
        MERGING_COMPARE,    // Comparing elements from two sub-arrays
        MERGING_COPY_LEFT,  // Copying remaining from left
        MERGING_COPY_RIGHT, // Copying remaining from right
        COPYING_BACK        // Copying from temp array back to main elements list
    }
    private MergeSortInternalState currentState;
    private int n;

    public MergeSort() {
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
        this.tempMergeArray = new ArrayList<>();
        this.isSortedFlag = (n <= 1);
        this.currentState = MergeSortInternalState.IDLE;
        this.currentMergeOp = null;

        if (!isSortedFlag && n > 0) {
            taskStack.push(new MergeRange(0, n - 1, false)); // Initial sort task for the whole array
            currentState = MergeSortInternalState.SPLITTING;
        }
        
        if (this.elements != null && this.callback != null) {
            for (SortElement el : this.elements) el.setState(ElementState.NORMAL);
        }
    }

    @Override
    public String getName() {
        return "Merge Sort";
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
                if (!taskStack.isEmpty()) currentState = MergeSortInternalState.SPLITTING;
                else { isSortedFlag = true; return false;} // Should be caught by stack empty in SPLITTING
                return true;

            case SPLITTING:
                return handleSplitting();

            case PREPARING_MERGE:
                return handlePreparingMerge();

            case MERGING_COMPARE:
                return handleMergingCompare();

            case MERGING_COPY_LEFT:
            case MERGING_COPY_RIGHT:
                return handleMergingCopyRemaining();

            case COPYING_BACK:
                return handleCopyingBack();

            default:
                isSortedFlag = true;
                return false;
        }
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }

    private boolean handleSplitting() {
        if (taskStack.isEmpty()) {
            // This means all sort tasks are done, and all necessary merge tasks should have been enqueued and processed.
            // Or, if we started empty, it means we are done.
            isSortedFlag = true;
             for(SortElement el : elements) el.setState(ElementState.SORTED); // Final assurance
            // callback.reportSortCompleted(); // Controller handles this
            return false;
        }

        MergeRange task = taskStack.pop();

        if (task.forMerge) { // This task was actually a placeholder to trigger merge after its children sorted
            currentMergeOp = task; // It contains left, mid, right
            currentState = MergeSortInternalState.PREPARING_MERGE;
            return true;
        }

        // Else, it's a sort task (start, end)
        int start = task.start;
        int end = task.end;

        if (start < end) {
            int mid = start + (end - start) / 2;

            // Crucial: Push merge task for (start, mid, end) *before* its children sort tasks.
            // It will be popped *after* its children complete.
            taskStack.push(new MergeRange(start, mid, end)); // This is forMerge=true implicitly by constructor

            // Push right child sort task
            taskStack.push(new MergeRange(mid + 1, end, false));
            // Push left child sort task
            taskStack.push(new MergeRange(start, mid, false));
            
            // Highlight the range being split (optional)
            // for(int i=start; i<=end; i++) callback.reportElementStateChange(i, ElementState.PIVOT);
            // callback.requestVisualUpdate();
            // for(int i=start; i<=end; i++) callback.reportElementStateChange(i, ElementState.NORMAL);


        } else {
            // Single element or invalid range, effectively sorted in this context.
            // If start == end, mark as sorted.
            if (start == end && start >=0 && start < n) {
                 elements.get(start).setState(ElementState.SORTED); // Base case of recursion
            }
        }
        // Still in SPLITTING state, will pick up next task from stack or finish if stack is empty.
        return true;
    }

    private boolean handlePreparingMerge() {
        // currentMergeOp should be set from SPLITTING state (when a forMerge=true task was popped)
        if (currentMergeOp == null) { // Should not happen
            currentState = MergeSortInternalState.SPLITTING; return true;
        }

        tempMergeArray.clear();
        i_merge = currentMergeOp.left;
        j_merge = currentMergeOp.mid + 1;
        k_merge = 0; // For our separate tempMergeArray

        // Highlight ranges being merged
        for (int k = currentMergeOp.left; k <= currentMergeOp.right; k++) {
            callback.reportElementStateChange(k, ElementState.PIVOT); // Use PIVOT or a custom "MERGING_SOURCE" state
        }
        callback.requestVisualUpdate();
        currentState = MergeSortInternalState.MERGING_COMPARE;
        return true;
    }

    private boolean handleMergingCompare() {
        if (i_merge <= currentMergeOp.mid && j_merge <= currentMergeOp.right) {
            callback.reportCompare(i_merge, j_merge);
            if (elements.get(i_merge).getValue() <= elements.get(j_merge).getValue()) {
                // Highlight elements.get(i_merge) as being chosen
                callback.reportElementStateChange(i_merge, ElementState.SWAP); // SWAP state to indicate it's being moved
                tempMergeArray.add(elements.get(i_merge));
                i_merge++;
            } else {
                callback.reportElementStateChange(j_merge, ElementState.SWAP);
                tempMergeArray.add(elements.get(j_merge));
                j_merge++;
            }
            callback.requestVisualUpdate();
            return true;
        } else {
            // One of the sub-arrays is exhausted
            currentState = (i_merge <= currentMergeOp.mid) ? MergeSortInternalState.MERGING_COPY_LEFT : MergeSortInternalState.MERGING_COPY_RIGHT;
            return true;
        }
    }

    private boolean handleMergingCopyRemaining() {
        if (currentState == MergeSortInternalState.MERGING_COPY_LEFT && i_merge <= currentMergeOp.mid) {
            callback.reportElementStateChange(i_merge, ElementState.SWAP);
            tempMergeArray.add(elements.get(i_merge));
            i_merge++;
            callback.requestVisualUpdate();
            return true;
        } else if (currentState == MergeSortInternalState.MERGING_COPY_RIGHT && j_merge <= currentMergeOp.right) {
            callback.reportElementStateChange(j_merge, ElementState.SWAP);
            tempMergeArray.add(elements.get(j_merge));
            j_merge++;
            callback.requestVisualUpdate();
            return true;
        } else {
            // All elements copied to tempMergeArray
            currentState = MergeSortInternalState.COPYING_BACK;
            k_merge = 0; // Reset for copying back
            return true;
        }
    }

    private boolean handleCopyingBack() {
        if (k_merge < tempMergeArray.size()) {
            int originalIndex = currentMergeOp.left + k_merge;
            elements.set(originalIndex, tempMergeArray.get(k_merge));
            // The elements in tempMergeArray were already SortElement objects.
            // Their state was SWAP when copied. Now they are in place.
            callback.reportElementStateChange(originalIndex, ElementState.SORTED); // Or NORMAL, then mark sorted later
            k_merge++;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Finished copying back for this merge operation
            // Mark the entire merged range as sorted
            for (int k_idx = currentMergeOp.left; k_idx <= currentMergeOp.right; k_idx++) {
                 elements.get(k_idx).setState(ElementState.SORTED);
            }
            currentMergeOp = null;
            currentState = MergeSortInternalState.SPLITTING; // Go back to see if more tasks on stack
            callback.requestVisualUpdate();
            return true;
        }
    }
}
