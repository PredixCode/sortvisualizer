
package com.predixcode.sortvisualizer.algorithms;

import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class HeapSort extends AbstractSortAlgorithm {

    private int n; // Size of the array being sorted
    private int heapSize; // Current size of the heap
    private int i; // Index for the current phase
    private int largest; // Index of the largest element in heapify
    private int sortedCount; // Number of elements sorted

    private boolean isSortedFlag = false;

    private enum HeapSortInternalState {
        BUILD_HEAP, // Building the max heap
        HEAPIFY, // Heapifying a subtree
        EXTRACT_MAX, // Extracting the maximum element
        COMPLETE // Sorting is complete
    }
    private HeapSortInternalState currentState;
    
    // For heapify state
    private int heapifyRoot; // Current root being heapified
    private int heapifyLeft; // Left child of the root
    private int heapifyRight; // Right child of the root
    private boolean heapifySwapped; // Whether a swap occurred during heapify

    public HeapSort() {
        // Constructor
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.n = (this.elements != null) ? this.elements.size() : 0;
        this.heapSize = n;
        this.i = n / 2 - 1; // Start from the last non-leaf node
        this.sortedCount = 0;
        this.isSortedFlag = (n <= 1);
        this.currentState = HeapSortInternalState.BUILD_HEAP;
        
        if (this.elements != null && this.callback != null) {
            for (SortElement el : this.elements) el.setState(ElementState.NORMAL);
        }
    }

    @Override
    public String getName() {
        return "Heap Sort";
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
            case BUILD_HEAP:
                return handleBuildHeap();
                
            case HEAPIFY:
                return handleHeapify();
                
            case EXTRACT_MAX:
                return handleExtractMax();
                
            case COMPLETE:
                isSortedFlag = true;
                return false;
                
            default:
                isSortedFlag = true;
                return false;
        }
    }

    private boolean handleBuildHeap() {
        if (i >= 0) {
            // Start heapify for the current node
            heapifyRoot = i;
            heapifyLeft = 2 * i + 1;
            heapifyRight = 2 * i + 2;
            largest = heapifyRoot;
            heapifySwapped = false;
            
            // Highlight the current subtree
            callback.reportElementStateChange(heapifyRoot, ElementState.PIVOT);
            if (heapifyLeft < heapSize) {
                callback.reportElementStateChange(heapifyLeft, ElementState.COMPARE);
            }
            if (heapifyRight < heapSize) {
                callback.reportElementStateChange(heapifyRight, ElementState.COMPARE);
            }
            
            currentState = HeapSortInternalState.HEAPIFY;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Heap is built, start extracting max elements
            i = heapSize - 1;
            currentState = HeapSortInternalState.EXTRACT_MAX;
            return true;
        }
    }

    private boolean handleHeapify() {
        // Find the largest among root, left child and right child
        if (heapifyLeft < heapSize && !heapifySwapped) {
            callback.reportCompare(largest, heapifyLeft);
            if (elements.get(heapifyLeft).getValue() > elements.get(largest).getValue()) {
                largest = heapifyLeft;
            }
            callback.requestVisualUpdate();
            heapifySwapped = true;
            return true;
        } else if (heapifyRight < heapSize && heapifySwapped) {
            callback.reportCompare(largest, heapifyRight);
            if (elements.get(heapifyRight).getValue() > elements.get(largest).getValue()) {
                largest = heapifyRight;
            }
            callback.requestVisualUpdate();
            heapifySwapped = false;
            
            // If largest is not root, swap and continue heapifying
            if (largest != heapifyRoot) {
                swap(heapifyRoot, largest);
                
                // Continue heapifying the affected subtree
                heapifyRoot = largest;
                heapifyLeft = 2 * heapifyRoot + 1;
                heapifyRight = 2 * heapifyRoot + 2;
                largest = heapifyRoot;
                
                // Highlight the new subtree
                for (int k = 0; k < heapSize; k++) {
                    if (elements.get(k).getState() != ElementState.SORTED) {
                        elements.get(k).setState(ElementState.NORMAL);
                    }
                }
                callback.reportElementStateChange(heapifyRoot, ElementState.PIVOT);
                if (heapifyLeft < heapSize) {
                    callback.reportElementStateChange(heapifyLeft, ElementState.COMPARE);
                }
                if (heapifyRight < heapSize) {
                    callback.reportElementStateChange(heapifyRight, ElementState.COMPARE);
                }
                callback.requestVisualUpdate();
                return true;
            } else {
                // Reset states and move to the next node in build heap
                for (int k = 0; k < heapSize; k++) {
                    if (elements.get(k).getState() != ElementState.SORTED) {
                        elements.get(k).setState(ElementState.NORMAL);
                    }
                }
                i--;
                currentState = HeapSortInternalState.BUILD_HEAP;
                callback.requestVisualUpdate();
                return true;
            }
        } else {
            // This case should not be reached normally
            i--;
            currentState = HeapSortInternalState.BUILD_HEAP;
            return true;
        }
    }

    private boolean handleExtractMax() {
        if (i > 0) {
            // Swap the root (maximum element) with the last element
            callback.reportElementStateChange(0, ElementState.PIVOT);
            callback.reportElementStateChange(i, ElementState.SWAP);
            swap(0, i);
            
            // Mark the last element as sorted
            callback.reportElementStateChange(i, ElementState.SORTED);
            sortedCount++;
            
            // Reduce heap size and heapify the root
            heapSize--;
            heapifyRoot = 0;
            heapifyLeft = 1;
            heapifyRight = 2;
            largest = heapifyRoot;
            heapifySwapped = false;
            
            // Highlight the new subtree for heapify
            for (int k = 0; k < heapSize; k++) {
                if (elements.get(k).getState() != ElementState.SORTED) {
                    elements.get(k).setState(ElementState.NORMAL);
                }
            }
            callback.reportElementStateChange(heapifyRoot, ElementState.PIVOT);
            if (heapifyLeft < heapSize) {
                callback.reportElementStateChange(heapifyLeft, ElementState.COMPARE);
            }
            if (heapifyRight < heapSize) {
                callback.reportElementStateChange(heapifyRight, ElementState.COMPARE);
            }
            
            currentState = HeapSortInternalState.HEAPIFY;
            i--;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Last element, mark it as sorted
            callback.reportElementStateChange(0, ElementState.SORTED);
            sortedCount++;
            
            // Check if all elements are sorted
            if (sortedCount == n) {
                currentState = HeapSortInternalState.COMPLETE;
                isSortedFlag = true;
            }
            
            callback.requestVisualUpdate();
            return !isSortedFlag;
        }
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }
}
