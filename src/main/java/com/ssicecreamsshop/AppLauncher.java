package com.ssicecreamsshop;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {
    private static Stage primaryStage;
    private static Scene scene; // Store the scene instance

    /**
     * Sets the root node for the primary stage's scene.
     * If the scene hasn't been initialized, it creates a new scene
     * with the given root. Otherwise, it replaces the root of the existing scene.
     * This approach ensures that if the stage is maximized, the scene retains
     * its maximized dimensions when views are switched.
     * @param root The Parent node to set as the root of the scene.
     */
    public static void setScreen(Parent root) {
        if (scene == null) { // First-time setup
            // Create the scene without fixed dimensions.
            // It will adapt to the stage size, especially when maximized.
            scene = new Scene(root);
            primaryStage.setScene(scene);
        } else {
            // For subsequent calls, just change the root of the existing scene.
            // The 'scene' object has already been sized by the stage (e.g., when maximized).
            scene.setRoot(root);
        }
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // MainView.show() will call setScreen, which initializes 'scene' and sets it on primaryStage
        MainView.show();

        primaryStage.setTitle("Ice Cream Shop");
        primaryStage.setMaximized(true); // Maximize the stage. This will affect the 'scene' dimensions.
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
