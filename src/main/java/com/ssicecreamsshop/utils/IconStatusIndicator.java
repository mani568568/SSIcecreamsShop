package com.ssicecreamsshop.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class IconStatusIndicator extends StackPane {

    private Node activeIcon;
    private Node inactiveIcon;
    private volatile boolean isActive = false;

    public IconStatusIndicator(Node activeIcon, Node inactiveIcon) {
        this.activeIcon = activeIcon;
        this.inactiveIcon = inactiveIcon;
        // Ensure only one icon is visible at a time and managed
        this.activeIcon.setVisible(false);
        this.activeIcon.setManaged(false);
        this.inactiveIcon.setVisible(true);
        this.inactiveIcon.setManaged(true);

        getChildren().addAll(this.inactiveIcon, this.activeIcon);
        updateVisuals(); // Set initial state
    }

    public void setActive(boolean active) {
        if (this.isActive != active) {
            this.isActive = active;
            Platform.runLater(this::updateVisuals);
        }
    }

    public boolean isActive() {
        return isActive;
    }

    private void updateVisuals() {
        if (isActive) {
            activeIcon.setVisible(true);
            activeIcon.setManaged(true);
            inactiveIcon.setVisible(false);
            inactiveIcon.setManaged(false);
        } else {
            activeIcon.setVisible(false);
            activeIcon.setManaged(false);
            inactiveIcon.setVisible(true);
            inactiveIcon.setManaged(true);
        }
    }
}
