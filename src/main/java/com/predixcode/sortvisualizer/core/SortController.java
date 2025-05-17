package com.predixcode.sortvisualizer.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.predixcode.sortvisualizer.algorithms.Algorithm;
import com.predixcode.sortvisualizer.ui.App;
import com.predixcode.sortvisualizer.ui.ControlPanel;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;
import com.predixcode.sortvisualizer.ui.SortPanel;

import javafx.application.Platform;

/**
 * Manages the sorting process, array data, and communication between
 * the UI (SortPanel, ControlPanel) and the sorting algorithms.
 */
public class SortController implements StepCallback {

    private final SortPanel sortPanel;
    private ControlPanel controlPanel; // Will be set via setter

    // ArrayManager now directly provides List<SortElement> and max value
    private ArrayManager arrayManager;
    // activeSortElements will be the list obtained from arrayManager and passed to algorithms
    private List<SortElement> activeSortElements;
    private Algorithm currentAlgorithm;

    private Thread sortThread;
    private final AtomicBoolean isSortingActive = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private int animationDelayMs = 100; // Default animation delay

    // Default values for array generation
    private static final int DEFAULT_ARRAY_SIZE = 50;
    private static final int DEFAULT_MIN_VALUE = 10;
    private static final int DEFAULT_MAX_VALUE = 100;

    /**
     * Constructs a SortController.
     * @param sortPanel The panel used for visualizing the array.
     */
    public SortController(SortPanel sortPanel) {
        this.sortPanel = sortPanel;
        // Initialize arrayManager with default values
        this.arrayManager = new ArrayManager(DEFAULT_ARRAY_SIZE, DEFAULT_MIN_VALUE, DEFAULT_MAX_VALUE);
        // Get the initial list of SortElements from the ArrayManager
        this.activeSortElements = this.arrayManager.getSortElements();
        // Update the UI with this initial data
        updateUiWithCurrentData("Initial Array Generation on Controller instantiation");
    }

    // --- StepCallback Implementation ---
    // These methods operate on `activeSortElements` which is the list the algorithm also works on.

    @Override
    public void reportCompare(int index1, int index2) {
        if (activeSortElements == null || index1 < 0 || index2 < 0 || index1 >= activeSortElements.size() || index2 >= activeSortElements.size()) return;
        Platform.runLater(() -> {
            // Reset all non-sorted, non-pivot elements to NORMAL before highlighting new ones
            for (int i = 0; i < activeSortElements.size(); i++) {
                SortElement el = activeSortElements.get(i);
                if (el.getState() != ElementState.SORTED && el.getState() != ElementState.PIVOT) {
                    el.setState(ElementState.NORMAL);
                }
            }
            activeSortElements.get(index1).setState(ElementState.COMPARE);
            activeSortElements.get(index2).setState(ElementState.COMPARE);
            // Visual update will be triggered by requestVisualUpdate() called by the algorithm later
        });
    }

    @Override
    public void reportSwap(int index1, int index2) {
        if (activeSortElements == null || index1 < 0 || index2 < 0 || index1 >= activeSortElements.size() || index2 >= activeSortElements.size()) return;
        Platform.runLater(() -> {
            // The algorithm's swap method calls this *before* the actual swap in its list.
            // So, we set the state of elements at their current positions to SWAP.
            activeSortElements.get(index1).setState(ElementState.SWAP);
            activeSortElements.get(index2).setState(ElementState.SWAP);
            // The algorithm then swaps them, and requestVisualUpdate() will show them in new positions with SWAP state.
        });
    }

    @Override
    public void reportElementStateChange(int index, ElementState newState) {
        if (activeSortElements == null || index < 0 || index >= activeSortElements.size()) return;
        Platform.runLater(() -> {
            activeSortElements.get(index).setState(newState);
        });
    }

    @Override
    public void reportResetStates(int startIndex, int endIndex) {
        if (activeSortElements == null) return;
        Platform.runLater(() -> {
            for (int k = startIndex; k <= endIndex; k++) {
                if (k >= 0 && k < activeSortElements.size() && activeSortElements.get(k).getState() != ElementState.SORTED) {
                    activeSortElements.get(k).setState(ElementState.NORMAL);
                }
            }
        });
    }

    @Override
    public void reportResetStates(int... indices) {
        if (activeSortElements == null) return;
        Platform.runLater(() -> {
            for (int index : indices) {
                if (index >= 0 && index < activeSortElements.size() && activeSortElements.get(index).getState() != ElementState.SORTED) {
                    activeSortElements.get(index).setState(ElementState.NORMAL);
                }
            }
        });
    }

    @Override
    public int getAnimationDelayMs() {
        return this.animationDelayMs;
    }

