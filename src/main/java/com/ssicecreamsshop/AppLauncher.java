package com.ssicecreamsshop;

import com.ssicecreamsshop.config.ConfigManager; // Import ConfigManager
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
            scene = new Scene(root);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }

    @Override
    public void start(Stage stage) {
        // Ensure default configuration paths are checked/created at startup
        ConfigManager.ensureDefaultPathsExist();

        primaryStage = stage;

        MainView.show(); // MainView will now include the Config button

        primaryStage.setTitle("Ice Cream Shop");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
