package com.predixcode.sortvisualizer.ui;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Theme class to hold constants for UI styling, such as colors and fonts.
 * This helps in maintaining a consistent look and feel across the application.
 */
public class Theme {

    // --- Primary Colors ---
    public static final Color PRIMARY_COLOR = Color.web("#4A90E2"); // A nice blue
    public static final Color SECONDARY_COLOR = Color.web("#50E3C2"); // A teal/mint color
    public static final Color ACCENT_COLOR = Color.web("#F5A623"); // An orange accent

    // --- Background Colors ---
    public static final Color BACKGROUND_COLOR = Color.web("#282c34"); // Dark background
    public static final String BACKGROUND_COLOR_HEX = "#282c34"; // Hex for CSS styling in code
    public static final Color PANEL_BACKGROUND_COLOR = Color.web("#3F444E"); // Slightly lighter for panels

    // --- Text Colors ---
    public static final Color TEXT_COLOR_LIGHT = Color.web("#ABB2BF"); // Light grey for text on dark background
    public static final Color TEXT_COLOR_DARK = Color.web("#282c34");  // Dark grey for text on light background
    public static final Color TEXT_COLOR_ACCENT = ACCENT_COLOR;

    // --- Array Bar Colors ---
    public static final Color BAR_DEFAULT_COLOR = PRIMARY_COLOR;
    public static final Color BAR_COMPARE_COLOR = Color.YELLOW;
    public static final Color BAR_SWAP_COLOR = Color.RED;
    public static final Color BAR_SORTED_COLOR = SECONDARY_COLOR; // Greenish when sorted
    public static final Color BAR_PIVOT_COLOR = Color.ORANGE; // For algorithms like QuickSort

    // --- Fonts ---
    public static final Font FONT_DEFAULT = Font.font("Arial", FontWeight.NORMAL, 14);
    public static final Font FONT_TITLE = Font.font("Arial", FontWeight.BOLD, 20);
    public static final Font FONT_BUTTON = Font.font("Arial", FontWeight.SEMI_BOLD, 13);

    // --- Spacing and Padding ---
    public static final double PADDING_SMALL = 5.0;
    public static final double PADDING_MEDIUM = 10.0;
    public static final double PADDING_LARGE = 15.0;

    // --- Border Styles ---
    public static final String BORDER_STYLE_DEFAULT = "-fx-border-color: #4A4F5A; -fx-border-width: 1px; -fx-border-radius: 3px;";

    // --- Button Styles (can be used with setStyle) ---
    public static final String BUTTON_STYLE_PRIMARY =
        "-fx-background-color: " + toHex(PRIMARY_COLOR) + ";" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 5px;" +
        "-fx-padding: 8px 15px;";

    public static final String BUTTON_STYLE_HOVER =
        "-fx-background-color: " + toHex(PRIMARY_COLOR.deriveColor(0, 1.0, 0.8, 1.0)) + ";"; // Lighter version for hover

    /**
     * Helper method to convert a JavaFX Color object to its HEX web representation.
     * @param color The color to convert.
     * @return The HEX string (e.g., "#RRGGBB").
     */
    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    // Private constructor to prevent instantiation
    private Theme() {}
}
