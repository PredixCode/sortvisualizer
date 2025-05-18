package com.predixcode.sortvisualizer.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.predixcode.sortvisualizer.algorithms.Algorithm;
import com.predixcode.sortvisualizer.sound.MusicalNote; // Added import
import com.predixcode.sortvisualizer.sound.ScaleType;   // Added import
import com.predixcode.sortvisualizer.sound.ToneGenerator;
import com.predixcode.sortvisualizer.ui.App;
import com.predixcode.sortvisualizer.ui.ControlPanel;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;
import com.predixcode.sortvisualizer.ui.SortPanel;

import javafx.application.Platform;

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

    public static final int DEFAULT_ARRAY_SIZE = 50;
    public static final int DEFAULT_MIN_VALUE = 1;
    public static final int DEFAULT_MAX_VALUE = 200;

    private Set<Integer> lastTransientStateIndices = new HashSet<>();

    // Sound related fields
    private ToneGenerator toneGenerator;
    private boolean soundEnabled = true;
    private MusicalNote currentBaseNote = MusicalNote.A;    // Default base note
    private int currentBaseOctave = 4;                      // Default octave (A4)
    private ScaleType currentScaleType = ScaleType.MAJOR;   // Default scale type

    private static final int TONE_DURATION_MS_COMPARE = 60;
    private static final int TONE_DURATION_MS_SWAP = 80;
    private static final int TONE_DURATION_MS_PIVOT = 90;
    private static final int TONE_DURATION_MS_COMPLETION_NOTE = 120;

    private final ExecutorService utilitySoundExecutor;

    public SortController(SortPanel sortPanel) {
        this.sortPanel = sortPanel;
        this.arrayManager = new ArrayManager(DEFAULT_ARRAY_SIZE, DEFAULT_MIN_VALUE, DEFAULT_MAX_VALUE);
        this.activeSortElements = this.arrayManager.getSortElements();
        
        // Initialize ToneGenerator
        this.toneGenerator = new ToneGenerator();
        // Set initial scale on ToneGenerator based on defaults
        this.toneGenerator.setMusicalScale(this.currentBaseNote, this.currentBaseOctave, this.currentScaleType);
        // Set initial linear frequencies (will be used if ScaleType.LINEAR_FREQUENCY is selected)
        this.toneGenerator.setMinFrequency(200.0); // Default min linear freq
        this.toneGenerator.setMaxFrequency(1200.0); // Default max linear freq
        
        this.utilitySoundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("UtilitySoundThread");
            return t;
        });
        
        updateUiWithCurrentData("Initial Array Generation on Controller instantiation");
    }

    // --- Sound Configuration Methods ---

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        System.out.println("Sound enabled: " + enabled);
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setMinToneFrequency(double freq) {
        if (toneGenerator != null) {
            toneGenerator.setMinFrequency(freq);
        }
    }

    public void setMaxToneFrequency(double freq) {
        if (toneGenerator != null) {
            toneGenerator.setMaxFrequency(freq);
        }
    }
    
    public double getCurrentMinToneFrequency() {
        // Return the actual current min frequency from ToneGenerator if linear mode is active,
        // or a sensible default if not. ControlPanel will initialize its sliders from these.
        return (toneGenerator != null) ? toneGenerator.getCurrentMinFrequencyHz() : 150.0;
    }

    public double getCurrentMaxToneFrequency() {
         return (toneGenerator != null) ? toneGenerator.getCurrentMaxFrequencyHz() : 2000.0;
    }

    public void setMusicalScaleType(ScaleType type) {
        if (type != null) {
            this.currentScaleType = type;
            if (toneGenerator != null) {
                toneGenerator.setMusicalScale(this.currentBaseNote, this.currentBaseOctave, this.currentScaleType);
            }
            System.out.println("SortController: ScaleType set to " + type.getDisplayName());
        }
    }

    public ScaleType getCurrentScaleType() {
        return this.currentScaleType;
    }

    public void setMusicalBaseNote(MusicalNote note) {
        if (note != null) {
            this.currentBaseNote = note;
            if (toneGenerator != null) {
                toneGenerator.setMusicalScale(this.currentBaseNote, this.currentBaseOctave, this.currentScaleType);
            }
            System.out.println("SortController: BaseNote set to " + note.toString());
        }
    }

    public MusicalNote getCurrentBaseNote() {
        return this.currentBaseNote;
    }

    public void setMusicalBaseOctave(int octave) {
        // Assuming octave range is validated by UI or here if necessary (e.g., 0-8)
        this.currentBaseOctave = octave;
        if (toneGenerator != null) {
            toneGenerator.setMusicalScale(this.currentBaseNote, this.currentBaseOctave, this.currentScaleType);
        }
        System.out.println("SortController: BaseOctave set to " + octave);
    }

    public int getCurrentBaseOctave() {
        return this.currentBaseOctave;
    }

    // --- End of Sound Configuration Methods ---

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
        
        if (soundEnabled && toneGenerator != null) {
            int value1 = activeSortElements.get(index1).getValue();
            int value2 = activeSortElements.get(index2).getValue();
            int maxVal = arrayManager.getMaxValueInCurrentArray();
            
            toneGenerator.playToneOnChannel(1, value1, maxVal, TONE_DURATION_MS_COMPARE);
            toneGenerator.playToneOnChannel(2, value2, maxVal, TONE_DURATION_MS_COMPARE);
        }

        Platform.runLater(() -> {
            resetLastTransientStates();
            activeSortElements.get(index1).setState(ElementState.COMPARE);
            activeSortElements.get(index2).setState(ElementState.COMPARE);
            lastTransientStateIndices.add(index1);
            lastTransientStateIndices.add(index2);
        });
    }

    @Override
    public void reportSwap(int index1, int index2) {
        if (activeSortElements == null || index1 < 0 || index2 < 0 || index1 >= activeSortElements.size() || index2 >= activeSortElements.size()) return;
        
        if (soundEnabled && toneGenerator != null) {
            // Play tone based on one of the elements involved in the swap
            // For simplicity, using the value at index1 *before* the conceptual swap for the tone.
            // If values are already swapped in activeSortElements by the algorithm before this call, adjust accordingly.
            int valueNowAtIndex1 = activeSortElements.get(index1).getValue(); 
            int maxVal = arrayManager.getMaxValueInCurrentArray();
            toneGenerator.playToneOnChannel(1, valueNowAtIndex1, maxVal, TONE_DURATION_MS_SWAP);
        }

        Platform.runLater(() -> {
            resetLastTransientStates();
            activeSortElements.get(index1).setState(ElementState.SWAP);
            activeSortElements.get(index2).setState(ElementState.SWAP);
            lastTransientStateIndices.add(index1);
            lastTransientStateIndices.add(index2);
        });
    }

    @Override
    public void reportElementStateChange(int index, ElementState newState) {
        if (activeSortElements == null || index < 0 || index >= activeSortElements.size()) return;

        if (soundEnabled && toneGenerator != null) {
            if (newState == ElementState.PIVOT) {
                toneGenerator.playToneOnChannel(1, activeSortElements.get(index).getValue(), 
                                                arrayManager.getMaxValueInCurrentArray(), TONE_DURATION_MS_PIVOT);
            }
            // Could add sounds for other state changes if desired
        }

        Platform.runLater(() -> {
            if (newState == ElementState.SORTED || newState == ElementState.PIVOT || newState == ElementState.NORMAL) {
                // If it's becoming a stable state, remove from transient
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
                    if (el.getState() != ElementState.SORTED) { // Don't reset already sorted elements
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
                    if (el.getState() != ElementState.SORTED) { // Don't reset already sorted elements
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
    
    private void playCompletionSound() {
        if (soundEnabled && toneGenerator != null && utilitySoundExecutor != null && !utilitySoundExecutor.isShutdown()) {
            utilitySoundExecutor.submit(() -> {
                try {
                    // Use current scale settings for completion sound for consistency
                    // Or define specific notes for a completion jingle
                    int maxVal = arrayManager.getMaxValueInCurrentArray(); // For linear mapping if active
                    
                    // Example: Play root, third, fifth of the current scale, or specific frequencies
                    if (currentScaleType != ScaleType.LINEAR_FREQUENCY && currentScaleType.getIntervals().length >= 3) {
                        // Play first note of scale
                        toneGenerator.playToneOnChannel(1, 0, // mapValueToFrequency will use 0 for first note
                                                        currentScaleType.getNotesInScale() -1, TONE_DURATION_MS_COMPLETION_NOTE);
                        Thread.sleep(TONE_DURATION_MS_COMPLETION_NOTE + 50);
                        if (Thread.currentThread().isInterrupted()) return;
                        // Play third note of scale (approx)
                        toneGenerator.playToneOnChannel(1, (int)Math.round((currentScaleType.getNotesInScale()-1) * 0.4),
                                                        currentScaleType.getNotesInScale() -1, TONE_DURATION_MS_COMPLETION_NOTE);
                        Thread.sleep(TONE_DURATION_MS_COMPLETION_NOTE + 50); 
                        if (Thread.currentThread().isInterrupted()) return;
                        // Play fifth note of scale (approx)
                         toneGenerator.playToneOnChannel(1, (int)Math.round((currentScaleType.getNotesInScale()-1) * 0.7),
                                                        currentScaleType.getNotesInScale() -1, TONE_DURATION_MS_COMPLETION_NOTE + 50);
                    } else { // Fallback to linear if not enough notes or linear is selected
                        toneGenerator.playToneOnChannel(1, (int)(DEFAULT_MIN_VALUE + (DEFAULT_MAX_VALUE - DEFAULT_MIN_VALUE) * 0.3), maxVal, TONE_DURATION_MS_COMPLETION_NOTE);
                        Thread.sleep(TONE_DURATION_MS_COMPLETION_NOTE + 50);
                        if (Thread.currentThread().isInterrupted()) return;
                        toneGenerator.playToneOnChannel(1, (int)(DEFAULT_MIN_VALUE + (DEFAULT_MAX_VALUE - DEFAULT_MIN_VALUE) * 0.6), maxVal, TONE_DURATION_MS_COMPLETION_NOTE);
                        Thread.sleep(TONE_DURATION_MS_COMPLETION_NOTE + 50); 
                        if (Thread.currentThread().isInterrupted()) return;
                        toneGenerator.playToneOnChannel(1, DEFAULT_MAX_VALUE, maxVal, TONE_DURATION_MS_COMPLETION_NOTE + 50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error during completion sound: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public void reportSortCompleted() {
        Platform.runLater(() -> {
            lastTransientStateIndices.clear();
            if (activeSortElements != null) {
                for (SortElement el : activeSortElements) {
                    el.setState(ElementState.SORTED);
                }
                sortPanel.updateElements(new ArrayList<>(activeSortElements)); // Ensure final update
            }
            if (controlPanel != null) controlPanel.enableControls();
            isSortingActive.set(false); // Ensure this is set before playing sound that might rely on state
            System.out.println("Sort completed visually (reported by algorithm).");
            playCompletionSound();
        });
    }
    
    @Override
    public void requestVisualUpdate() {
        if (activeSortElements != null && sortPanel != null) {
            // Create a defensive copy for the UI thread
            final List<SortElement> elementsCopy = new ArrayList<>(activeSortElements.size());
            for (SortElement el : activeSortElements) {
                elementsCopy.add(new SortElement(el.getValue(), el.getState()));
            }
            Platform.runLater(() -> {
                sortPanel.updateElements(elementsCopy);
            });
        }
    }

    public void setControlPanel(ControlPanel controlPanel) {
        this.controlPanel = controlPanel;
        // After control panel is set, it might initialize its values from controller.
        // Ensure controller's sound settings are passed to it if not already done.
        if (this.controlPanel != null) {
            // This is typically handled by ControlPanel.setSortController()
        }
    }

    public void setAlgorithm(Algorithm algorithm) {
        if (isSortingActive.get()) {
            System.out.println("Cannot change algorithm while sorting is in progress.");
            // Optionally, inform the user via UI if controlPanel is available
            // if (controlPanel != null) App.showAlert("Busy", "Cannot change algorithm during sorting.");
            return;
        }
        this.currentAlgorithm = algorithm;
        System.out.println("Algorithm set to: " + (algorithm != null ? algorithm.getName() : "None"));
        if (this.currentAlgorithm != null) {
            this.currentAlgorithm.reset(); // Reset algorithm state when selected
        }
    }

    private void updateUiWithCurrentData(String reason) {
        System.out.println("SortController: " + reason);
        if (this.arrayManager == null) {
            System.err.println("SortController: ArrayManager is null. Cannot update UI.");
            this.activeSortElements = new ArrayList<>(); // Ensure it's not null
        } else {
            // Get a fresh copy of elements, as algorithms might modify the list they receive
            this.activeSortElements = this.arrayManager.getSortElements();
        }
        
        final int maxValue = (this.arrayManager != null) ? this.arrayManager.getMaxValueInCurrentArray() : 1;
        this.lastTransientStateIndices.clear(); // Clear any lingering transient states

        if (sortPanel != null) {
            // Create a defensive copy for the UI thread
            final List<SortElement> elementsCopy = new ArrayList<>(this.activeSortElements.size());
            for (SortElement el : this.activeSortElements) {
                elementsCopy.add(new SortElement(el.getValue(), el.getState()));
            }
            Platform.runLater(() -> {
                sortPanel.setMaxValueForScaling(maxValue);
                sortPanel.updateElements(elementsCopy);
            });
        } else {
            System.err.println("SortController: SortPanel is null. Cannot update UI.");
        }
    }

    public void generateNewArray(int size, int minVal, int maxVal) {
        if (isSortingActive.get()) {
            // Consider stopping the sort or preventing generation
            // For now, let's stop the sort if active
            stopSort(); 
            // It might be better to wait for stopSort to fully complete if it's async,
            // or disable the generate button while sorting.
            // For simplicity, assuming stopSort is effective quickly.
        }
        
        if (this.arrayManager == null) {
            this.arrayManager = new ArrayManager(size, minVal, maxVal);
        } else {
            this.arrayManager.generateNewElements(size, minVal, maxVal);
        }
        
        updateUiWithCurrentData("New Array Generated in Controller");
        
        if (currentAlgorithm != null) {
            currentAlgorithm.reset(); // Reset algorithm state for the new array
        }
        if (controlPanel != null) {
            Platform.runLater(controlPanel::enableControls); // Ensure controls are re-enabled
        }
        System.out.println("SortController: New array generated. Size: " + size);
    }

    public void startSort() {
        if (currentAlgorithm == null) {
            App.showAlert("Error", "Please select a sorting algorithm first.");
            return;
        }
        if (activeSortElements == null || activeSortElements.isEmpty()) {
            // This case should ideally be handled by generating a default array if none exists
            // or disabling start if array is empty.
            App.showAlert("Error", "Array is empty. Please generate an array first.");
            return;
        }
        if (isSortingActive.compareAndSet(false, true)) {
            isPaused.set(false);
            lastTransientStateIndices.clear(); // Clear before sort starts
            if (controlPanel != null) Platform.runLater(controlPanel::disableControlsDuringSort);

            // Pass a copy of the current elements to the algorithm
            List<SortElement> elementsToSort = new ArrayList<>(activeSortElements.size());
            for(SortElement el : activeSortElements) {
                elementsToSort.add(new SortElement(el.getValue(), ElementState.NORMAL)); // Reset states for algorithm
            }
            this.activeSortElements = elementsToSort; // Controller now tracks this list being sorted
            
            currentAlgorithm.initialize(this.activeSortElements, this);

            sortThread = new Thread(() -> {
                try {
                    boolean moreSteps = true;
                    requestVisualUpdate(); // Initial state before first step
                    Thread.sleep(getAnimationDelayMs()); // Initial delay

                    while (moreSteps && isSortingActive.get() && !Thread.currentThread().isInterrupted()) {
                        if (isPaused.get()) {
                            Thread.sleep(100); // Polling delay while paused
                            continue;
                        }
                        moreSteps = currentAlgorithm.nextStep();
                        requestVisualUpdate(); // Update UI after each step
                        if (moreSteps && isSortingActive.get()) { // Only sleep if more steps and not stopped
                            Thread.sleep(getAnimationDelayMs());
                        }
                    }
                    // After loop, if not interrupted and sort was active, check completion
                    if (isSortingActive.get() && !Thread.currentThread().isInterrupted()) {
                        if (!moreSteps) { // Algorithm indicated completion
                           // reportSortCompleted(); // This is now called by algorithm via callback
                        }
                    }

                } catch (InterruptedException e) {
                    System.out.println("Sort thread interrupted.");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                } catch (Exception e) {
                    System.err.println("Error during sorting steps: " + e.getMessage());
                    e.printStackTrace(); // For debugging
                    Platform.runLater(() -> App.showAlert("Sorting Error", "An error occurred during sorting: " + e.getMessage()));
                } finally {
                    boolean wasAlgorithmStillMarkedAsSorting = isSortingActive.getAndSet(false); // Ensure isSortingActive is false
                    isPaused.set(false); // Reset pause state
                    
                    // Final UI update and control re-enabling
                    Platform.runLater(() -> {
                        if (controlPanel != null) controlPanel.enableControls();
                        
                        // Ensure the visual state reflects the outcome
                        if (currentAlgorithm != null && currentAlgorithm.isSorted()) {
                             System.out.println("SortController: Ensuring final sorted state is displayed post-completion/stop.");
                             lastTransientStateIndices.clear();
                             for (SortElement el : activeSortElements) el.setState(ElementState.SORTED);
                             sortPanel.updateElements(new ArrayList<>(activeSortElements));
                        } else if (wasAlgorithmStillMarkedAsSorting) { // Sort was stopped or didn't complete
                            System.out.println("SortController: Sort did not complete naturally or was stopped. Resetting non-sorted visual states.");
                            lastTransientStateIndices.clear();
                            if (activeSortElements != null) {
                                for(SortElement el : activeSortElements) {
                                    if (el.getState() != ElementState.SORTED) { // Only reset if not already marked sorted
                                        el.setState(ElementState.NORMAL);
                                    }
                                }
                                sortPanel.updateElements(new ArrayList<>(activeSortElements));
                            }
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
        if (isSortingActive.get()) { // Check if sorting is active before trying to set it false
            isSortingActive.set(false); // Signal the thread to stop
            if (sortThread != null && sortThread.isAlive()) {
                sortThread.interrupt(); // Interrupt the thread to break sleep/wait
            }
            System.out.println("SortController: Stop signal sent to sorting thread.");
        } else {
            System.out.println("SortController: Sort was not active or already stopping.");
            // If sort was not active, ensure controls are enabled.
            // This might happen if stop is clicked multiple times.
            if (controlPanel != null) {
                 Platform.runLater(controlPanel::enableControls);
            }
        }
        // The finally block in the sortThread will handle UI updates and control enabling.
    }
    
    public void shutdownSound() {
        if (toneGenerator != null) {
            toneGenerator.close();
        }
        if (utilitySoundExecutor != null) {
            utilitySoundExecutor.shutdownNow();
            try {
                // Wait a little for termination, though shutdownNow is aggressive
                // if (!utilitySoundExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                //     System.err.println("Utility sound executor did not terminate in time.");
                // }
            } catch (Exception e) { // Catch InterruptedException if using awaitTermination
                // Thread.currentThread().interrupt();
            }
            System.out.println("Utility sound executor shutdown.");
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

    public boolean isPaused() {
        return isPaused.get();
    }
}