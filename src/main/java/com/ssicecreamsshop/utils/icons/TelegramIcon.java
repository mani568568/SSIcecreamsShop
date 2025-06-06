package com.ssicecreamsshop.utils.icons;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

public class TelegramIcon extends Pane {
    private SVGPath mainPath;
    private static final String TELEGRAM_PATH_DATA = "M22.219 5.522l-3.752 17.586c-.244 1.139-.861 1.42-1.773 0.888l-5.46-4.028-2.645 2.54a1.22 1.22 0 01-.987.444l.357-5.569L19.555 7.75c.746-.66-.065-1.021-.996-.39L7.454 13.71.945 12.219c-1.132-.356-1.148-1.143.242-1.701L20.553 4.19c.96-.452 1.826-.223 1.666 1.332z";

    public TelegramIcon(Color color) {
        mainPath = new SVGPath();
        mainPath.setContent(TELEGRAM_PATH_DATA);
        mainPath.setFill(color);
        // mainPath.setStroke(Color.TRANSPARENT); // No stroke for standard Telegram icon

        // Scale the icon. Original path is roughly for a 24x24 viewbox.
        double scaleFactor = 0.9; // Adjust for desired size
        Scale scale = new Scale(scaleFactor, scaleFactor, 0, 0);
        mainPath.getTransforms().add(scale);

        setPrefSize(24 * scaleFactor, 24 * scaleFactor);
        getChildren().add(mainPath);
    }

    public void setColor(Color color) {
        mainPath.setFill(color);
    }
}
