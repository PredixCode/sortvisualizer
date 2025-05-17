package com.predixcode.sortvisualizer.algorithms;

import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class InsertionSort extends AbstractSortAlgorithm {

    private int i; // Outer loop index: current element to be inserted into sorted portion
    private int j; // Inner loop index: used for shifting elements in the sorted portion
    private SortElement keyElement; // The element currently being inserted
    private boolean isSortedFlag = false;

    private enum InsertionSortInternalState {
        SELECTING_KEY,  // Picking the next element to insert
        SHIFTING_ELEMENTS, // Comparing key with sorted portion and shifting
        INSERTING_KEY   // Placing the key in its correct position
    }
    private InsertionSortInternalState currentState;
    private int n; // Size of the array

    public InsertionSort() {
        // Constructor
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback); // Sets this.elements, this.callback, and calls reset()
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.i = 1; // Insertion sort starts by considering the second element (index 1)
        this.j = 0;
        this.keyElement = null;
        this.isSortedFlag = (n <= 1); // Array of 0 or 1 elements is sorted
        this.currentState = InsertionSortInternalState.SELECTING_KEY;

        if (this.elements != null && this.callback != null) {
            for (int k = 0; k < n; k++) {
                this.elements.get(k).setState(ElementState.NORMAL);
            }
            if (n > 0 && !isSortedFlag) { // Mark first element as sorted if array has elements
                this.elements.get(0).setState(ElementState.SORTED);
            }
        }
    }

    @Override
    public String getName() {
        return "Insertion Sort";
    }

    @Override
    public boolean nextStep() {
        if (isSortedFlag || callback.isStopRequested() || n == 0) {
            if (!isSortedFlag && n > 0) { // Stopped early or before completion
                for (SortElement el : elements) { // Reset non-sorted visual states
                    if (el.getState() != ElementState.SORTED) el.setState(ElementState.NORMAL);
                }
                callback.requestVisualUpdate();
            }
            isSortedFlag = true;
            return false;
        }

        switch (currentState) {
            case SELECTING_KEY:
                return handleSelectingKey();
            case SHIFTING_ELEMENTS:
                return handleShiftingElements();
            case INSERTING_KEY:
                return handleInsertingKey();
            default:
                isSortedFlag = true;
                return false;
        }
    }

    private boolean handleSelectingKey() {
        // Reset states from previous step (except already sorted ones)
        for(int k=0; k < i && k < n; k++) { // Ensure elements up to i-1 are marked sorted
            if(elements.get(k).getState() != ElementState.SORTED) elements.get(k).setState(ElementState.SORTED);
        }
        for(int k=i; k < n; k++) { // Reset elements yet to be processed
             if(elements.get(k).getState() != ElementState.SORTED) elements.get(k).setState(ElementState.NORMAL);
        }


        if (i < n) {
            keyElement = elements.get(i); // Get the actual SortElement object
            callback.reportElementStateChange(i, ElementState.PIVOT); // Highlight key element
            j = i - 1;
            currentState = InsertionSortInternalState.SHIFTING_ELEMENTS;
            callback.requestVisualUpdate();
            return true;
        } else {
            // All elements processed
            isSortedFlag = true;
            for(SortElement el : elements) el.setState(ElementState.SORTED); // Final pass to mark all sorted
            callback.requestVisualUpdate();
            return false;
        }
    }

    private boolean handleShiftingElements() {
        if (j >= 0 && elements.get(j).getValue() > keyElement.getValue()) {
            // Highlight comparison
            callback.reportCompare(j, i); // Comparing element at 'j' with original position of keyElement 'i'
                                          // or with keyElement itself if we had a way to show that
            
            // Shift element at j to j+1
            SortElement elementToShift = elements.get(j);
            elements.set(j + 1, elementToShift); // Move the SortElement object
            callback.reportElementStateChange(j + 1, ElementState.SWAP); // Mark as being moved/swapped
            if (j==i-1) { // If this is the first shift for the current keyElement
                 elements.get(i).setState(ElementState.NORMAL); // Original key position becomes "empty" or normal
            }


            j--; // Move to the next element in the sorted portion
            callback.requestVisualUpdate();
            return true;
        } else {
            // Found the correct position for keyElement, or start of array reached
            currentState = InsertionSortInternalState.INSERTING_KEY;
            // No visual update needed here, next state will handle it
            return true; // Proceed to insert
        }
    }

    private boolean handleInsertingKey() {
        // Insert keyElement at arr[j + 1]
        elements.set(j + 1, keyElement);
        callback.reportElementStateChange(j + 1, ElementState.SORTED); // Key element is now in sorted position

        // The element that was originally at i (keyElement) is now at j+1.
        // Other elements have been shifted.
        // All elements from 0 to 'i' are now sorted.
        for (int k = 0; k <= i; k++) {
             if (elements.get(k).getState() != ElementState.SORTED) { // Ensure they are marked sorted
                elements.get(k).setState(ElementState.SORTED);
             }
        }
        
        i++; // Move to the next element to be inserted
        currentState = InsertionSortInternalState.SELECTING_KEY;
        callback.requestVisualUpdate();
        return true; // Continue if i < n
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
