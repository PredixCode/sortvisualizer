package com.predixcode.sortvisualizer.core;

import java.security.SecureRandom;
import java.util.ArrayList; // Required import
import java.util.List;
import java.util.stream.Collectors;

import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

/**
 * Manages the creation and storage of a list of SortElement objects
 * to be used by sorting algorithms and the UI.
 */
public class ArrayManager {

    private final SecureRandom random = new SecureRandom();
    private List<SortElement> sortElements; // Changed from core.Element to ui.SortElement
    private int maxValueInCurrentArray;

    /**
     * Constructs an ArrayManager and initializes it with a list of random SortElements.
     * @param size The number of elements to create.
     * @param minVal The minimum value (inclusive) for the random elements.
     * @param maxVal The maximum value (exclusive) for the random elements.
     */
    public ArrayManager(int size, int minVal, int maxVal) {
        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        if (minVal >= maxVal && size > 0) { // Allow minVal == maxVal if size is 0 (empty array)
            throw new IllegalArgumentException("Min value must be less than max value for non-empty array.");
        }
        generateNewElements(size, minVal, maxVal);
    }

    /**
     * Generates a new list of SortElements and updates the internal state.
     * @param size The number of elements.
     * @param minVal The minimum value.
     * @param maxVal The maximum value (exclusive).
     */
    public void generateNewElements(int size, int minVal, int maxVal) {
        this.sortElements = new ArrayList<>();
        this.maxValueInCurrentArray = 0; // Reset max value

        if (size == 0) { // Handle empty array case explicitly
            this.maxValueInCurrentArray = 1; // Default for scaling if array is empty
            return;
        }

        for (int i = 0; i < size; i++) {
            int randomValue = random.nextInt(minVal, maxVal);
            this.sortElements.add(new SortElement(randomValue, ElementState.NORMAL));
            if (randomValue > this.maxValueInCurrentArray) {
                this.maxValueInCurrentArray = randomValue;
            }
        }
        // If all values are 0 or negative, maxValueInCurrentArray might be 0 or less.
        // For scaling, ensure it's at least 1 if there are elements.
        if (size > 0 && this.maxValueInCurrentArray <= 0) {
            // If values can be negative and you want to scale based on magnitude,
            // this logic might need adjustment. For typical positive bar charts,
            // a max of 0 (if all are 0) should scale to a max of 1.
            boolean allNonPositive = true;
            for(SortElement el : this.sortElements) {
                if (el.getValue() > 0) {
                    allNonPositive = false;
                    break;
                }
            }
            if(allNonPositive) this.maxValueInCurrentArray = 1; // Default scaling factor
        }
    }

    /**
     * Gets the current list of SortElement objects.
     * Returns a new list containing copies to prevent external modification of the internal list.
     * @return A new list of SortElement objects.
     */
    public List<SortElement> getSortElements() {
        // Return a new list of new SortElement objects to ensure true immutability
        // of the returned collection with respect to the internal state if needed.
        // For now, a shallow copy of the list is often sufficient if SortElements themselves are handled carefully.
        return new ArrayList<>(this.sortElements);
    }

    /**
     * Gets the maximum value found in the current set of elements.
     * This is useful for the SortPanel to scale the bars correctly.
     * Returns 1 if the array is empty or all values are <= 0, to prevent division by zero
     * and provide a default scaling base.
     * @return The maximum value, or 1 as a default.
     */
    public int getMaxValueInCurrentArray() {
        if (this.sortElements.isEmpty()) return 1; // Default for empty array
        return this.maxValueInCurrentArray <= 0 ? 1 : this.maxValueInCurrentArray;
    }


    /**
     * Gets the current size of the array.
     * @return The number of elements.
     */
    public int getSize() {
        return sortElements.size();
    }

    /**
     * Validates the internal array of SortElements.
     * Checks if the list itself is null (should not happen with current constructor).
     * Empty lists are considered valid.
     * @throws IllegalStateException if the internal sortElements list is null.
     */
    public void validateArray() {
        if (this.sortElements == null) {
            // This state should ideally be prevented by the constructor and methods.
            throw new IllegalStateException("Internal SortElements list is null.");
        }
        // An empty list is valid. The algorithms should handle it.
    }


    @Override
    public String toString() {
        return sortElements.stream()
                           .map(SortElement::toString) // Assumes SortElement has a meaningful toString
                           .collect(Collectors.joining(", ", "[", "]"));
    }

    public static void main(String[] args) {
        ArrayManager am = new ArrayManager(10, 0, 100);
        System.out.println("ArrayManager: " + am.toString());
        System.out.println("Max value: " + am.getMaxValueInCurrentArray());
        System.out.println("Size: " + am.getSize());

        List<SortElement> elements = am.getSortElements();
        System.out.println("Retrieved SortElements count: " + elements.size());
        if (!elements.isEmpty()) {
            System.out.println("First element value: " + elements.get(0).getValue() + ", state: " + elements.get(0).getState());
        }

        System.out.println("\nTesting with all zeros:");
        ArrayManager amZeros = new ArrayManager(5, 0, 1); // Will generate all 0s
        System.out.println("ArrayManager (zeros): " + amZeros.toString());
        System.out.println("Max value (zeros): " + amZeros.getMaxValueInCurrentArray());


        System.out.println("\nTesting with empty array:");
        ArrayManager amEmpty = new ArrayManager(0, 0, 100);
        System.out.println("ArrayManager (empty): " + amEmpty.toString());
        System.out.println("Max value (empty): " + amEmpty.getMaxValueInCurrentArray());
        System.out.println("Size (empty): " + amEmpty.getSize());
    }
}
