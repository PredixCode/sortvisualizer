package com.predixcode.sortvisualizer.ui;

/**
 * Represents a single element in the array being sorted.
 * It holds its value and current visual state.
 */
public class SortElement {

    /**
     * Enum representing the current state of the array element,
     * used for determining its visual representation (e.g., color).
     */
    public enum ElementState {
        NORMAL,
        COMPARE,
        SWAP,
        PIVOT,
        SORTED
    }

    private int value;
    private ElementState state;

    /**
     * Constructs a SortElement with a given value and initial state.
     * @param value The integer value of this element.
     * @param initialState The initial state of this element.
     */
    public SortElement(int value, ElementState initialState) {
        this.value = value;
        this.state = initialState;
    }

    /**
     * Constructs a SortElement with a given value and sets its state to NORMAL.
     * @param value The integer value of this element.
     */
    public SortElement(int value) {
        this(value, ElementState.NORMAL);
    }

    /**
     * Gets the value of this element.
     * @return The integer value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the value of this element.
     * @param value The new integer value.
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Gets the current state of this element.
     * @return The ElementState.
     */
    public ElementState getState() {
        return state;
    }

    /**
     * Sets the current state of this element.
     * @param state The new ElementState.
     */
    public void setState(ElementState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "SortElement{" +
               "value=" + value +
               ", state=" + state +
               '}';
    }
}
