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

    private double currentMinFrequencyHz = 200.0;  // A3
    private double currentMaxFrequencyHz = 2000.0; // C6
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

    private double mapValueToFrequency(int elementValue, int actualMaxValueInArray) {
        int maxVal = (actualMaxValueInArray <= 0) ? DEFAULT_VALUE_RANGE_MAX : actualMaxValueInArray;
        double normalizedValue = (double) Math.max(0, elementValue) / maxVal;
        normalizedValue = Math.max(0.0, Math.min(1.0, normalizedValue));
        return currentMinFrequencyHz + (normalizedValue * (currentMaxFrequencyHz - currentMinFrequencyHz));
    }

    public void playToneOnChannel(int channel, int elementValue, int maxValueInArray, int durationMs) {
        SourceDataLine line;
        ExecutorService executor;
        Future<?> currentFuture;
        Object lock;

        switch (channel) {
            case 1 -> {
                line = sourceDataLine1;
                executor = soundPlayerExecutor1;
                lock = line1Lock;
            }
            case 2 -> {
                line = sourceDataLine2;
                executor = soundPlayerExecutor2;
                lock = line2Lock;
            }
            default -> {
                System.err.println("Invalid sound channel: " + channel);
                return;
            }
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
                    line.stop();
                    line.flush();

                    double frequency = mapValueToFrequency(elementValue, effectiveMaxValue);
                    int totalSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    byte[] buffer = new byte[totalSamples];

                    int fadeSamples = (int) ((FADE_DURATION_MS / 1000.0) * SAMPLE_RATE);
                    if (fadeSamples > totalSamples / 2) { // Ensure fade isn't longer than half the sound
                        fadeSamples = totalSamples / 2;
                    }


                    for (int i = 0; i < totalSamples; i++) {
                        if (Thread.currentThread().isInterrupted()) {
                            line.flush();
                            return;
                        }
                        double angle = (2.0 * Math.PI * i * frequency) / SAMPLE_RATE;
                        double rawSample = Math.sin(angle);
                        
                        // Apply envelope
                        double envelopeMultiplier = 1.0;
                        if (i < fadeSamples) { // Fade-in
                            envelopeMultiplier = (double) i / fadeSamples;
                        } else if (i >= totalSamples - fadeSamples) { // Fade-out
                            envelopeMultiplier = (double) (totalSamples - 1 - i) / fadeSamples;
                        }
                        // Ensure multiplier is not negative if fadeSamples is 0
                        envelopeMultiplier = Math.max(0.0, envelopeMultiplier);


                        buffer[i] = (byte) (rawSample * envelopeMultiplier * 127.0);
                    }

                    if (!Thread.currentThread().isInterrupted()) {
                        line.start();
                        line.write(buffer, 0, buffer.length);
                    }

                } catch (Exception e) {
                    if (e instanceof InterruptedException || e instanceof java.nio.channels.ClosedByInterruptException) {
                        Thread.currentThread().interrupt();
                    }
                    if (line.isOpen()) {
                         try {
                            if (!line.isRunning()) line.start();
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

    private void closeLine(SourceDataLine line) {
        if (line != null && line.isOpen()) {
            try {
                try (line) {
                    line.drain();
                    line.stop();
                }
            } catch (Exception e) { /* ignore */ }
        }
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void close() {
        System.out.println("Closing ToneGenerator (Dual Channel)...");
        synchronized (line1Lock) {
            shutdownExecutor(soundPlayerExecutor1);
            closeLine(sourceDataLine1);
        }
        synchronized (line2Lock) {
            shutdownExecutor(soundPlayerExecutor2);
            closeLine(sourceDataLine2);
        }
         System.out.println("Audio channels closed.");
    }
}
