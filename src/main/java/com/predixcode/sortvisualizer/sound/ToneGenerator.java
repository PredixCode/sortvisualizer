package com.predixcode.sortvisualizer.sound;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Generates and plays up to two simple tones simultaneously with a fade-in/out envelope.
 * New tone requests on a channel will interrupt/replace currently playing tones on that same channel.
 */
public class ToneGenerator {

    public static final float SAMPLE_RATE = 44100f;

    private double currentMinFrequencyHz = 150.0;  // Default for linear mode
    private double currentMaxFrequencyHz = 2000.0; // Default for linear mode
    private static final int DEFAULT_VALUE_RANGE_MAX = 100;

    private AudioFormat audioFormat;

    private SourceDataLine sourceDataLine1;
    private ExecutorService soundPlayerExecutor1;
    private Future<?> currentSoundFuture1 = null;
    private final Object line1Lock = new Object();

    private SourceDataLine sourceDataLine2;
    private ExecutorService soundPlayerExecutor2;
    private Future<?> currentSoundFuture2 = null;
    private final Object line2Lock = new Object();

    // Musical Scale related fields
    private MusicalNote baseNote = MusicalNote.A; // Default base note A
    private int baseOctave = 4; // Default octave for A4 (440 Hz)
    private ScaleType scaleType = ScaleType.CHROMATIC; // Default to Major scale

    // Duration of the fade-in/out in milliseconds
    private static final int FADE_DURATION_MS = 5; // Short fade to reduce clicks

    public ToneGenerator() {
        try {
            audioFormat = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            
            sourceDataLine1 = createAndOpenLine(audioFormat);
            soundPlayerExecutor1 = createSingleThreadExecutor("ToneGeneratorChannel1Thread");

            sourceDataLine2 = createAndOpenLine(audioFormat);
            soundPlayerExecutor2 = createSingleThreadExecutor("ToneGeneratorChannel2Thread");

        } catch (LineUnavailableException e) {
            System.err.println("Error initializing audio lines: " + e.getMessage());
            closeLine(sourceDataLine1); sourceDataLine1 = null;
            closeLine(sourceDataLine2); sourceDataLine2 = null;
            shutdownExecutor(soundPlayerExecutor1); soundPlayerExecutor1 = null;
            shutdownExecutor(soundPlayerExecutor2); soundPlayerExecutor2 = null;
        }
    }

    private SourceDataLine createAndOpenLine(AudioFormat format) throws LineUnavailableException {
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        line.open(format);
        line.start();
        return line;
    }

