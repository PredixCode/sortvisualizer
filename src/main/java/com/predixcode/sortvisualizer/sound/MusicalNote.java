package com.predixcode.sortvisualizer.sound;

public enum MusicalNote {
    C("C", -9), 
    C_SHARP("C#", -8), 
    D("D", -7), 
    D_SHARP("D#", -6),
    E("E", -5), 
    F("F", -4), 
    F_SHARP("F#", -3), 
    G("G", -2),
    G_SHARP("G#", -1), 
    A("A", 0),        // A4 is often the reference, 440 Hz
    A_SHARP("A#", 1), 
    B("B", 2);

    private final String displayName;
    private final int semitoneOffsetFromA; // Semitones relative to A of the same octave

    MusicalNote(String displayName, int semitoneOffsetFromA) {
        this.displayName = displayName;
        this.semitoneOffsetFromA = semitoneOffsetFromA;
    }

    public int getSemitoneOffsetFromA() {
        return semitoneOffsetFromA;
    }

    @Override
    public String toString() {
        return displayName;
    }
}