# SortVisualizer: A Dynamic JavaFX Sorting Algorithm Visualization Tool

## Overview

Welcome to **SortVisualizer**, an interactive application designed to bring sorting algorithms to life! Built with Java and JavaFX, this tool provides a clear, step-by-step visual representation of how different sorting algorithms operate on a dataset.

Witness the elegance of algorithms like Bubble Sort, Insertion Sort, QuickSort, Merge Sort, and Tree Sort as they meticulously arrange data, with each comparison and swap animated and audibly represented for enhanced understanding.

## Features

* **Dynamic Visualization:** Watch stylized bars (with rounded corners and gradients) representing array elements rearrange in real-time according to the chosen algorithm's logic.
* **Step-by-Step Execution:**
    * Control the pace of the visualization with an adjustable animation speed slider.
    * **Pause and Resume** the sorting process to examine specific stages.
    * **Stop** the current sort at any time.
* **Algorithm Selection:** Easily switch between a variety of implemented sorting algorithms:
    * Bubble Sort
    * Insertion Sort
    * QuickSort (Lomuto partition scheme)
    * Merge Sort (Iterative approach for visualization)
    * Tree Sort (BST-based)
* **Audio Feedback:**
    * Hear tones corresponding to element values during operations (e.g., comparisons, swaps), with support for two simultaneous tones for dual-pointer operations.
    * Enable or disable sound.
    * Adjust minimum and maximum sound frequencies via sliders for a customized audio experience.
* **Customizable Data:** Generate new random arrays of varying sizes to observe algorithm behavior.
* **Intuitive User Interface:**
    * A clean and user-friendly interface built with JavaFX, organized into clear sections: "Algorithm & Speed Controls," "Array Configuration," and "Sound Configuration."
    * Styled with a cohesive dark theme for a modern look and feel.
* **Educational Focus:** Designed to demystify complex sorting processes and provide a deeper understanding of their underlying mechanics.
* **Color-Coded States:** Elements are visually distinguished by color to indicate their current state (e.g., being compared, swapped, pivot element, or sorted).

## Technologies Used

* **Java:** Core application logic.
* **JavaFX (OpenJFX):** Modern GUI framework for the user interface and visualizations.
* **Maven:** Project build and dependency management.

## Getting Started

### Prerequisites

* **JDK (Java Development Kit):** Version 17 or higher.
* **Maven:** Version 3.6 or higher (for building and running via Maven).
* *(JavaFX SDK is managed by Maven as a dependency).*

### Setup and Running

1.  **Clone the Repository:**
    ```bash
    git clone https://https://github.com/PredixCode/sortvisualizer/
    # Example: git clone ()
    cd SortVisualizer
    ```

2.  **Build and Run with 'start.bat' (Recommended):**
    The project is configured to run easily using the JavaFX Maven plugin.
    Just execute the start.bat which in turn execures the command:
    ```bash
    mvn javafx:run
    ```


3.  **Running from an IDE (e.g., IntelliJ IDEA, Eclipse):**
    * Import the project as a Maven project.
    * Ensure your IDE is configured to use JDK 17.
    * The primary main class is `com.predixcode.sortvisualizer.ui.App`.
    * **Important:** When running directly from an IDE (not using `mvn javafx:run`), you might need to configure VM options to include the JavaFX modules. For example:
        ```
        --module-path /path/to/your/javafx-sdk-XX/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics 
        ```
        Replace `/path/to/your/javafx-sdk-XX/lib` with the actual path to your JavaFX SDK's lib directory (XX being your JavaFX version, e.g., 17.0.10). Using `mvn javafx:run` avoids this manual configuration.

## How to Use

1.  **Launch the Application:** Follow the "Getting Started" instructions.
2.  **Algorithm & Speed Controls Section:**
    * **Select Algorithm:** Choose a sorting algorithm from the dropdown menu.
    * **Start Sort:** Click to begin the visualization.
    * **Pause/Resume:** Click to pause the ongoing sort; click again ("Resume") to continue.
    * **Stop Sort:** Click to halt the current sorting process.
    * **Adjust Speed:** Use the "Animation Delay" slider to control the speed between steps.
3.  **Array Configuration Section:**
    * **Array Size:** Enter a desired size (e.g., 10-500).
    * **Generate New Array:** Click to create a new random array based on the specified size.
4.  **Sound Configuration Section:**
    * **Enable Sound:** Use the checkbox to toggle audio feedback on or off.
    * **Min/Max Frequency:** Adjust the sliders to change the range of pitches used for the audio representation of element values.
5.  **Observe the Visualization:**
    * **Bars:** Represent array elements, with height corresponding to value. Bars have rounded corners and subtle gradients for improved aesthetics.
    * **Colors:**
        * **Yellow:** Typically indicates elements being compared.
        * **Red:** Typically indicates elements being swapped.
        * **Orange:** May indicate pivot elements.
        * **Green:** Indicates elements in their final sorted position.
    * **Sound:** Listen to the tones corresponding to element values during operations.

## Implemented Algorithms

The SortVisualizer currently features the following algorithms, each adapted for step-by-step visualization:

* **Bubble Sort**
* **Insertion Sort**
* **Quick Sort** (Lomuto partition scheme, iterative step-by-step)
* **Merge Sort** (Iterative step-by-step)
* **Tree Sort** (BST-based, step-by-step build and traversal)

## Future Enhancements

* More sorting algorithms (e.g., Heap Sort, Shell Sort, Radix Sort).
* Detailed statistics (comparisons, swaps, time complexity).
* Custom user-input datasets.
* Visual rendering of the Binary Search Tree for Tree Sort.
* Display of pseudo-code snippets for current algorithm steps.
