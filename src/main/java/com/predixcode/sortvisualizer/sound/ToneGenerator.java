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
 * Generates and plays up to two simple tones simultaneously.
 * New tone requests on a channel will interrupt/replace currently playing tones on that same channel.
 */
public class ToneGenerator {

    public static final float SAMPLE_RATE = 44100f;

    private static final double MIN_FREQUENCY_HZ = 220.0;  // A3
    private static final double MAX_FREQUENCY_HZ = 1046.0; // C6 (Adjust as needed)

    private AudioFormat audioFormat;

    // Channel 1 resources
    private SourceDataLine sourceDataLine1;
    private ExecutorService soundPlayerExecutor1;
    private Future<?> currentSoundFuture1 = null;
    private final Object line1Lock = new Object();

    // Channel 2 resources
    private SourceDataLine sourceDataLine2;
    private ExecutorService soundPlayerExecutor2;
    private Future<?> currentSoundFuture2 = null;
    private final Object line2Lock = new Object();

    public ToneGenerator() {
        try {
            audioFormat = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            
            // Initialize Channel 1
            sourceDataLine1 = createAndOpenLine(audioFormat);
            soundPlayerExecutor1 = createSingleThreadExecutor("ToneGeneratorChannel1Thread");

            // Initialize Channel 2
            sourceDataLine2 = createAndOpenLine(audioFormat);
            soundPlayerExecutor2 = createSingleThreadExecutor("ToneGeneratorChannel2Thread");

        } catch (LineUnavailableException e) {
            System.err.println("Error initializing audio lines: " + e.getMessage());
            e.printStackTrace();
            // Disable sound if initialization fails for any line
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

    private double mapValueToFrequency(int elementValue, int actualMaxValueInArray) {
        double normalizedValue = (double) elementValue / actualMaxValueInArray;
        normalizedValue = Math.max(0.0, Math.min(1.0, normalizedValue));
        return MIN_FREQUENCY_HZ + (normalizedValue * (MAX_FREQUENCY_HZ - MIN_FREQUENCY_HZ));
    }

    /**
     * Plays a tone on a specific channel.
     *
     * @param channel The channel to play on (1 or 2).
     * @param elementValue The value of the element.
     * @param maxValueInArray The maximum value in the current array context for scaling frequency.
     * @param durationMs The duration of the tone in milliseconds.
     */
    public void playToneOnChannel(int channel, int elementValue, int maxValueInArray, int durationMs) {
        SourceDataLine line;
        ExecutorService executor;
        Future<?> currentFuture;
        Object lock;

        if (channel == 1) {
            line = sourceDataLine1;
            executor = soundPlayerExecutor1;
            lock = line1Lock;
        } else if (channel == 2) {
            line = sourceDataLine2;
            executor = soundPlayerExecutor2;
            lock = line2Lock;
        } else {
            System.err.println("Invalid sound channel: " + channel);
            return;
        }

        if (line == null || executor == null || executor.isShutdown()) {
            return; // Sound system for this channel not available or shut down.
        }

        final int effectiveMaxValue = Math.max(1, maxValueInArray);

        synchronized (lock) {
            // Get current future for this channel
            currentFuture = (channel == 1) ? currentSoundFuture1 : currentSoundFuture2;
            
            if (currentFuture != null && !currentFuture.isDone()) {
                currentFuture.cancel(true); // Interrupt the previous sound task on this channel
            }

            Future<?> newFuture = executor.submit(() -> {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    // These operations should be safe as they are on a single-threaded executor for this line
                    line.stop();
                    line.flush();

                    double frequency = mapValueToFrequency(elementValue, effectiveMaxValue);
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    byte[] buffer = new byte[numSamples];

                    for (int i = 0; i < numSamples; i++) {
                        if (Thread.currentThread().isInterrupted()) {
                            line.flush();
                            return;
                        }
                        double angle = (2.0 * Math.PI * i * frequency) / SAMPLE_RATE;
                        buffer[i] = (byte) (Math.sin(angle) * 127.0);
                    }

                    if (!Thread.currentThread().isInterrupted()) {
                        line.start();
                        line.write(buffer, 0, buffer.length);
                    }

                } catch (Exception e) {
                    if (e instanceof InterruptedException || e instanceof java.nio.channels.ClosedByInterruptException) {
                        Thread.currentThread().interrupt();
                    }
                    if (line != null && line.isOpen()) {
                         try {
                            if (!line.isRunning()) line.start();
                            line.flush();
                         } catch (Exception flushEx) { /* ignore */ }
                    }
                }
            });
            
            // Store the new future for this channel
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
                line.drain();
                line.stop();
                line.close();
            } catch (Exception e) {
                System.err.println("Exception while closing a SourceDataLine: " + e.getMessage());
            }
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
            System.out.println("Audio channel 1 closed.");
        }
        synchronized (line2Lock) {
            shutdownExecutor(soundPlayerExecutor2);
            closeLine(sourceDataLine2);
            System.out.println("Audio channel 2 closed.");
        }
    }
}
