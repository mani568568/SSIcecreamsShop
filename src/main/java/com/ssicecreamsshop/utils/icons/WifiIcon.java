package com.ssicecreamsshop.utils.icons;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

public class WifiIcon extends Pane {
    private SVGPath mainPath;
    // A single path representing a Wi-Fi symbol for easier coloring.
    // This path is a simplified representation.
    private static final String WIFI_SINGLE_PATH = "M0.735 21.879c3.083-3.083 7.166-4.814 11.265-4.814s8.182 1.731 11.265 4.814l-11.265 11.265-11.265-11.265zm4.53 4.53c1.849-1.849 4.3-2.889 6.735-2.889s4.885 1.039 6.735 2.889l-6.735 6.735-6.735-6.735zm4.531 4.531c0.616-0.616 1.434-0.962 2.204-0.962s1.588 0.346 2.204 0.962l-2.204 2.204-2.204-2.204z";

    public WifiIcon(Color color) {
        mainPath = new SVGPath();
        mainPath.setContent(WIFI_SINGLE_PATH);
        mainPath.setFill(color);
        mainPath.setStroke(color.darker()); // Optional: for a slight outline
        mainPath.setStrokeWidth(0.3);

        // Scale the icon. The path data is for a viewbox around 32x32.
        double scaleFactor = 0.7; // Adjust this for desired size
        Scale scale = new Scale(scaleFactor, scaleFactor, 0, 0); // Pivot at 0,0
        mainPath.getTransforms().add(scale);

        // Set preferred size based on scaled icon.
        // Original path is ~24 units wide and high after accounting for its internal structure.
        // A more precise way would be to get bounds after setting content.
        setPrefSize(24 * scaleFactor, 24 * scaleFactor);
        getChildren().add(mainPath);
    }

    public void setColor(Color color) {
        mainPath.setFill(color);
        mainPath.setStroke(color.darker());
    }
}
