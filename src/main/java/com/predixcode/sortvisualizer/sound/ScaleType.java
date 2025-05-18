package com.predixcode.sortvisualizer.sound;

public enum ScaleType {
    MAJOR("Major", new int[]{0, 2, 4, 5, 7, 9, 11}),             // W-W-H-W-W-W-H
    MINOR_NATURAL("Natural Minor", new int[]{0, 2, 3, 5, 7, 8, 10}), // W-H-W-W-H-W-W
    MINOR_HARMONIC("Harmonic Minor", new int[]{0, 2, 3, 5, 7, 8, 11}),
    PENTATONIC_MAJOR("Major Pentatonic", new int[]{0, 2, 4, 7, 9}),
    PENTATONIC_MINOR("Minor Pentatonic", new int[]{0, 3, 5, 7, 10}),
    BLUES("Blues", new int[]{0, 3, 5, 6, 7, 10}),
    CHROMATIC("Chromatic", new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}),
    WHOLE_TONE("Whole Tone", new int[]{0, 2, 4, 6, 8, 10}),
    // Fallback for linear frequency mapping, not a musical scale in the same sense
    LINEAR_FREQUENCY("Linear Frequency", new int[]{});


    private final String displayName;
    private final int[] intervals; // in semitones from the root of the scale

    ScaleType(String displayName, int[] intervals) {
        this.displayName = displayName;
        this.intervals = intervals;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int[] getIntervals() {
        return intervals;
    }

    public int getNotesInScale() {
        // For LINEAR_FREQUENCY, effectively treat as 0 notes for scale logic,
        // so mapValueToFrequency will use its linear fallback.
        return intervals.length;
    }

    @Override
    public String toString() {
        return displayName;
    }
}