package com.predixcode.sortvisualizer.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private ControlPanel controlPanel;

    private ArrayManager arrayManager;
    private Algorithm currentAlgorithm;
    private List<SortElement> activeSortElements;

    private Thread sortThread;
    private final AtomicBoolean isSortingActive = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private int animationDelayMs = 100;

    private static final int DEFAULT_ARRAY_SIZE = 50;
    private static final int DEFAULT_MIN_VALUE = 10;
    private static final int DEFAULT_MAX_VALUE = 100;

    // To keep track of indices whose states were recently changed to transient (COMPARE, SWAP)
    private Set<Integer> lastTransientStateIndices = new HashSet<>();

    public SortController(SortPanel sortPanel) {
        this.sortPanel = sortPanel;
        this.arrayManager = new ArrayManager(DEFAULT_ARRAY_SIZE, DEFAULT_MIN_VALUE, DEFAULT_MAX_VALUE);
        this.activeSortElements = this.arrayManager.getSortElements();
        updateUiWithCurrentData("Initial Array Generation on Controller instantiation");
    }

    /**
     * Resets specified transient state indices to NORMAL if they are not SORTED or PIVOT.
     * Clears the tracking set afterwards.
     */
    private void resetLastTransientStates() {
        for (Integer idx : lastTransientStateIndices) {
            if (idx >= 0 && idx < activeSortElements.size()) {
                SortElement el = activeSortElements.get(idx);
                if (el.getState() != ElementState.SORTED && el.getState() != ElementState.PIVOT) {
                    el.setState(ElementState.NORMAL);
                }
            }
        }
        lastTransientStateIndices.clear();
    }

    @Override
    public void reportCompare(int index1, int index2) {
        if (activeSortElements == null || index1 < 0 || index2 < 0 || index1 >= activeSortElements.size() || index2 >= activeSortElements.size()) return;
        Platform.runLater(() -> {
            resetLastTransientStates(); // Reset previously highlighted COMPARE/SWAP elements

            activeSortElements.get(index1).setState(ElementState.COMPARE);
            activeSortElements.get(index2).setState(ElementState.COMPARE);
            lastTransientStateIndices.add(index1);
            lastTransientStateIndices.add(index2);
        });
    }

    @Override
    public void reportSwap(int index1, int index2) {
        if (activeSortElements == null || index1 < 0 || index2 < 0 || index1 >= activeSortElements.size() || index2 >= activeSortElements.size()) return;
        Platform.runLater(() -> {
            resetLastTransientStates(); // Reset previously highlighted COMPARE/SWAP elements

            activeSortElements.get(index1).setState(ElementState.SWAP);
            activeSortElements.get(index2).setState(ElementState.SWAP);
            lastTransientStateIndices.add(index1);
            lastTransientStateIndices.add(index2);
        });
    }

    @Override
    public void reportElementStateChange(int index, ElementState newState) {
        if (activeSortElements == null || index < 0 || index >= activeSortElements.size()) return;
        Platform.runLater(() -> {
            // If an element is being set to a more permanent state,
            // or to NORMAL, it's no longer in a "last transient" state from compare/swap.
            if (newState == ElementState.SORTED || newState == ElementState.PIVOT || newState == ElementState.NORMAL) {
                lastTransientStateIndices.remove(index);
            }
            activeSortElements.get(index).setState(newState);
        });
    }

    @Override
    public void reportResetStates(int startIndex, int endIndex) {
        if (activeSortElements == null) return;
        Platform.runLater(() -> {
            for (int k = startIndex; k <= endIndex; k++) {
                if (k >= 0 && k < activeSortElements.size()) {
                    SortElement el = activeSortElements.get(k);
                    if (el.getState() != ElementState.SORTED) { // Don't override SORTED
                        el.setState(ElementState.NORMAL);
                        lastTransientStateIndices.remove(k);
                    }
                }
            }
        });
    }

    @Override
    public void reportResetStates(int... indices) {
        if (activeSortElements == null) return;
        Platform.runLater(() -> {
            for (int index : indices) {
                if (index >= 0 && index < activeSortElements.size()) {
                     SortElement el = activeSortElements.get(index);
                    if (el.getState() != ElementState.SORTED) { // Don't override SORTED
                        el.setState(ElementState.NORMAL);
                        lastTransientStateIndices.remove(index);
                    }
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
        return !this.isSortingActive.get();
    }

    @Override
    public void reportSortCompleted() {
        Platform.runLater(() -> {
            lastTransientStateIndices.clear(); // Clear any lingering transient states
            if (activeSortElements != null) {
                for (SortElement el : activeSortElements) {
                    el.setState(ElementState.SORTED);
                }
                sortPanel.updateElements(new ArrayList<>(activeSortElements));
            }
            if (controlPanel != null) controlPanel.enableControls();
            isSortingActive.set(false);
            System.out.println("Sort completed visually (reported by algorithm).");
        });
    }

    @Override
    public void requestVisualUpdate() {
        if (activeSortElements != null && sortPanel != null) {
            Platform.runLater(() -> {
                sortPanel.updateElements(new ArrayList<>(activeSortElements));
            });
        }
    }

    public void setControlPanel(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
    }

    public void setAlgorithm(Algorithm algorithm) {
        if (isSortingActive.get()) {
            System.out.println("Cannot change algorithm while sorting is in progress.");
            return;
        }
        this.currentAlgorithm = algorithm;
        System.out.println("Algorithm set to: " + (algorithm != null ? algorithm.getName() : "None"));
    }

    private void updateUiWithCurrentData(String reason) {
        System.out.println("SortController: " + reason);
        if (this.arrayManager == null) {
            System.err.println("SortController: ArrayManager is null. Cannot update UI.");
            this.activeSortElements = new ArrayList<>();
        } else {
            this.activeSortElements = this.arrayManager.getSortElements();
        }
        
        final int maxValue = (this.arrayManager != null) ? this.arrayManager.getMaxValueInCurrentArray() : 1;
        this.lastTransientStateIndices.clear(); // Clear transient states on new data

        if (sortPanel != null) {
            Platform.runLater(() -> {
                sortPanel.setMaxValueForScaling(maxValue);
                sortPanel.updateElements(new ArrayList<>(this.activeSortElements));
            });
        } else {
            System.err.println("SortController: SortPanel is null. Cannot update UI.");
        }
    }

    public void generateNewArray(int size, int minVal, int maxVal) {
        if (isSortingActive.get()) {
            stopSort();
        }
        
        if (this.arrayManager == null) {
            this.arrayManager = new ArrayManager(size, minVal, maxVal);
        } else {
            this.arrayManager.generateNewElements(size, minVal, maxVal);
        }
        
        updateUiWithCurrentData("New Array Generated in Controller");
        
        if (currentAlgorithm != null) {
            currentAlgorithm.reset();
        }
        if (controlPanel != null) {
            Platform.runLater(controlPanel::enableControls);
        }
        System.out.println("SortController: New array generated. Size: " + size);
    }

    public void startSort() {
        if (currentAlgorithm == null) {
            App.showAlert("Error", "Please select a sorting algorithm first.");
            return;
        }
        if (activeSortElements == null || activeSortElements.isEmpty()) {
            App.showAlert("Error", "Array is empty. Please generate an array first.");
            return;
        }
        if (isSortingActive.compareAndSet(false, true)) {
            isPaused.set(false);
            lastTransientStateIndices.clear(); // Clear before starting a new sort
            if (controlPanel != null) Platform.runLater(controlPanel::disableControlsDuringSort);

            currentAlgorithm.initialize(this.activeSortElements, this);

            sortThread = new Thread(() -> {
                try {
                    boolean moreSteps = true;
                    requestVisualUpdate(); 
                    Thread.sleep(getAnimationDelayMs());

                    while (moreSteps && isSortingActive.get() && !Thread.currentThread().isInterrupted()) {
                        if (isPaused.get()) {
                            Thread.sleep(100);
                            continue;
                        }
                        moreSteps = currentAlgorithm.nextStep();
                        if (moreSteps && isSortingActive.get()) {
                            Thread.sleep(getAnimationDelayMs());
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Sort thread interrupted.");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error during sorting steps: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> App.showAlert("Sorting Error", "An error occurred during sorting: " + e.getMessage()));
                } finally {
                    boolean wasAlgorithmStillMarkedAsSorting = isSortingActive.getAndSet(false);
                    isPaused.set(false);
                    Platform.runLater(() -> {
                        if (controlPanel != null) controlPanel.enableControls();
                        if (currentAlgorithm != null && !currentAlgorithm.isSorted() && wasAlgorithmStillMarkedAsSorting) {
                            System.out.println("SortController: Sort did not complete naturally or was stopped. Resetting visual states.");
                            lastTransientStateIndices.clear();
                            if (activeSortElements != null) {
                                for(SortElement el : activeSortElements) {
                                    if (el.getState() != ElementState.SORTED) {
                                        el.setState(ElementState.NORMAL);
                                    }
                                }
                                sortPanel.updateElements(new ArrayList<>(activeSortElements));
                            }
                        } else if (currentAlgorithm != null && currentAlgorithm.isSorted()) {
                             System.out.println("SortController: Ensuring final sorted state is displayed post-completion.");
                             lastTransientStateIndices.clear();
                             for (SortElement el : activeSortElements) el.setState(ElementState.SORTED);
                             sortPanel.updateElements(new ArrayList<>(activeSortElements));
                        }
                        System.out.println("SortController: Sorting process thread finished.");
                    });
                }
            });
            sortThread.setDaemon(true);
            sortThread.start();
        } else {
            System.out.println("SortController: Sorting is already in progress or another action is pending.");
        }
    }

    public void stopSort() {
        System.out.println("SortController: Attempting to stop sort...");
        if (isSortingActive.getAndSet(false)) {
            if (sortThread != null && sortThread.isAlive()) {
                sortThread.interrupt();
            }
            System.out.println("SortController: Stop signal sent to sorting thread.");
        } else {
            System.out.println("SortController: Sort was not active or already stopping.");
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
        this.animationDelayMs = Math.max(0, delay);
        System.out.println("Animation delay set to: " + this.animationDelayMs + "ms");
    }

    public int getCurrentAnimationDelay() {
        return animationDelayMs;
    }

    public boolean isCurrentlySorting() {
        return isSortingActive.get();
    }
}
