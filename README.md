# SortVisualizer: A Dynamic JavaFX Sorting Algorithm Visualization Tool

## Overview

Welcome to **SortVisualizer**, an interactive application designed to bring sorting algorithms to life! Built with Java and JavaFX, this tool provides a clear, step-by-step visual representation of how different sorting algorithms operate on a dataset.

## Features

* **Dynamic Visualization:** Watch bars representing array elements rearrange in real-time according to the chosen algorithm's logic.
* **Step-by-Step Execution:** Control the pace of the visualization with adjustable animation speed, or pause and resume to examine specific stages.
* **Algorithm Selection:** Easily switch between a variety of implemented sorting algorithms to compare their processes and efficiencies.
    * Bubble Sort
    * Insertion Sort
    * QuickSort (Lomuto partition scheme)
    * Merge Sort (Iterative approach for visualization)
    * Tree Sort (BST-based)
    * *(More to come!)*
* **Customizable Data:** Generate new random arrays of varying sizes and value ranges to observe algorithm behavior under different conditions.
* **Intuitive User Interface:** A clean and user-friendly interface built with JavaFX, making it easy to control and interact with the visualizer.
* **Educational Focus:** Designed to demystify complex sorting processes and provide a deeper understanding of their underlying mechanics.
* **Color-Coded States:** Elements are color-coded to indicate their current state (e.g., being compared, swapped, part of a pivot operation, or sorted).

## Technologies Used

* **Java:** Core application logic.
* **JavaFX (OpenJFX):** Modern GUI framework for the user interface and visualizations.
* **Maven:** Project build and dependency management.

## Getting Started

### Prerequisites

* **JDK (Java Development Kit):** Version 17 or higher.
* **Maven:** Version 3.6 or higher (for building and running via Maven).
* **JavaFX SDK:** Ensure your JavaFX SDK version matches the one specified in the `pom.xml` (e.g., 17.0.10). While Maven handles dependencies, direct SDK download might be needed if running outside Maven with specific VM options.

### Setup and Running

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/PredixCode/sortvisualizer/tree/main
    cd SortVisualizer
    ```

2.  **Build and Run with Maven (Recommended):**
    The project is configured to run easily using the JavaFX Maven plugin.
    Just execute the start.bat, which holds the command:
    ```bash
    mvn javafx:run
    ```
    This command will compile the project and launch the application.

3.  **Running from an IDE (e.g., IntelliJ IDEA, Eclipse):**
    * Import the project as a Maven project.
    * Ensure your IDE is configured to use JDK 17.
    * The primary main class is `com.predixcode.sortvisualizer.ui.App`.
    * **Important:** When running directly from an IDE (not using `mvn javafx:run`), you might need to configure VM options to include the JavaFX modules. For example:
        ```
        --module-path /path/to/your/javafx-sdk-17.0.10/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics
        ```
        Replace `/path/to/your/javafx-sdk-17.0.10/lib` with the actual path to your JavaFX SDK's lib directory. Using the `javafx-maven-plugin` (as in step 2) avoids this manual configuration.

## How to Use

1.  **Launch the Application:** Follow the "Getting Started" instructions.
2.  **Generate Array:**
    * Optionally, enter a desired **Array Size** (default is 50, min 10, max typically 200-500 for good visualization).
    * Click the "**Generate New Array**" button. A new set of bars representing random integer values will appear.
3.  **Select Algorithm:**
    * Choose a sorting algorithm from the "**Algorithm**" dropdown menu.
4.  **Adjust Speed:**
    * Use the "**Speed**" slider to control the animation delay between steps (in milliseconds). Slower speeds make it easier to follow detailed operations.
5.  **Start Sorting:**
    * Click the "**Start Sort**" button. The visualization will begin, showing the selected algorithm in action.
6.  **Observe:**
    * **Yellow Bars:** Typically indicate elements currently being compared.
    * **Red Bars:** Typically indicate elements currently being swapped or moved.
    * **Orange Bars (or other distinct color):** May indicate pivot elements or other special-status elements depending on the algorithm.
    * **Green Bars (or other distinct color):** Indicate elements that are in their final sorted position.
7.  **Stop Sorting:**
    * Click the "**Stop Sort**" button (becomes enabled during sorting) to halt the current sorting process. The array will remain in its current state.
8.  **Pause/Resume (Future Feature):**
    * Controls for pausing and resuming the visualization step-by-step.

## Implemented Algorithms

The SortVisualizer currently features the following algorithms, each adapted for step-by-step visualization:

* **Bubble Sort:** A simple comparison-based algorithm that repeatedly steps through the list, compares adjacent elements, and swaps them if they are in the wrong order.
* **Insertion Sort:** Builds the final sorted array one item at a time. It iterates through an input array and removes one element per iteration, finds the place the element belongs in the sorted list, and inserts it there.
* **Quick Sort:** An efficient divide-and-conquer algorithm. It picks an element as a pivot and partitions the given array around the picked pivot. (Currently uses Lomuto partition scheme, adapted for iterative step-by-step visualization).
* **Merge Sort:** Another efficient divide-and-conquer algorithm. It divides the unsorted list into n sublists, each containing one element (a list of one element is considered sorted), and then repeatedly merges sublists to produce new sorted sublists until there is only one sublist remaining. (Adapted for iterative step-by-step visualization).
* **Tree Sort:** Builds a binary search tree (BST) from the elements to be sorted and then performs an in-order traversal on the tree to get the elements in sorted order. (Adapted for step-by-step visualization of BST construction and traversal).