    private ExecutorService createSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName(threadName);
            return t;
        });
    }

    public void setMinFrequency(double minFreq) {
        if (minFreq > 0 && minFreq < this.currentMaxFrequencyHz) {
            this.currentMinFrequencyHz = minFreq;
        } else {
            System.err.println("ToneGenerator: Invalid min frequency " + minFreq + ". Keeping current: " + this.currentMinFrequencyHz);
        }
    }

    public void setMaxFrequency(double maxFreq) {
        if (maxFreq > this.currentMinFrequencyHz) {
            this.currentMaxFrequencyHz = maxFreq;
        } else {
             System.err.println("ToneGenerator: Invalid max frequency " + maxFreq + ". Keeping current: " + this.currentMaxFrequencyHz);
        }
    }
    
    public double getCurrentMinFrequencyHz() {
        return currentMinFrequencyHz;
    }

    public double getCurrentMaxFrequencyHz() {
        return currentMaxFrequencyHz;
    }

    public void playToneOnChannel(int channel, int elementValue, int maxValueInArray, int durationMs) {
        SourceDataLine line;
        ExecutorService executor;
        Future<?> currentFuture;
        Object lock;

        switch (channel) {
            case 1:
                line = sourceDataLine1;
                executor = soundPlayerExecutor1;
                lock = line1Lock;
                break;
            case 2:
                line = sourceDataLine2;
                executor = soundPlayerExecutor2;
                lock = line2Lock;
                break;
            default:
                System.err.println("Invalid sound channel: " + channel);
                return;
        }

        if (line == null || executor == null || executor.isShutdown()) {
            return;
        }

        final int effectiveMaxValue = Math.max(1, maxValueInArray);

        synchronized (lock) {
            currentFuture = (channel == 1) ? currentSoundFuture1 : currentSoundFuture2;
            
            if (currentFuture != null && !currentFuture.isDone()) {
                currentFuture.cancel(true);
            }

            Future<?> newFuture = executor.submit(() -> {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    line.stop(); // Stop and flush before generating new sound
                    line.flush();

                    double frequency = mapValueToFrequency(elementValue, effectiveMaxValue);
                    int totalSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    byte[] buffer = new byte[totalSamples];

                    int fadeSamples = (int) ((FADE_DURATION_MS / 1000.0) * SAMPLE_RATE);
                    if (fadeSamples > totalSamples / 2) { 
                        fadeSamples = totalSamples / 2;
                    }
                    fadeSamples = Math.max(0, fadeSamples); // Ensure fadeSamples is not negative


                    for (int i = 0; i < totalSamples; i++) {
                        if (Thread.currentThread().isInterrupted()) {
                            line.flush(); // Flush before exiting if interrupted
                            return;
                        }
                        double angle = (2.0 * Math.PI * i * frequency) / SAMPLE_RATE;
                        double rawSample = Math.sin(angle);
                        
                        double envelopeMultiplier = 1.0;
                        if (fadeSamples > 0) {
                            if (i < fadeSamples) { 
                                envelopeMultiplier = (double) i / fadeSamples;
                            } else if (i >= totalSamples - fadeSamples) { 
                                envelopeMultiplier = (double) (totalSamples - 1 - i) / fadeSamples;
                            }
                        }
                        envelopeMultiplier = Math.max(0.0, Math.min(1.0, envelopeMultiplier));


                        buffer[i] = (byte) (rawSample * envelopeMultiplier * 127.0);
                    }

                    if (!Thread.currentThread().isInterrupted()) {
                        line.start(); // Ensure line is started before writing
                        line.write(buffer, 0, buffer.length);
                        // line.drain(); // Optional: wait for buffer to empty before next sound (can cause choppiness)
                    }

                } catch (Exception e) {
                    if (e instanceof InterruptedException || e instanceof java.nio.channels.ClosedByInterruptException) {
                        Thread.currentThread().interrupt(); // Re-interrupt if caught
                    }
                    // Attempt to flush if line is still open, even on error
                    if (line.isOpen()) {
                         try {
                            if (!line.isRunning()) line.start(); // Ensure started to flush
                            line.flush();
                         } catch (Exception flushEx) { /* ignore */ }
                    }
                }
            });
            
            if (channel == 1) {
                currentSoundFuture1 = newFuture;
            } else {
                currentSoundFuture2 = newFuture;
            }
        }
    }

    /**
     * Sets the musical scale parameters.
     * @param baseNote The root note of the scale.
     * @param baseOctave The octave of the root note.
     * @param scaleType The type of scale (e.g., Major, Minor, Chromatic).
     */
    public void setMusicalScale(MusicalNote baseNote, int baseOctave, ScaleType scaleType) {
        this.baseNote = baseNote != null ? baseNote : MusicalNote.A;
        this.baseOctave = (baseOctave >= 0 && baseOctave <= 8) ? baseOctave : 4;
        this.scaleType = scaleType != null ? scaleType : ScaleType.MAJOR;
        System.out.println("ToneGenerator: Scale set to " + this.baseNote + this.baseOctave + " " + this.scaleType.getDisplayName());

        // If switching to linear, UI might re-enable min/max freq sliders.
        // If switching from linear, UI might disable them.
        // This class doesn't directly control UI, but it's a consideration for SortController.
    }
    
    public MusicalNote getBaseNote() {
        return baseNote;
    }

    public int getBaseOctave() {
        return baseOctave;
    }

    public ScaleType getScaleType() {
        return scaleType;
    }


    private double getFrequencyForScaleNote(int noteIndexInScale) {
        int baseNoteOffsetFromA = baseNote.getSemitoneOffsetFromA();
        int semitonesFromA4ForBase = (baseOctave - 4) * 12 + baseNoteOffsetFromA;

        int[] intervals = scaleType.getIntervals();
        // Ensure intervals array is not empty and index is valid
        if (intervals == null || intervals.length == 0) {
             // This case should ideally be handled by mapValueToFrequency's fallback to linear
            System.err.println("ScaleType has no intervals. Defaulting to A4 (440Hz).");
            return 440.0;
        }
        
        // Modulo arithmetic to wrap noteIndexInScale if it's out of bounds
        // (e.g. if mapping produces index equal to number of notes, wrap to 0 for next octave,
        // or simply clamp to the highest note in the current octave of the scale)
        // For simplicity, let's clamp to the defined notes in one octave of the scale.
        // A more advanced mapping could span multiple octaves based on elementValue.
        int effectiveNoteIndex = noteIndexInScale % intervals.length;
        if (effectiveNoteIndex < 0) {
            effectiveNoteIndex += intervals.length;
        }

        int intervalSemitones = intervals[effectiveNoteIndex];
        int totalSemitonesFromA4 = semitonesFromA4ForBase + intervalSemitones;

        return 440.0 * Math.pow(2, totalSemitonesFromA4 / 12.0);
    }

    private double mapValueToFrequency(int elementValue, int actualMaxValueInArray) {
        // Use linear frequency mapping if scaleType is LINEAR_FREQUENCY or if intervals are missing
        if (scaleType == ScaleType.LINEAR_FREQUENCY || scaleType == null || scaleType.getIntervals() == null || scaleType.getIntervals().length == 0) {
            int maxVal = (actualMaxValueInArray <= 0) ? DEFAULT_VALUE_RANGE_MAX : actualMaxValueInArray;
            double normalizedValue = (double) Math.max(0, elementValue) / maxVal;
            normalizedValue = Math.max(0.0, Math.min(1.0, normalizedValue)); // Clamp to [0,1]
            return currentMinFrequencyHz + (normalizedValue * (currentMaxFrequencyHz - currentMinFrequencyHz));
        }

        int notesInScale = scaleType.getNotesInScale();
        int maxVal = (actualMaxValueInArray <= 0) ? DEFAULT_VALUE_RANGE_MAX : actualMaxValueInArray;
        
        int clampedElementValue = Math.max(0, Math.min(elementValue, maxVal));
        
        int noteIndexInScale;
        if (notesInScale <= 1) { // Handles single note scales or error cases
            noteIndexInScale = 0;
        } else {
            if (maxVal == 0) { 
                noteIndexInScale = 0;
            } else {
                // Map value to a note index within the scale's current octave
                // (elementValue / maxVal) gives a ratio from 0 to 1
                // Multiply by (notesInScale - 1) to get an index from 0 to notesInScale - 1
                noteIndexInScale = (int) Math.round(((double)clampedElementValue / maxVal) * (notesInScale - 1.0));
            }
        }
        // Ensure index is within bounds [0, notesInScale - 1]
        noteIndexInScale = Math.max(0, Math.min(notesInScale - 1, noteIndexInScale)); 

        return getFrequencyForScaleNote(noteIndexInScale);
    }

    private void closeLine(SourceDataLine line) {
        if (line != null) {
            synchronized(line) { // Synchronize on the line object itself if it's shared or accessed concurrently
                if (line.isOpen()) {
                    try {
                        line.drain(); // Wait for buffer to empty
                        line.stop();
                        line.close();
                    } catch (Exception e) { 
                        System.err.println("Error closing line: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdownNow(); // Attempt to stop all actively executing tasks
        }
    }

    public void close() {
        System.out.println("Closing ToneGenerator...");
        synchronized (line1Lock) {
            if (currentSoundFuture1 != null) {
                currentSoundFuture1.cancel(true);
            }
            shutdownExecutor(soundPlayerExecutor1);
            closeLine(sourceDataLine1);
            sourceDataLine1 = null; // Help GC
        }
        synchronized (line2Lock) {
            if (currentSoundFuture2 != null) {
                currentSoundFuture2.cancel(true);
            }
            shutdownExecutor(soundPlayerExecutor2);
            closeLine(sourceDataLine2);
            sourceDataLine2 = null; // Help GC
        }
         System.out.println("ToneGenerator closed.");
    }
}