    @Override
    public boolean isStopRequested() {
        // If sorting is not active, it's effectively a stop request for the algorithm's loop.
        return !this.isSortingActive.get();
    }

    @Override
    public void reportSortCompleted() {
        Platform.runLater(() -> {
            if (activeSortElements != null) {
                for (SortElement el : activeSortElements) {
                    el.setState(ElementState.SORTED); // Ensure all are marked sorted
                }
                // Pass a new ArrayList to ensure SortPanel detects the change if it relies on list identity.
                sortPanel.updateElements(new ArrayList<>(activeSortElements)); // Final update
            }
            if (controlPanel != null) controlPanel.enableControls();
            isSortingActive.set(false); // Update the sorting state
            System.out.println("Sort completed visually (reported by algorithm).");
        });
    }

    @Override
    public void requestVisualUpdate() {
        if (activeSortElements != null && sortPanel != null) {
            Platform.runLater(() -> {
                // Pass a new ArrayList to SortPanel.
                sortPanel.updateElements(new ArrayList<>(activeSortElements));
            });
        }
    }

    // --- Controller Logic ---

    public void setControlPanel(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    public void setAlgorithm(Algorithm algorithm) {
        if (isSortingActive.get()) {
            System.out.println("Cannot change algorithm while sorting is in progress.");
            // Consider re-selecting the current algorithm in the ComboBox if the UI allows changing it.
            return;
        }
        this.currentAlgorithm = algorithm;
        System.out.println("Algorithm set to: " + (algorithm != null ? algorithm.getName() : "None"));
    }

    /**
     * Helper method to update the SortPanel with the current activeSortElements
     * from the ArrayManager and set the correct scaling factor.
     * @param reason A string for logging purposes, explaining why the update is happening.
     */
    private void updateUiWithCurrentData(String reason) {
        System.out.println("SortController: " + reason);
        if (this.arrayManager == null) {
            System.err.println("SortController: ArrayManager is null. Cannot update UI.");
            // Initialize activeSortElements to an empty list to prevent NullPointerExceptions downstream
            this.activeSortElements = new ArrayList<>();
        } else {
             // Get a fresh copy of SortElements from ArrayManager
            this.activeSortElements = this.arrayManager.getSortElements();
        }
        
        final int maxValue = (this.arrayManager != null) ? this.arrayManager.getMaxValueInCurrentArray() : 1;

        if (sortPanel != null) {
            Platform.runLater(() -> {
                sortPanel.setMaxValueForScaling(maxValue);
                // Pass a new ArrayList to SortPanel to ensure it detects changes.
                sortPanel.updateElements(new ArrayList<>(this.activeSortElements));
            });
        } else {
            System.err.println("SortController: SortPanel is null. Cannot update UI.");
        }
    }

    /**
     * Generates a new array using ArrayManager and updates the UI.
     * @param size The number of elements in the new array.
     * @param minVal The minimum value for elements.
     * @param maxVal The maximum value for elements (exclusive).
     */
    public void generateNewArray(int size, int minVal, int maxVal) {
        if (isSortingActive.get()) {
            stopSort(); // Attempt to stop any ongoing sort.
            // Note: stopSort() is asynchronous. For true safety, one might need to
            // wait for the sortThread to terminate or use a more robust state machine.
            // For this visualizer, we assume stopSort() signals the thread and proceed.
        }
        
        // Re-initialize or use existing ArrayManager to generate new elements.
        // If arrayManager is null (e.g. first time after an error), create it.
        if (this.arrayManager == null) {
            this.arrayManager = new ArrayManager(size, minVal, maxVal);
        } else {
            this.arrayManager.generateNewElements(size, minVal, maxVal);
        }
        
        // Update activeSortElements and refresh the UI
        updateUiWithCurrentData("New Array Generated in Controller");
        
        if (currentAlgorithm != null) {
            currentAlgorithm.reset(); // Reset the state of the currently selected algorithm
        }
        if (controlPanel != null) {
            Platform.runLater(controlPanel::enableControls); // Ensure controls are enabled
        }
        System.out.println("SortController: New array generated. Size: " + size);
    }

    /**
     * Starts the sorting process using the currently selected algorithm.
     */
    public void startSort() {
        if (currentAlgorithm == null) {
            App.showAlert("Error", "Please select a sorting algorithm first.");
            return;
        }
        if (activeSortElements == null || activeSortElements.isEmpty()) {
            // This might happen if generateNewArray failed or was called with size 0
            App.showAlert("Error", "Array is empty. Please generate an array first.");
            return;
        }
        if (isSortingActive.compareAndSet(false, true)) {
            isPaused.set(false);
            if (controlPanel != null) Platform.runLater(controlPanel::disableControlsDuringSort);

            // activeSortElements is already populated from ArrayManager via generateNewArray.
            // Initialize the algorithm with this list. The algorithm will modify it directly.
            currentAlgorithm.initialize(this.activeSortElements, this);

            sortThread = new Thread(() -> {
                try {
                    boolean moreSteps = true;
                    // Initial visual update to show the array before the first algorithm step
                    requestVisualUpdate(); 
                    Thread.sleep(getAnimationDelayMs()); // Pause to see initial state

                    while (moreSteps && isSortingActive.get() && !Thread.currentThread().isInterrupted()) {
                        if (isPaused.get()) {
                            Thread.sleep(100); // Check pause state periodically
                            continue;
                        }

                        moreSteps = currentAlgorithm.nextStep(); // Execute one step of the algorithm

                        // The algorithm calls requestVisualUpdate() via the callback for UI refresh.
                        // The algorithm also calls reportSortCompleted() when it's done.

                        if (moreSteps && isSortingActive.get()) { // Only sleep if algo has more steps and not stopped
                            Thread.sleep(getAnimationDelayMs());
                        }
                    }
                    // Loop exits if !moreSteps (algorithm finished), !isSortingActive (stopped), or interrupted.
                    // reportSortCompleted() is called by the algorithm itself upon natural completion.
                } catch (InterruptedException e) {
                    System.out.println("Sort thread interrupted.");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                } catch (Exception e) {
                    System.err.println("Error during sorting steps: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> App.showAlert("Sorting Error", "An error occurred during sorting: " + e.getMessage()));
                } finally {
                    // This block runs regardless of how the try block exits.
                    boolean wasAlgorithmStillMarkedAsSorting = isSortingActive.getAndSet(false); // Atomically set to false
                    isPaused.set(false);

                    Platform.runLater(() -> {
                        if (controlPanel != null) controlPanel.enableControls();
                        
                        // If sort was stopped prematurely or interrupted, and algorithm didn't finish
                        if (currentAlgorithm != null && !currentAlgorithm.isSorted() && wasAlgorithmStillMarkedAsSorting) {
                            System.out.println("SortController: Sort did not complete naturally or was stopped. Resetting visual states.");
                            if (activeSortElements != null) {
                                for(SortElement el : activeSortElements) {
                                    if (el.getState() != ElementState.SORTED) { // Don't override already sorted elements
                                        el.setState(ElementState.NORMAL);
                                    }
                                }
                                sortPanel.updateElements(new ArrayList<>(activeSortElements));
                            }
                        } else if (currentAlgorithm != null && currentAlgorithm.isSorted()) {
                             // This case should be covered by reportSortCompleted, but as a safeguard:
                             System.out.println("SortController: Ensuring final sorted state is displayed post-completion.");
                             for (SortElement el : activeSortElements) el.setState(ElementState.SORTED);
                             sortPanel.updateElements(new ArrayList<>(activeSortElements));
                        }
                        System.out.println("SortController: Sorting process thread finished.");
                    });
                }
            });
            sortThread.setDaemon(true); // Allows application to exit if this thread is running
            sortThread.start();
        } else {
            System.out.println("SortController: Sorting is already in progress or another action is pending.");
        }
    }

    /**
     * Stops the current sorting process.
     */
    public void stopSort() {
        System.out.println("SortController: Attempting to stop sort...");
        // Set isSortingActive to false. The sorting loop checks this flag.
        // Also, interrupt the thread in case it's sleeping.
        if (isSortingActive.getAndSet(false)) { // Atomically set to false and get previous value
            if (sortThread != null && sortThread.isAlive()) {
                sortThread.interrupt(); // Interrupt the sleep in the sorting loop
            }
            System.out.println("SortController: Stop signal sent to sorting thread.");
            // The 'finally' block in the sortThread's Runnable will handle enabling controls
            // and resetting visual state if the sort was indeed stopped prematurely.
        } else {
            System.out.println("SortController: Sort was not active or already stopping.");
            // If for some reason controls are disabled and sort wasn't active, re-enable them.
            if (controlPanel != null) {
                 Platform.runLater(controlPanel::enableControls);
            }
        }
    }

    public void pauseSort() {
        if (isSortingActive.get() && !isPaused.get()) {
            isPaused.set(true);
            System.out.println("Sorting paused.");
        }
    }

    public void resumeSort() {
        if (isSortingActive.get() && isPaused.get()) {
            isPaused.set(false);
            System.out.println("Sorting resumed.");
        }
    }

    public void setAnimationDelay(int delay) {
        this.animationDelayMs = Math.max(0, delay); // Ensure non-negative
        System.out.println("Animation delay set to: " + this.animationDelayMs + "ms");
    }

    public int getCurrentAnimationDelay() {
        return animationDelayMs;
    }

    public boolean isCurrentlySorting() {
        return isSortingActive.get();
    }
}
