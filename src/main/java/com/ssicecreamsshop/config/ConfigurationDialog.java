package com.ssicecreamsshop.config;

import com.ssicecreamsshop.ManageInventoryView;
import com.ssicecreamsshop.NewOrderView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationDialog {

    private static TextField imagePathField;
    private static TextField menuJsonPathField;
    private static Stage dialogStage;

    public static void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("⚙️ Application Configuration");

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f0f4f8;");

        Label titleLabel = new Label("Configure File Paths");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Image Path
        grid.add(new Label("Images Directory Path:"), 0, 0);
        imagePathField = new TextField(ConfigManager.getImagePath());
        imagePathField.setPrefWidth(350);
        imagePathField.setPromptText("Path to your images folder");
        Button browseImageDirButton = new Button("Browse...");
        browseImageDirButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Images Directory");
            File currentDir = new File(imagePathField.getText());
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            } else {
                File defaultDir = new File(ConfigManager.getImagePath()).getParentFile(); // Try parent of default
                if (defaultDir!=null && defaultDir.exists() && defaultDir.isDirectory()) {
                    directoryChooser.setInitialDirectory(defaultDir);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(dialogStage);
            if (selectedDirectory != null) {
                imagePathField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        HBox imagePathBox = new HBox(10, imagePathField, browseImageDirButton);
        imagePathBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(imagePathBox, 1, 0);

        // Menu JSON Path
        grid.add(new Label("Menu JSON File Path:"), 0, 1);
        menuJsonPathField = new TextField(ConfigManager.getMenuItemsJsonPath());
        menuJsonPathField.setPromptText("Path to your menu_items.json file");
        Button browseMenuJsonButton = new Button("Browse...");
        browseMenuJsonButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select menu_items.json");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));

            File currentJsonFile = new File(menuJsonPathField.getText());
            File parentDir = currentJsonFile.getParentFile();
            if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                fileChooser.setInitialDirectory(parentDir);
            } else {
                Path defaultJsonPath = Paths.get(ConfigManager.getMenuItemsJsonPath());
                File defaultParentDir = defaultJsonPath.getParent() != null ? defaultJsonPath.getParent().toFile() : null;
                if (defaultParentDir != null && defaultParentDir.exists() && defaultParentDir.isDirectory()) {
                    fileChooser.setInitialDirectory(defaultParentDir);
                }
            }
            File selectedFile = fileChooser.showOpenDialog(dialogStage);
            if (selectedFile != null) {
                menuJsonPathField.setText(selectedFile.getAbsolutePath());
            }
        });
        HBox menuJsonPathBox = new HBox(10, menuJsonPathField, browseMenuJsonButton);
        menuJsonPathBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(menuJsonPathBox, 1, 1);

        // Save and Cancel Buttons
        Button saveButton = new Button("Save Configuration");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        saveButton.setOnAction(e -> saveConfiguration());

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-padding: 8 15;");
        cancelButton.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(20, saveButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(20, 0, 0, 0));

        layout.getChildren().addAll(titleLabel, grid, buttonBar);

        Scene scene = new Scene(layout);
        dialogStage.setScene(scene);
        dialogStage.sizeToScene();
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private static void saveConfiguration() {
        String imagePathStr = imagePathField.getText().trim();
        String menuJsonPathStr = menuJsonPathField.getText().trim();

        if (imagePathStr.isEmpty() || menuJsonPathStr.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Both paths must be specified.");
            return;
        }

        try {
            Path imgP = Paths.get(imagePathStr);
            // We expect a directory. If it doesn't exist, ConfigManager.setImagePath will try to create it.
            // If it exists but is not a directory, that's an issue.
            if (Files.exists(imgP) && !Files.isDirectory(imgP)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Image path exists but is not a directory: " + imagePathStr);
                return;
            }
        } catch (InvalidPathException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid image directory path: " + e.getMessage());
            return;
        }

        try {
            Path menuP = Paths.get(menuJsonPathStr);
            if (Files.exists(menuP) && Files.isDirectory(menuP)) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Menu JSON path must be a file, not a directory: " + menuJsonPathStr);
                return;
            }
            if (!menuJsonPathStr.toLowerCase().endsWith(".json")) {
                showAlert(Alert.AlertType.WARNING, "Validation Warning", "Menu file path does not end with .json. Ensure it's a valid JSON file.");
            }
            // Parent directory creation is handled by ConfigManager.setMenuItemsJsonPath
        } catch (InvalidPathException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid menu JSON file path: " + e.getMessage());
            return;
        }

        ConfigManager.setImagePath(imagePathStr);
        ConfigManager.setMenuItemsJsonPath(menuJsonPathStr);

        showAlert(Alert.AlertType.INFORMATION, "Configuration Saved", "Paths have been saved successfully.");

        // Attempt to reload data and refresh views
        try {
            // These methods should internally handle if their respective views are not yet fully initialized
            ManageInventoryView.loadInventoryData();
            NewOrderView.loadMenuItemsFromJson();
            NewOrderView.refreshMenuView(); // If NewOrderView is active, this will update its UI
        } catch (Exception ex) {
            System.err.println("Error refreshing views after config change: " + ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Refresh Issue",
                    "Configuration saved, but there was an issue refreshing some views immediately: " + ex.getMessage() +
                            "\nChanges will apply fully on next view or application restart.");
        }
        dialogStage.close();
    }

    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(dialogStage); // Ensures the alert is modal to the config dialog
        alert.showAndWait();
    }
}
